package ru.kavlab.dataimportaddon.app.data;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.JmixId;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;

import java.util.UUID;

@JmixEntity
public class MappingForListView {
    @JmixGeneratedValue
    @JmixId
    private UUID id;

    @InstanceName
    private String entityNameJmix;

    private String entityName1C;

    private Boolean attributesMapped;

    private Boolean scriptDefined;

    private Boolean upload;

    public Boolean getUpload() {
        return upload;
    }

    public void setUpload(Boolean upload) {
        this.upload = upload;
    }

    public String getEntityNameJmix() {
        return entityNameJmix;
    }

    public void setEntityNameJmix(String entityNameJmix) {
        this.entityNameJmix = entityNameJmix;
    }

    public String getEntityName1C() {
        return entityName1C;
    }

    public void setEntityName1C(String entityName1C) {
        this.entityName1C = entityName1C;
    }

    public Boolean getAttributesMapped() {
        return attributesMapped;
    }

    public void setAttributesMapped(Boolean attributesMapped) {
        this.attributesMapped = attributesMapped;
    }

    public Boolean getScriptDefined() {
        return scriptDefined;
    }

    public void setScriptDefined(Boolean scriptSet) {
        this.scriptDefined = scriptSet;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
