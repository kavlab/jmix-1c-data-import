package ru.kavlab.dataimportaddon.app.service.odata;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import ru.kavlab.dataimportaddon.app.data.ExternalMetadata;
import ru.kavlab.dataimportaddon.app.data.ExternalProperty;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ODataMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(ODataMetadataExtractor.class);
    private static final List<String> ALLOWED_TYPES = List.of(
            "Edm.String",
            "Edm.Int16",
            "Edm.Int32",
            "Edm.Int64",
            "Edm.DateTime",
            "Edm.Double",
            "Edm.Boolean"
    );

    private final List<ExternalMetadata> externalMetadata = new ArrayList<>();

    private final ODataClientProvider clientProvider;

    public ODataMetadataExtractor(ODataClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    public Boolean extractMetadata() {
        try {
            ODataClient client = clientProvider.getClient();
            URI metadataUri = client.newURIBuilder(clientProvider.getOdataUrl())
                    .appendMetadataSegment()
                    .build();

            ODataRetrieveResponse<ClientEntitySet> response = client.getRetrieveRequestFactory()
                    .getEntitySetRequest(metadataUri)
                    .execute();

            InputStream inputStream = response.getRawResponse();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String metadataContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                return parseMetadata(metadataContent);
            }
        } catch (Exception e) {
            log.error("Metadata extracting error", e);
        }
        return false;
    }

    private Boolean parseMetadata(String metadataContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(metadataContent)));

            NodeList entityTypeNodes = doc.getElementsByTagName("EntityType");
            for (int i = 0; i < entityTypeNodes.getLength(); i++) {
                Element entityTypeElement = (Element) entityTypeNodes.item(i);
                String entityName = entityTypeElement.getAttribute("Name");

                if (!entityName.startsWith("Catalog_") &&
                        !entityName.startsWith("Document_") &&
                        !entityName.startsWith("ExchangePlan_") &&
                        !entityName.startsWith("ChartOfAccounts_") &&
                        !entityName.startsWith("ChartOfCalculationTypes_") &&
                        !entityName.startsWith("ChartOfCharacteristicTypes_") &&
                        !entityName.startsWith("BusinessProcess_") &&
                        !entityName.startsWith("Task_")) {
                    continue;
                }

                List<ExternalProperty> propertyList = new ArrayList<>();
                log.debug("Process entity: {}", entityName);

                NodeList propertyNodes = entityTypeElement.getElementsByTagName("Property");
                for (int j = 0; j < propertyNodes.getLength(); j++) {
                    Element propertyElement = (Element) propertyNodes.item(j);
                    String propertyName = propertyElement.getAttribute("Name");
                    String propertyType = propertyElement.getAttribute("Type");
                    boolean nullable = "true".equals(propertyElement.getAttribute("Nullable"));

                    if ((propertyName.equals("Ref_Key") || ALLOWED_TYPES.contains(propertyType))
                            && !propertyName.endsWith("_Type")
                            && !propertyName.endsWith("_Base64Data")) {
                        propertyList.add(new ExternalProperty(propertyName, propertyType, nullable));
                    } else {
                        log.debug("Property is skipped: {}, Type: {}, Nullable: {}", propertyName, propertyType, nullable);
                    }
                }
                externalMetadata.add(new ExternalMetadata(entityName, propertyList));
            }
            return true;
        } catch (Exception e) {
            log.error("Metadata extracting error", e);
        }
        return false;
    }

    public List<ExternalMetadata> getExternalMetadata() {
        return externalMetadata;
    }
}
