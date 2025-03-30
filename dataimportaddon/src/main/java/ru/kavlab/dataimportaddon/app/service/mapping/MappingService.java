package ru.kavlab.dataimportaddon.app.service.mapping;

import ru.kavlab.dataimportaddon.app.configuration.MappingSettings;
import ru.kavlab.dataimportaddon.app.data.MappingEntity;
import ru.kavlab.dataimportaddon.app.data.MappingForListView;
import ru.kavlab.dataimportaddon.app.data.PropertyFillType;
import ru.kavlab.dataimportaddon.app.data.PropertyInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MappingService {

    enum MappingField {
        TYPE, VALUE, ATTR
    }

    MappingSettings getMappingSettings();

    Optional<MappingEntity> getMappingEntityByLocalName(String localEntityName);

    String getMappingSettingsAsString();

    boolean loadMappingSettingsFromString(String settings);

    List<MappingForListView> getMappingsForListView();

    Map<String, Map<String, PropertyInfo>> getLocalMetadata();

    Optional<String> getSelectedValue(String entityName,
                                      String attr,
                                      MappingField field);

    void setUpload(String localEntityName, boolean upload);

    void setExternalEntityName(String localEntityName,
                               String externalEntityName);

    void setScript(String localEntityName,
                   String script);

    void setPropertyValue(String localEntityName,
                          String property,
                          PropertyFillType type,
                          String value,
                          String attribute);

    void setBatchSize(Integer batchSize);

    void setDuplicateEntityPolicy(String id);

    void setErrorStrategy(String id);

    List<String> duplicateEntityPolicy();

    List<String> errorStrategies();
}
