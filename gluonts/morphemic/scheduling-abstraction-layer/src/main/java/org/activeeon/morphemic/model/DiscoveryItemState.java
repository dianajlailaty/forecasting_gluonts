package org.activeeon.morphemic.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Gets or Sets DiscoveryItemState
 */
public enum DiscoveryItemState {

    NEW("NEW"),

    OK("OK"),

    REMOTELY_DELETED("REMOTELY_DELETED"),

    LOCALLY_DELETED("LOCALLY_DELETED"),

    DISABLED("DISABLED"),

    DELETED("DELETED"),

    UNKNOWN("UNKNOWN");

    private String value;

    DiscoveryItemState(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static DiscoveryItemState fromValue(String text) {
        for (DiscoveryItemState b : DiscoveryItemState.values()) {
            if (String.valueOf(b.value).equals(text.toUpperCase(Locale.ROOT))) {
                return b;
            }
        }
        return null;
    }
}

