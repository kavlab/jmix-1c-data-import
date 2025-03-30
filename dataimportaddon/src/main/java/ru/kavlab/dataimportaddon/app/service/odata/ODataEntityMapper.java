package ru.kavlab.dataimportaddon.app.service.odata;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.jmix.core.Metadata;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.model.MetaClass;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.eclipse.persistence.jpa.jpql.parser.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kavlab.dataimportaddon.app.data.*;
import ru.kavlab.dataimportaddon.app.service.mapping.MappingServiceImpl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ODataEntityMapper {

    private static final Logger log = LoggerFactory.getLogger(ODataEntityMapper.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final Metadata metadata;
    private final MappingServiceImpl mappingService;
    private final List<ExternalMetadata> externalMetadataList;

    public ODataEntityMapper(Metadata metadata,
                             MappingServiceImpl mappingService,
                             List<ExternalMetadata> externalMetadataList) {
        this.metadata = metadata;
        this.mappingService = mappingService;
        this.externalMetadataList = externalMetadataList;
    }

    public Object mapEntity(MappingEntity mappingEntity,
                            ClientEntity clientEntity,
                            boolean skipEntity) {
        MetaClass metaClass = metadata.getClass(mappingEntity.getEntityNameJmix());
        Object newEntity = metadata.create(metaClass.getJavaClass());
        EntityValues.setId(newEntity, UUID.randomUUID());

        fillAttributes(mappingEntity, clientEntity, newEntity);
        fillConstants(mappingEntity, newEntity);
        applyGroovyScript(mappingEntity, clientEntity, newEntity, skipEntity);
        return newEntity;
    }

    private void fillAttributes(MappingEntity mappingEntity, ClientEntity clientEntity, Object newEntity) {
        mappingEntity.getMappingProperties().stream()
                .filter(prop -> PropertyFillType.ATTRIBUTE.equals(prop.getType()))
                .forEach(prop -> {
                    PropertyInfo propertyInfo = mappingService.getLocalMetadata()
                            .get(mappingEntity.getEntityNameJmix())
                            .get(prop.getJmixProperty());
                    getExternalProperty(mappingEntity.getEntityName1C(), prop.getAttribute())
                            .ifPresent(extProp -> {
                                Optional<?> valueOpt = getValue(clientEntity, propertyInfo,
                                        extProp, propertyInfo.getJavaType());
                                valueOpt.ifPresent(value -> EntityValues.setValue(
                                        newEntity, propertyInfo.getName(), value));
                            });
                });
    }

    private void fillConstants(MappingEntity mappingEntity, Object newEntity) {
        mappingEntity.getMappingProperties().stream()
                .filter(prop -> PropertyFillType.CONSTANT.equals(prop.getType()))
                .forEach(prop -> EntityValues.setValue(
                        newEntity, prop.getJmixProperty(), prop.getValue()));
    }

    private void applyGroovyScript(MappingEntity mappingEntity,
                                   ClientEntity clientEntity,
                                   Object newEntity,
                                   boolean skipEntity) {
        String script = mappingEntity.getScript();
        if (script != null && !script.isEmpty()) {
            Map<String, Object> externalProperties = new HashMap<>();
            List<ExternalProperty> extProperties = getExternalPropertiesByEntity(mappingEntity.getEntityName1C());
            for (ExternalProperty extProp : extProperties) {
                String attrName = extProp.name();
                Optional<?> valueOpt = switch (extProp.type()) {
                    case "Edm.String", "Edm.Guid" -> getStringValue(clientEntity, attrName);
                    case "Edm.Int16", "Edm.Int32", "Edm.Int64" -> getIntValue(clientEntity, attrName);
                    case "Edm.Double" -> getDoubleValue(clientEntity, attrName);
                    case "Edm.DateTime" -> getDateValue(clientEntity, attrName);
                    case "Edm.Boolean" -> getBooleanValue(clientEntity, attrName);
                    default -> Optional.empty();
                };
                valueOpt.ifPresent(value -> externalProperties.put(attrName, value));
            }
            try {
                Binding binding = new Binding();
                binding.setVariable("newEntity", newEntity);
                binding.setVariable("externalProperties", externalProperties);
                binding.setVariable("skipEntity", skipEntity);
                GroovyShell shell = new GroovyShell(binding);
                shell.evaluate(script);
            } catch (Exception e) {
                log.error("Error executing Groovy script for MappingEntity: {}",
                        mappingEntity.getEntityNameJmix(), e);
            }
        }
    }

    private Optional<String> getStringValue(ClientEntity entity, String property) {
        ClientProperty clientProperty = entity.getProperty(property);
        return clientProperty != null ? Optional.of(clientProperty.getValue().toString()) : Optional.empty();
    }

    private Optional<Integer> getIntValue(ClientEntity entity, String property) {
        ClientProperty clientProperty = entity.getProperty(property);
        if (clientProperty != null) {
            try {
                return Optional.ofNullable(clientProperty.getValue().asPrimitive().toCastValue(Integer.class));
            } catch (EdmPrimitiveTypeException e) {
                log.debug("Type casting error for the property {} to Integer", property, e);
            }
        }
        return Optional.empty();
    }

    private Optional<Double> getDoubleValue(ClientEntity entity, String property) {
        ClientProperty clientProperty = entity.getProperty(property);
        if (clientProperty != null) {
            try {
                return Optional.ofNullable(clientProperty.getValue().asPrimitive().toCastValue(Double.class));
            } catch (EdmPrimitiveTypeException e) {
                log.debug("Type casting error for the property {} to Double", property, e);
            }
        }
        return Optional.empty();
    }

    private Optional<Boolean> getBooleanValue(ClientEntity entity, String property) {
        ClientProperty clientProperty = entity.getProperty(property);
        if (clientProperty != null) {
            try {
                return Optional.of(clientProperty.getValue().asPrimitive().toCastValue(Boolean.class));
            } catch (EdmPrimitiveTypeException e) {
                log.debug("Type casting error for the property {} to Boolean", property, e);
            }
        }
        return Optional.empty();
    }

    private Optional<Date> getDateValue(ClientEntity entity, String property) {
        return getStringValue(entity, property).flatMap(dateStr -> {
            try {
                return Optional.of(DATE_FORMAT.parse(dateStr));
            } catch (ParseException e) {
                log.debug("Date parsing error for the property {}", property, e);
                return Optional.empty();
            }
        });
    }

    private <T> Optional<T> getValue(ClientEntity entity,
                                     PropertyInfo localProperty,
                                     ExternalProperty externalProperty,
                                     Class<T> clazz) {
        ClientProperty clientProperty = entity.getProperty(externalProperty.name());
        if (clientProperty == null || clientProperty.getValue() == null) {
            return Optional.empty();
        }
        String rawValue = clientProperty.getValue().asPrimitive().toString();
        return convertValue(clientProperty, rawValue, localProperty, externalProperty, clazz);
    }

    <T> Optional<T> convertValue(ClientProperty clientProperty,
                                 String rawValue,
                                 PropertyInfo localProperty,
                                 ExternalProperty externalProperty,
                                 Class<T> clazz) {
        // Edm.Guid -> String
        if ("Edm.Guid".equals(externalProperty.type())) {
            if (clazz.equals(String.class)) {
                return Optional.of(clazz.cast(rawValue));
            } else {
                log.warn("Edm.Guid can only be converted to String, type requested: " + clazz.getSimpleName());
                return Optional.empty();
            }
        }

        // String
        if (clazz.equals(String.class)) {
            if (localProperty.getMaxLength() > 0 && rawValue.length() > localProperty.getMaxLength()) {
                String truncated = rawValue.substring(0, localProperty.getMaxLength());
                return Optional.of(clazz.cast(truncated));
            } else {
                return Optional.of(clazz.cast(rawValue));
            }
        }
        // Date, DateTime
        else if (clazz.equals(DateTime.class) || clazz.equals(Date.class)) {
            try {
                return Optional.of(clazz.cast(DATE_FORMAT.parse(rawValue)));
            } catch (ParseException e) {
                log.debug("Date parsing error for the value {}", rawValue, e);
                return Optional.empty();
            }
        }
        // Integer, Long, Short
        else if (clazz.equals(Integer.class)) {
            try {
                return Optional.of(clazz.cast(Integer.parseInt(rawValue)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        else if (clazz.equals(Long.class)) {
            try {
                return Optional.of(clazz.cast(Long.parseLong(rawValue)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        else if (clazz.equals(Short.class)) {
            try {
                return Optional.of(clazz.cast(Short.parseShort(rawValue)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        // BigDecimal, BigInteger
        else if (clazz.equals(BigDecimal.class)) {
            try {
                return Optional.of(clazz.cast(new BigDecimal(rawValue)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        else if (clazz.equals(BigInteger.class)) {
            try {
                return Optional.of(clazz.cast(new BigInteger(rawValue)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        // Boolean
        else if (clazz.equals(Boolean.class)) {
            return Optional.of(clazz.cast(Boolean.parseBoolean(rawValue)));
        }
        // Character
        else if (clazz.equals(Character.class)) {
            if (rawValue.length() == 1) {
                return Optional.of(clazz.cast(rawValue.charAt(0)));
            } else {
                return Optional.empty();
            }
        }
        // Double, float
        else if (clazz.equals(Double.class)) {
            try {
                return Optional.of(clazz.cast(Double.parseDouble(rawValue)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        else if (clazz.equals(Float.class)) {
            try {
                return Optional.of(clazz.cast(Float.parseFloat(rawValue)));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        // LocalDate, LocalDateTime, LocalTime, OffsetDateTime, OffsetTime
        else if (clazz.equals(LocalDate.class)) {
            try {
                // if the format is "yyyy-MM-dd'T'HH:mm:ss" then extract the part
                String datePart = rawValue.length() >= 10 ? rawValue.substring(0, 10) : rawValue;
                return Optional.of(clazz.cast(LocalDate.parse(datePart)));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
        else if (clazz.equals(LocalDateTime.class)) {
            try {
                return Optional.of(clazz.cast(LocalDateTime.parse(rawValue)));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
        else if (clazz.equals(LocalTime.class)) {
            try {
                return Optional.of(clazz.cast(LocalTime.parse(rawValue)));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
        else if (clazz.equals(OffsetDateTime.class)) {
            try {
                return Optional.of(clazz.cast(OffsetDateTime.parse(rawValue)));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
        else if (clazz.equals(OffsetTime.class)) {
            try {
                return Optional.of(clazz.cast(OffsetTime.parse(rawValue)));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
        // Time
        else if (clazz.equals(Time.class)) {
            try {
                LocalTime lt = LocalTime.parse(rawValue);
                return Optional.of(clazz.cast(Time.valueOf(lt)));
            } catch (DateTimeParseException e) {
                return Optional.empty();
            }
        }
        // Olingo cast
        else {
            try {
                T value = clientProperty.getValue().asPrimitive().toCastValue(clazz);
                return Optional.ofNullable(value);
            } catch (EdmPrimitiveTypeException e) {
                log.error("Type casting error for the property " + externalProperty.name() +
                        ", the type: " + clazz.getSimpleName(), e);
                return Optional.empty();
            }
        }
    }

    private Optional<ExternalProperty> getExternalProperty(String entityName, String propertyName) {
        return externalMetadataList.stream()
                .filter(meta -> meta.name().equals(entityName))
                .findFirst()
                .flatMap(meta -> meta.externalProperties().stream()
                        .filter(prop -> prop.name().equals(propertyName))
                        .findFirst());
    }

    public List<ExternalProperty> getExternalPropertiesByEntity(String entityName) {
        return externalMetadataList.stream()
                .filter(meta -> meta.name().equals(entityName))
                .findFirst()
                .map(ExternalMetadata::externalProperties)
                .orElseGet(ArrayList::new);
    }
}
