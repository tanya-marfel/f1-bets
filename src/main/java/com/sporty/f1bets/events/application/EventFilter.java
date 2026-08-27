package com.sporty.f1bets.events.application;

/**
 * Optional filters for listing events. Any field may be null.
 */
public record EventFilter(String sessionType, Integer year, String country) {}
