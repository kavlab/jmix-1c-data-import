package ru.kavlab.dataimportaddon.app.service.mapping;

import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import org.springframework.stereotype.Component;
import ru.kavlab.dataimportaddon.app.configuration.DuplicateEntityPolicy;
import ru.kavlab.dataimportaddon.app.configuration.ImportErrorPolicy;
import ru.kavlab.dataimportaddon.app.configuration.MappingSettings;
import ru.kavlab.dataimportaddon.app.data.*;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MappingServiceImpl implements MappingService {

    private final List<MappingForListView> mappingsForListView;
    private final LocalMetadataExtractor metadataExtractor;
    private final MappingSettingsManager settingsManager;
    private final DataManager dataManager;

    private List<String> localMetadataNames;
    private final Map<String, Map<String, PropertyInfo>> localMetadata = new HashMap<>();

    public MappingServiceImpl(Metadata metadata,
                              MetadataTools metadataTools,
                              DataManager dataManager,
                              LocalMetadataExtractor metadataExtractor,
                              MappingSettingsManager settingsManager) {
        this.dataManager = dataManager;
        this.metadataExtractor = metadataExtractor;
        this.settingsManager = settingsManager;

        extractLocalMetadata(metadata, metadataTools);
        this.mappingsForListView = createMappingsForListView();
    }

    private void extractLocalMetadata(Metadata metadata, MetadataTools metadataTools) {
        Map<String, Map<String, PropertyInfo>> extracted = metadataExtractor.extract(metadata, metadataTools);
        localMetadata.putAll(extracted);
        localMetadataNames = new ArrayList<>(extracted.keySet());
    }

    private List<MappingForListView> createMappingsForListView() {
        return localMetadataNames.stream()
                .map(name -> {
                    var newEntity = dataManager.create(MappingForListView.class);
                    newEntity.setEntityNameJmix(name);
                    return newEntity;
                })
                .collect(Collectors.toList());
    }

    @Override
    public MappingSettings getMappingSettings() {
        return settingsManager.getMappingSettings();
    }

    @Override
    public Optional<MappingEntity> getMappingEntityByLocalName(String localEntityName) {
        return settingsManager.getMappingSettings().getMappingEntities()
                .stream()
                .filter(mappingEntity -> mappingEntity.getEntityNameJmix().equals(localEntityName))
                .findFirst();
    }

    @Override
    public String getMappingSettingsAsString() {
        return settingsManager.serializeSettings();
    }

    @Override
    public boolean loadMappingSettingsFromString(String settings) {
        boolean result = settingsManager.deserializeSettings(settings);
        updateMappingsForListView();
        return result;
    }

    private void updateMappingsForListView() {
        mappingsForListView.forEach(mappingFLV ->
            getMappingEntityByLocalName(mappingFLV.getEntityNameJmix())
                    .ifPresentOrElse(
                            mappingEntity -> {
                                mappingFLV.setUpload(mappingEntity.isUpload());
                                mappingFLV.setEntityName1C(mappingEntity.getEntityName1C());
                                mappingFLV.setScriptDefined(mappingEntity.getScript() != null
                                        && !mappingEntity.getScript().isEmpty());
                                mappingFLV.setAttributesMapped(!mappingEntity.getMappingProperties().isEmpty());
                            },
                            () -> {
                                mappingFLV.setUpload(false);
                                mappingFLV.setEntityName1C(null);
                                mappingFLV.setScriptDefined(false);
                                mappingFLV.setAttributesMapped(false);
                            }
                    )
        );
    }

    private void updateMappingsForListView(String localEntityName) {
        mappingsForListView.stream()
                .filter(mappingFLV -> mappingFLV.getEntityNameJmix().equals(localEntityName))
                .forEach(mappingFLV ->
                        getMappingEntityByLocalName(mappingFLV.getEntityNameJmix())
                                .ifPresentOrElse(
                                        mappingEntity -> {
                                            mappingFLV.setUpload(mappingEntity.isUpload());
                                            mappingFLV.setEntityName1C(mappingEntity.getEntityName1C());
                                            mappingFLV.setScriptDefined(mappingEntity.getScript() != null
                                                    && !mappingEntity.getScript().isEmpty());
                                            mappingFLV.setAttributesMapped(
                                                    !mappingEntity.getMappingProperties().isEmpty());
                                        },
                                        () -> {
                                            mappingFLV.setUpload(false);
                                            mappingFLV.setEntityName1C(null);
                                            mappingFLV.setScriptDefined(false);
                                            mappingFLV.setAttributesMapped(false);
                                        }
                                )
                );
    }

    @Override
    public List<MappingForListView> getMappingsForListView() {
        return mappingsForListView;
    }

    @Override
    public Map<String, Map<String, PropertyInfo>> getLocalMetadata() {
        return localMetadata;
    }

    @Override
    public Optional<String> getSelectedValue(String entityName,
                                             String attr,
                                             MappingField field) {
        return settingsManager.getMappingSettings().getMappingEntities().stream()
                .filter(mappingEntity -> mappingEntity.getEntityNameJmix().equals(entityName))
                .flatMap(mappingEntity -> mappingEntity.getMappingProperties().stream())
                .filter(mappingProperty -> mappingProperty.getJmixProperty().equals(attr))
                .findFirst()
                .flatMap(mappingProperty -> getFieldValue(mappingProperty, field));
    }

    private Optional<String> getFieldValue(MappingProperty mp, MappingField field) {
        return switch (field) {
            case TYPE -> Optional.ofNullable(mp.getType() != null ? mp.getType().getId() : null);
            case VALUE -> Optional.ofNullable(mp.getValue() != null ? mp.getValue().toString() : "");
            case ATTR -> Optional.ofNullable(mp.getAttribute());
        };
    }

    @Override
    public void setUpload(String localEntityName, boolean upload) {
        settingsManager.setUpload(localEntityName, upload);
    }

    @Override
    public void setExternalEntityName(String localEntityName, String externalEntityName) {
        settingsManager.setExternalEntityName(localEntityName, externalEntityName);
        updateMappingsForListView(localEntityName);
    }

    @Override
    public void setScript(String localEntityName, String script) {
        settingsManager.setScript(localEntityName, script);
        updateMappingsForListView(localEntityName);
    }

    @Override
    public void setPropertyValue(String localEntityName,
                                 String property,
                                 PropertyFillType type,
                                 String value,
                                 String attribute) {
        settingsManager.setPropertyValue(localEntityName, property, type, value, attribute);
        updateMappingsForListView(localEntityName);
    }

    @Override
    public void setBatchSize(Integer batchSize) {
        settingsManager.setBatchSize(batchSize);
    }

    @Override
    public void setDuplicateEntityPolicy(String id) {
        settingsManager.setDuplicateEntityPolicy(DuplicateEntityPolicy.fromId(id));
    }

    @Override
    public void setErrorStrategy(String id) {
        settingsManager.setErrorStrategy(ImportErrorPolicy.fromId(id));
    }

    @Override
    public List<String> duplicateEntityPolicy() {
        return List.of(
                DuplicateEntityPolicy.SKIP.toString());
    }

    @Override
    public List<String> errorStrategies() {
        return List.of(
                ImportErrorPolicy.SKIP.toString(),
                ImportErrorPolicy.ABORT.toString());
    }
}