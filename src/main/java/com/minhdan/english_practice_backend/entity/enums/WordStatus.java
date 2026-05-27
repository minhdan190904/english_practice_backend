package com.minhdan.english_practice_backend.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum WordStatus {
    UNKNOWN,
    /** @deprecated Use {@link #STUDYING} instead */
    @Deprecated LEARNING,
    MASTERED,
    /** @deprecated Use {@link #STUDYING} instead */
    @Deprecated STARRED,
    STUDYING;

    /**
     * Normalize legacy statuses to their modern equivalents.
     * STARRED and LEARNING are treated as STUDYING.
     */
    public WordStatus normalize() {
        return switch (this) {
            case STARRED, LEARNING -> STUDYING;
            default -> this;
        };
    }

    /**
     * Deserialize from JSON with backward compatibility.
     * Accepts "STARRED" and "LEARNING" as aliases for "STUDYING".
     */
    @JsonCreator
    public static WordStatus fromString(String value) {
        if (value == null) return null;
        WordStatus status = WordStatus.valueOf(value.toUpperCase());
        return status.normalize();
    }
}

