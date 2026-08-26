package com.sporty.f1bets.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Google "Medium" test: single machine, may use localhost I/O (Testcontainers
 * Postgres, WireMock, an embedded server).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("medium")
public @interface Medium {
}

