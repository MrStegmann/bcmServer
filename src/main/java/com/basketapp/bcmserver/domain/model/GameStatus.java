package com.basketapp.bcmserver.domain.model;

/**
 * Pure Java 21 domain enum representing match progression.
 * Free from framework and persistence annotations.
 */
public enum GameStatus {
    PENDING("pending"),
    LIVE("live"),
    FINISHED("finished");

    private final String value;

    GameStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GameStatus fromValue(String value) {
        if (value == null) {
            return PENDING;
        }
        for (GameStatus status : values()) {
            if (status.value.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid game status: " + value + ". Allowed values: pending, live, finished");
    }
}

