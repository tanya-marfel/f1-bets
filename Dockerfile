# syntax=docker/dockerfile:1

# Kept in sync with `java` in gradle/libs.versions.toml and with
# gradle/wrapper/gradle-wrapper.properties; `make check-versions` fails loudly
# on drift. Defaults live here so a bare `docker build .` works on a clean
# checkout.
ARG JAVA_VERSION=25
ARG GRADLE_VERSION=9.5.1

# -----------------------------------------------------------------------------
# Build stage - compiles the boot jar from source so the image is reproducible
# from a clean checkout, with no prior Gradle run required on the host.
#
# The official Gradle image ships the matching Gradle distribution, so the build
# never needs to reach services.gradle.org (which some corporate TLS-inspecting
# proxies break). Dependencies come from Maven Central.
#
# The -corretto-al2023 variant is used so both stages share the same JDK vendor
# (Amazon Corretto) and base distro (AL2023) as the runtime image below; the
# plain `gradle:*-jdk*` tag would pull in Temurin on Ubuntu instead.
# -----------------------------------------------------------------------------
FROM gradle:${GRADLE_VERSION}-jdk${JAVA_VERSION}-corretto-al2023 AS build

WORKDIR /workspace

# Build definition first: when only sources change, Docker still reuses this
# layer, and the Gradle cache mount keeps dependencies off the network.
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./

COPY src src

# The cache mount persists the Gradle module cache between builds without
# baking it into a layer.
RUN --mount=type=cache,target=/root/.gradle \
    gradle --no-daemon --console=plain bootJar

# Split the fat jar into its layers. Dependencies (~59 MB) change rarely, while
# the application layer (~300 KB) changes on every commit.
RUN java -Djarmode=tools -jar build/libs/*-SNAPSHOT.jar \
    extract --layers --launcher --destination extracted

# -----------------------------------------------------------------------------
# Runtime stage - headless AL2023 (same glibc as the build stage, no AWT/X11).
# -----------------------------------------------------------------------------
FROM amazoncorretto:${JAVA_VERSION}-al2023-headless AS runtime

# Defense in depth: run as a dedicated non-root user. amazoncorretto (Amazon
# Linux) ships no useradd, so create the user/group entries directly (no packages, no network).
RUN echo 'app:x:10001:10001:app:/app:/sbin/nologin' >> /etc/passwd \
    && echo 'app:x:10001:' >> /etc/group

WORKDIR /app

# Ordered least- to most-frequently-changing for maximum layer reuse: a code
# change only invalidates the final, tiny application layer.
COPY --from=build --chown=10001:10001 /workspace/extracted/dependencies/ ./
COPY --from=build --chown=10001:10001 /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=10001:10001 /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=10001:10001 /workspace/extracted/application/ ./

USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
