package ru.kavlab.dataimportaddon.app.service.odata;

import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import io.jmix.flowui.backgroundtask.TaskLifeCycle;
import org.apache.olingo.client.api.communication.request.retrieve.ODataValueRequest;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.kavlab.dataimportaddon.app.configuration.ImportErrorPolicy;
import ru.kavlab.dataimportaddon.app.data.*;
import ru.kavlab.dataimportaddon.app.service.mapping.MappingServiceImpl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service("imp1c_ODataImportServiceImpl")
public class ODataImportServiceImpl implements ODataImportService {

    private static final Logger log = LoggerFactory.getLogger(ODataImportServiceImpl.class);

    private static final int MAX_SELECT_LENGTH = 70;

    private ODataClientProvider clientProvider;
    private ODataEntityMapper entityMapper;
    private ODataMetadataExtractor metadataExtractor;
    private final MappingServiceImpl mappingService;
    private final DataManager dataManager;
    private final Metadata metadata;

    private boolean isMetadataExtracted;

    public ODataImportServiceImpl(MappingServiceImpl mappingService,
                                  DataManager dataManager,
                                  Metadata metadata) {
        this.mappingService = mappingService;
        this.dataManager = dataManager;
        this.metadata = metadata;
        this.isMetadataExtracted = false;
    }

    @Override
    public void setConnectionSettings(String baseURL, String user, String pass) {
        clientProvider = new ODataClientProvider(baseURL, user, pass);
    }

    @Override
    public boolean extractMetadata() {
        isMetadataExtracted = false;
        if (clientProvider == null) {
            return false;
        } else {
            metadataExtractor = new ODataMetadataExtractor(clientProvider);
            isMetadataExtracted = metadataExtractor.extractMetadata();
            if (isMetadataExtracted) {
                entityMapper = new ODataEntityMapper(
                        metadata,
                        mappingService,
                        metadataExtractor.getExternalMetadata()
                );
            }
            return isMetadataExtracted;
        }
    }

    @Override
    public boolean isMetadataExtracted() {
        return isMetadataExtracted;
    }

    @Override
    public int getTotalCountOfEntities() {
        int totalCount = 0;
        for (MappingEntity entity : mappingService.getMappingSettings().getMappingEntities()) {
            if (entity.getEntityName1C() != null
                    && !entity.getEntityName1C().isEmpty()
                    && entity.isUpload()) {
                URI countUri = clientProvider.getClient()
                        .newURIBuilder(clientProvider.getOdataUrl())
                        .appendEntitySetSegment(entity.getEntityName1C())
                        .count()
                        .build();
                ODataValueRequest request = clientProvider.getClient()
                        .getRetrieveRequestFactory().getValueRequest(countUri);
                request.setAccept("application/json");
                var response = request.execute();
                try {
                    totalCount += Integer.parseInt(response.getBody().toString());
                } catch (NumberFormatException e) {
                    log.error("Get count error for the entity {}", entity.getEntityName1C(), e);
                }
            }
        }
        return totalCount;
    }

    @Override
    public void importAllData() {
        if (entityMapper != null) {
            mappingService.getMappingSettings().getMappingEntities().stream()
                    .filter(entity -> entity.getEntityName1C() != null)
                    .filter(MappingEntity::isUpload)
                    .forEach(this::importObjects);
        }
    }

