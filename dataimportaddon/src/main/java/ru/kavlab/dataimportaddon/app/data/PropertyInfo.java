package ru.kavlab.dataimportaddon.app.data;

import io.jmix.core.metamodel.model.MetaProperty;

public class PropertyInfo {
    private String name;
    private Class<?> javaType;
    private boolean isMandatory;
    private Integer maxLength;

    public PropertyInfo(String name, Class<?> javaType, boolean isMandatory, Integer maxLength) {
        this.name = name;
        this.javaType = javaType;
        this.isMandatory = isMandatory;
        this.maxLength = maxLength;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public void setJavaType(Class<?> javaType) {
        this.javaType = javaType;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public static PropertyInfo createPropertyInfo(MetaProperty property) {
        int maxLength = 0;
        // If the property is of type String, try to extract the jmix length annotation
        if (String.class.equals(property.getJavaType())) {
            Object jmixLength = property.getAnnotations().get("jmix.length");
            if (jmixLength instanceof Integer) {
                maxLength = (Integer) jmixLength;
            }
        }
        return new PropertyInfo(
                property.getName(),
                property.getJavaType(),
                property.isMandatory(),
                maxLength
        );
    }
}
