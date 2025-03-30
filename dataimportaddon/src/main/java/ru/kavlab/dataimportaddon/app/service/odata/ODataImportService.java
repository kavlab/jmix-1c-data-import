package ru.kavlab.dataimportaddon.app.service.odata;

import io.jmix.flowui.backgroundtask.TaskLifeCycle;

import java.util.List;

public interface ODataImportService {

    void setConnectionSettings(String baseURL, String user, String pass);

    boolean extractMetadata();

    boolean isMetadataExtracted();

    int getTotalCountOfEntities();

    void importAllData();

    void importAllDataInBatches(TaskLifeCycle<Integer> taskLifeCycle,
                                int batchSize,
                                int totalCount) throws InterruptedException;

    List<String> getExternalMetadataNames();

    List<String> getExternalPropertyNamesByEntity(String externalEntityName);
}