    private void importObjects(MappingEntity mappingEntity) {
        String selectedProperties = prepareSelectedProperties(mappingEntity);
        URI uri = clientProvider.buildEntitySetUri(mappingEntity.getEntityName1C(), selectedProperties);
        var iterator = clientProvider.getEntityIterator(uri);

        while (iterator.hasNext()) {
            ClientEntity clientEntity = iterator.next();
            try {
                AtomicBoolean skipEntity = new AtomicBoolean(false);
                Object newEntity = entityMapper.mapEntity(mappingEntity, clientEntity, skipEntity);
                if (!skipEntity.get()) {
                    dataManager.save(newEntity);
                }
            } catch (Exception e) {
                log.error("Process entity error: ", e);
                if (mappingService.getMappingSettings().getErrorStrategy().equals(ImportErrorPolicy.ABORT)) {
                    throw new RuntimeException("Process entity error: " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void importAllDataInBatches(TaskLifeCycle<Integer> taskLifeCycle,
                                       int batchSize,
                                       int totalCount) throws InterruptedException {
        if (entityMapper != null) {
            for (MappingEntity mappingEntity : mappingService.getMappingSettings().getMappingEntities()) {
                if (!mappingEntity.isUpload()) {
                    continue;
                }

                String selectedProperties = prepareSelectedProperties(mappingEntity);

                int offset = 0;
                while (true) {
                    URI uri = clientProvider.getClient().newURIBuilder(clientProvider.getOdataUrl())
                            .appendEntitySetSegment(mappingEntity.getEntityName1C())
                            .select(selectedProperties)
                            .orderBy("Ref_Key")
                            .top(batchSize)
                            .skip(offset)
                            .build();
                    var iterator = clientProvider.getEntityIterator(uri);
                    if (!iterator.hasNext()) break;

                    while (iterator.hasNext()) {
                        ClientEntity clientEntity = iterator.next();

                        var errorStrategies = mappingService.getMappingSettings().getErrorStrategy();
                        try {
                            AtomicBoolean skipEntity = new AtomicBoolean(false);
                            Object newEntity = entityMapper.mapEntity(mappingEntity, clientEntity, skipEntity);
                            if (!skipEntity.get()) {
                                dataManager.save(newEntity);
                            }
                        } catch (Exception e) {
                            log.error("Process entity error: ", e);
                            if (errorStrategies.equals(ImportErrorPolicy.ABORT)) {
                                throw new RuntimeException("Process entity error: " + e.getMessage());
                            }
                        }

                        taskLifeCycle.publish(Math.min(offset + batchSize, totalCount));
                        if (taskLifeCycle.isCancelled()) {
                            return;
                        }
                    }
                    offset += batchSize;
                }
            }
        }
    }

    private String prepareSelectedProperties(MappingEntity mappingEntity) {
        String selectedProperties = mappingEntity.getMappingProperties().stream()
                .filter(prop -> prop.getType() != null
                        && prop.getType().equals(PropertyFillType.ATTRIBUTE))
                .map(MappingProperty::getAttribute)
                .collect(Collectors.joining(","));

        String script = mappingEntity.getScript();
        List<ExternalProperty> extProperties = metadataExtractor.getExternalMetadata().stream()
                .filter(meta -> meta.name().equals(mappingEntity.getEntityName1C()))
                .findFirst()
                .map(ExternalMetadata::externalProperties)
                .orElseGet(ArrayList::new);

        if (script != null && !script.isEmpty()) {
            String finalSelectedProperties = selectedProperties;
            String addSelectedProperties = extProperties.stream()
                    .map(ExternalProperty::name)
                    .filter(script::contains)
                    .filter(prop -> !finalSelectedProperties.contains(prop))
                    .collect(Collectors.joining(","));
            if (!addSelectedProperties.isEmpty()) {
                selectedProperties += "," + addSelectedProperties;
            }
        }
        if (selectedProperties.length() > MAX_SELECT_LENGTH) {
            selectedProperties = "*";
        }
        return selectedProperties;
    }

    @Override
    public List<String> getExternalMetadataNames() {
        if (isMetadataExtracted) {
            return metadataExtractor.getExternalMetadata().stream()
                    .map(ExternalMetadata::name)
                    .toList();
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getExternalPropertyNamesByEntity(String externalEntityName) {
        if (isMetadataExtracted) {
            return entityMapper.getExternalPropertiesByEntity(externalEntityName).stream()
                    .map(ExternalProperty::name)
                    .toList();
        } else {
            return new ArrayList<>();
        }
    }
}
