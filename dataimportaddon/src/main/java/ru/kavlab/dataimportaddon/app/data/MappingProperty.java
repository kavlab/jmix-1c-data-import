package ru.kavlab.dataimportaddon.app.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class MappingProperty {
    @JsonProperty("jmix_property")
    private final String jmixProperty;

    private PropertyFillType type;

    private Object value;

    private String attribute;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MappingProperty(
            @JsonProperty("jmix_property") String jmixProperty,
            @JsonProperty("type") PropertyFillType type,
            @JsonProperty("value") Object value,
            @JsonProperty("attribute") String attribute
    ) {
        this.jmixProperty = jmixProperty;
        this.type = type;
        this.value = value;
        this.attribute = attribute;
    }

    public String getJmixProperty() {
        return jmixProperty;
    }

    public PropertyFillType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setType(PropertyFillType type) {
        this.type = type;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MappingProperty that = (MappingProperty) o;
        return Objects.equals(jmixProperty, that.jmixProperty)
                && type == that.type
                && Objects.equals(value, that.value)
                && Objects.equals(attribute, that.attribute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jmixProperty, type, value, attribute);
    }
}