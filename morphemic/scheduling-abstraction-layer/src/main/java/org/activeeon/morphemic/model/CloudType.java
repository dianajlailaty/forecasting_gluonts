package org.activeeon.morphemic.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * type of the cloud
 */
public enum CloudType {

    PRIVATE("PRIVATE"),

    PUBLIC("PUBLIC"),

    SIMULATION("SIMULATION");

    private String value;

    CloudType(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static CloudType fromValue(String text) {
        for (CloudType b : CloudType.values()) {
            if (String.valueOf(b.value).equals(text.toUpperCase(Locale.ROOT))) {
                return b;
            }
        }
        return null;
    }
}

