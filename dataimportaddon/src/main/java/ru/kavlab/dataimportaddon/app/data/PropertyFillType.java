package ru.kavlab.dataimportaddon.app.data;

import org.springframework.lang.Nullable;

public enum PropertyFillType {

    CONSTANT("Constant"),
    ATTRIBUTE("Attribute");

    private final String id;

    PropertyFillType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static PropertyFillType fromId(String id) {
        for (PropertyFillType at : PropertyFillType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}
