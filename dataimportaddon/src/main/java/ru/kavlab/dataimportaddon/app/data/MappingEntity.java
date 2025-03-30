package ru.kavlab.dataimportaddon.app.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MappingEntity {

    private boolean upload;

    @JsonProperty("entity_jmix")
    private final String entityNameJmix;

    @JsonProperty("entity_1c")
    private String entityName1C;

    private String script;

    @JsonProperty("mapping_properties")
    private final List<MappingProperty> mappingProperties;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MappingEntity(
            @JsonProperty("upload") boolean upload,
            @JsonProperty("entity_jmix") String entityNameJmix,
            @JsonProperty("entity_1c") String entityName1C,
            @JsonProperty("script") String script,
            @JsonProperty("mapping_properties") List<MappingProperty> mappingProperties
    ) {
        this.upload = upload;
        this.entityNameJmix = entityNameJmix;
        this.entityName1C = entityName1C;
        this.script = script;
        this.mappingProperties = Objects.requireNonNullElseGet(mappingProperties, ArrayList::new);
    }

    public String getEntityNameJmix() {
        return entityNameJmix;
    }

    public String getEntityName1C() {
        return entityName1C;
    }

    public String getScript() {
        return script;
    }

    public List<MappingProperty> getMappingProperties() {
        return mappingProperties;
    }

    public void setEntityName1C(String entityName1C) {
        this.entityName1C = entityName1C;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public boolean isUpload() {
        return upload;
    }

    public void setUpload(boolean upload) {
        this.upload = upload;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MappingEntity that = (MappingEntity) o;
        return upload == that.upload
                && Objects.equals(entityNameJmix, that.entityNameJmix)
                && Objects.equals(entityName1C, that.entityName1C)
                && Objects.equals(script, that.script)
                && Objects.equals(mappingProperties, that.mappingProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(upload, entityNameJmix, entityName1C, script, mappingProperties);
    }
}