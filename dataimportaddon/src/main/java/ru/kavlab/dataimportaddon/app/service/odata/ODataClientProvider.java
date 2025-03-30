package ru.kavlab.dataimportaddon.app.service.odata;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetIteratorRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientEntitySetIterator;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.client.core.http.BasicAuthHttpClientFactory;

import java.net.URI;

public class ODataClientProvider {
    private final ODataClient client;
    private final String baseUrl;
    private final String user;
    private final String pass;

    public ODataClientProvider(String baseUrl, String user, String pass) {
        this.client = ODataClientFactory.getClient();
        this.baseUrl = baseUrl;
        this.user = user;
        this.pass = pass;
        configure();
    }

    private void configure() {
        client.getConfiguration().setHttpClientFactory(
                new BasicAuthHttpClientFactory(user, pass == null ? "" : pass)
        );
    }

    public String getOdataUrl() {
        return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "odata/standard.odata/";
    }

    public URI buildEntitySetUri(String entitySet, String selectClause) {
        return client.newURIBuilder(getOdataUrl())
                .appendEntitySetSegment(entitySet)
                .select(selectClause)
                .build();
    }

    public ClientEntitySetIterator<ClientEntitySet, ClientEntity> getEntityIterator(URI uri) {
        ODataEntitySetIteratorRequest<ClientEntitySet, ClientEntity> request =
                client.getRetrieveRequestFactory().getEntitySetIteratorRequest(uri);
        request.setAccept("application/json");
        ODataRetrieveResponse<ClientEntitySetIterator<ClientEntitySet, ClientEntity>> response = request.execute();
        return response.getBody();
    }

    public ODataClient getClient() {
        return client;
    }
}
