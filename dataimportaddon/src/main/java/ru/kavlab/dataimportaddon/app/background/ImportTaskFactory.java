package ru.kavlab.dataimportaddon.app.background;

import io.jmix.core.Messages;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.backgroundtask.BackgroundTask;
import io.jmix.flowui.backgroundtask.TaskLifeCycle;
import io.jmix.flowui.view.StandardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.kavlab.dataimportaddon.app.service.mapping.MappingService;
import ru.kavlab.dataimportaddon.app.service.odata.ODataImportService;

import java.util.concurrent.TimeUnit;

@Component("imp1c_ImportTaskFactory")
public class ImportTaskFactory {

    @Autowired
    private ODataImportService oDataImportService;
    @Autowired
    private MappingService mappingService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Messages messages;

    public BackgroundTask<Integer, Boolean> create(int totalCount,
                                                   Runnable onComplete,
                                                   StandardView view) {

        return new ImportTask(
                view,
                oDataImportService,
                mappingService,
                notifications,
                messages,
                totalCount,
                onComplete
        );
    }

    private static class ImportTask extends BackgroundTask<Integer, Boolean> {

        private final ODataImportService oDataImportService;
        private final MappingService mappingService;
        private final Notifications notifications;
        private final Messages messages;
        private final int totalCount;
        private final Runnable onComplete;

        public ImportTask(StandardView view,
                          ODataImportService oDataImportService,
                          MappingService mappingService,
                          Notifications notifications,
                          Messages messages,
                          int totalCount,
                          Runnable onComplete) {
            super(60, TimeUnit.SECONDS, view);
            this.oDataImportService = oDataImportService;
            this.mappingService = mappingService;
            this.notifications = notifications;
            this.messages = messages;
            this.totalCount = totalCount;
            this.onComplete = onComplete;
        }

        @Override
        public Boolean run(TaskLifeCycle<Integer> taskLifeCycle) throws InterruptedException {
            int batchSize = mappingService.getMappingSettings().getBatchSize();

            if (batchSize == 0) {
                oDataImportService.importAllData();
                taskLifeCycle.publish(1);
            } else {
                oDataImportService.importAllDataInBatches(taskLifeCycle, batchSize, totalCount);
            }
            return true;
        }

        @Override
        public void done(Boolean result) {
            if (result) {
                notifications.show(messages.getMessage(
                        "ru.kavlab.dataimportaddon.notifications/import-successfully"));
                onComplete.run();
            }
        }

        @Override
        public boolean handleException(Exception ex) {
            notifications.show( messages.getMessage(
                    "ru.kavlab.dataimportaddon.notifications/import-error"
            ) + " " + ex.getMessage());
            return true;
        }
    }
}
