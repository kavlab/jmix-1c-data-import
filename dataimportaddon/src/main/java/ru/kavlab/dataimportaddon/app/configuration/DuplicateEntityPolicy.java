package ru.kavlab.dataimportaddon.app.configuration;

import org.springframework.lang.Nullable;

public enum DuplicateEntityPolicy {
    SKIP("Create new entity"),
    UPDATE("Update entity"),
    ABORT("Abort");

    private final String id;

    DuplicateEntityPolicy(String id) {
        this.id = id;
    }

    @Nullable
    public static DuplicateEntityPolicy fromId(String id) {
        for (DuplicateEntityPolicy at : DuplicateEntityPolicy.values()) {
            if (at.toString().equals(id)) {
                return at;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return id;
    }
}
