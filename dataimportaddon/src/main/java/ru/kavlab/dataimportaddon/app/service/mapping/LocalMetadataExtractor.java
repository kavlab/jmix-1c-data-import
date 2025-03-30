package ru.kavlab.dataimportaddon.app.service.mapping;

import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaProperty;
import org.springframework.stereotype.Component;
import ru.kavlab.dataimportaddon.app.data.PropertyInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LocalMetadataExtractor {

    public Map<String, Map<String, PropertyInfo>> extract(Metadata metadata, MetadataTools metadataTools) {
        Map<String, Map<String, PropertyInfo>> localMetadata = new HashMap<>();
        metadata.getSession().getClasses().stream()
                .filter(metadataTools::isJpaEntity)
                .filter(entity -> !metadataTools.isSystemLevel(entity))
                .forEach(entity -> {
                    Map<String, PropertyInfo> properties = entity.getProperties().stream()
                            .filter(property -> property.getRange().isDatatype())
                            .filter(property -> !property.getName().equals("id"))
                            .collect(Collectors.toMap(
                                    MetaProperty::getName,
                                    PropertyInfo::createPropertyInfo
                            ));
                    localMetadata.put(entity.getName(), properties);
                });
        return localMetadata;
    }
}
