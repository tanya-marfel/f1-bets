
FROM amazoncorretto:25

# Defense in depth: run as a dedicated non-root user. amazoncorretto (Amazon
# Linux) ships no useradd, so create the user/group entries directly (no packages, no network).
RUN echo 'app:x:10001:10001:app:/app:/sbin/nologin' >> /etc/passwd \
    && echo 'app:x:10001:' >> /etc/group

WORKDIR /app
COPY build/libs/*-SNAPSHOT.jar app.jar
RUN chown -R 10001:10001 /app
USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]



