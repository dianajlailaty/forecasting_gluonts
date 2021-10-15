package org.activeeon.morphemic.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Type of OS Architecture
 */
public enum OperatingSystemArchitecture {

    AMD64("AMD64"),

    UNKOWN("UNKOWN"),

    I386("I386"),

    ARM("ARM"),

    ARM64("ARM64");

    private String value;

    OperatingSystemArchitecture(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static OperatingSystemArchitecture fromValue(String text) {
        for (OperatingSystemArchitecture b : OperatingSystemArchitecture.values()) {
            if (String.valueOf(b.value).equals(text.toUpperCase(Locale.ROOT))) {
                return b;
            }
        }
        return UNKOWN;
    }
}


