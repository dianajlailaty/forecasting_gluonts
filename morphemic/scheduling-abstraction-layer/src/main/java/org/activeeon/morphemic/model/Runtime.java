package org.activeeon.morphemic.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents runtime provided by a FaaS platform.
 */
public enum Runtime {

    NODEJS("nodejs"),

    PYTHON("python"),

    JAVA("java"),

    DOTNET("dotnet"),

    GO("go");

    private String value;

    Runtime(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static Runtime fromValue(String text) {
        for (Runtime b : Runtime.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}

