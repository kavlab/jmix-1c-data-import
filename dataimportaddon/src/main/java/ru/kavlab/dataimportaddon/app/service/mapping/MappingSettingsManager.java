package ru.kavlab.dataimportaddon.app.service.mapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.kavlab.dataimportaddon.app.configuration.DuplicateEntityPolicy;
import ru.kavlab.dataimportaddon.app.configuration.ImportErrorPolicy;
import ru.kavlab.dataimportaddon.app.configuration.MappingSettings;
import ru.kavlab.dataimportaddon.app.data.MappingEntity;
import ru.kavlab.dataimportaddon.app.data.MappingProperty;
import ru.kavlab.dataimportaddon.app.data.PropertyFillType;

import java.util.ArrayList;

@Component("imp1c_MappingSettingsManager")
public class MappingSettingsManager {

    private static final Logger log = LoggerFactory.getLogger(MappingSettingsManager.class);

    private MappingSettings mappingSettings;
    private final ObjectMapper mapper;

    public MappingSettingsManager() {
        mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(SerializationFeature.EAGER_SERIALIZER_FETCH);

        mappingSettings = new MappingSettings(
                new ArrayList<>(), 0, DuplicateEntityPolicy.SKIP, ImportErrorPolicy.ABORT);
    }

    public MappingSettings getMappingSettings() {
        return mappingSettings;
    }

    public void setUpload(String localEntityName, boolean upload) {
        mappingSettings.getMappingEntity(localEntityName).ifPresent(
                mappingEntity -> mappingEntity.setUpload(upload)
        );
    }

    public void setBatchSize(Integer batchSize) {
        mappingSettings.setBatchSize(batchSize);
    }

    public void setDuplicateEntityPolicy(DuplicateEntityPolicy duplicateEntityPolicy) {
        mappingSettings.setLoadingStrategy(duplicateEntityPolicy);
    }

    public void setErrorStrategy(ImportErrorPolicy errorStrategy) {
        mappingSettings.setErrorStrategy(errorStrategy);
    }

    public void setExternalEntityName(String localEntityName, String externalEntityName) {
        mappingSettings.getMappingEntity(localEntityName)
                .ifPresentOrElse(
                        mappingEntity -> {
                            // change entity
                            mappingEntity.setEntityName1C(externalEntityName);
                            if (externalEntityName == null) {
                                mappingEntity.setScript(null);
                                mappingEntity.getMappingProperties().clear();
                            }
                        },
                        () -> {
                            // add new entity
                            var mappingEntity = new MappingEntity(
                                    false, localEntityName, externalEntityName, null, new ArrayList<>());
                            mappingSettings.getMappingEntities().add(mappingEntity);
                        });
    }

    public void setScript(String localEntityName, String script) {
        mappingSettings.getMappingEntity(localEntityName)
                .ifPresent(mappingEntity -> mappingEntity.setScript(script));
    }

    public void setPropertyValue(String localEntityName,
                                 String property,
                                 PropertyFillType type,
                                 String value,
                                 String attribute) {
        mappingSettings.getMappingEntity(localEntityName)
                .ifPresent(mappingEntity ->
                    addMappingProperty(mappingEntity, property, type, value, attribute)
                );
    }

    private void addMappingProperty(MappingEntity mappingEntity,
                                    String property,
                                    PropertyFillType type,
                                    String value,
                                    String attribute) {
        mappingEntity.getMappingProperties().stream()
                .filter(mappingProperty -> mappingProperty.getJmixProperty().equals(property))
                .findFirst()
                .ifPresentOrElse(
                        // change property
                        mappingProperty -> {
                            mappingProperty.setType(type);
                            mappingProperty.setValue(value);
                            mappingProperty.setAttribute(attribute);
                        },
                        // add property
                        () -> mappingEntity.getMappingProperties().add(
                                new MappingProperty(property, type, value, attribute))
                );
    }

    public String serializeSettings() {
        try {
            return mapper.writeValueAsString(mappingSettings);
        } catch (JsonProcessingException e) {
            log.error("Serialize settings error", e);
            return "";
        }
    }

    public boolean deserializeSettings(String settings) {
        try {
            mappingSettings = mapper.readValue(settings, MappingSettings.class);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Deserialize settings error", e);
            return false;
        }
    }
}
