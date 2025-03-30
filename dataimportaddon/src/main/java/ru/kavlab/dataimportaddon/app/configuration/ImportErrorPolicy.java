package ru.kavlab.dataimportaddon.app.configuration;

import org.springframework.lang.Nullable;

public enum ImportErrorPolicy {
    SKIP("Skip errors"),
    ABORT("Stop on error");

    private final String id;

    ImportErrorPolicy(String id) {
        this.id = id;
    }

    @Nullable
    public static ImportErrorPolicy fromId(String id) {
        for (ImportErrorPolicy at : ImportErrorPolicy.values()) {
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
