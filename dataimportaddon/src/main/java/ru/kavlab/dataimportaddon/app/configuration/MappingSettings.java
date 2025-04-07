package ru.kavlab.dataimportaddon.app.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.kavlab.dataimportaddon.app.data.MappingEntity;

import java.util.List;
import java.util.Optional;

public class MappingSettings {
    private final List<MappingEntity> mappingEntities;

    private Integer batchSize;
    private DuplicateEntityPolicy loadingStrategy;
    private ImportErrorPolicy errorStrategy;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MappingSettings(@JsonProperty("mapping_entities") List<MappingEntity> mappingEntities,
                           @JsonProperty("batch_size") Integer batchSize,
                           @JsonProperty("loading_strategy") DuplicateEntityPolicy loadingStrategy,
                           @JsonProperty("error_strategy") ImportErrorPolicy errorStrategy) {
        this.mappingEntities = mappingEntities;
        this.batchSize = batchSize;
        this.loadingStrategy = loadingStrategy;
        this.errorStrategy = errorStrategy;
    }

    @JsonProperty("mapping_entities")
    public List<MappingEntity> getMappingEntities() {
        return mappingEntities;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public DuplicateEntityPolicy getLoadingStrategy() {
        return loadingStrategy;
    }

    public void setLoadingStrategy(DuplicateEntityPolicy loadingStrategy) {
        this.loadingStrategy = loadingStrategy;
    }

    public ImportErrorPolicy getErrorStrategy() {
        return errorStrategy;
    }

    public void setErrorStrategy(ImportErrorPolicy errorStrategy) {
        this.errorStrategy = errorStrategy;
    }

    public Optional<MappingEntity> getMappingEntity(String localEntityName) {
        return mappingEntities.stream()
                .filter(mappingEntity -> mappingEntity.getEntityNameJmix().equals(localEntityName))
                .findFirst();
    }
}
