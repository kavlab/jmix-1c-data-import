package ru.kavlab.dataimportaddon.app.background;

import io.jmix.core.Messages;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.backgroundtask.BackgroundTask;
import io.jmix.flowui.backgroundtask.TaskLifeCycle;
import io.jmix.flowui.view.StandardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.kavlab.dataimportaddon.app.service.odata.ODataImportService;

import java.util.concurrent.TimeUnit;

@Component("imp1c_ConnectionTaskFactory")
public class ConnectionTaskFactory {

    @Autowired
    private ODataImportService oDataImportService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Messages messages;

    public ConnectionTaskFactory() {
    }

    public BackgroundTask<Integer, Boolean> create(String url,
                                                   String user,
                                                   String password,
                                                   Runnable onComplete,
                                                   StandardView view) {
        return new ConnectionTask(
                view,
                oDataImportService,
                notifications,
                messages,
                url,
                user,
                password,
                onComplete
        );
    }

    private static class ConnectionTask extends BackgroundTask<Integer, Boolean> {

        private final ODataImportService oDataImportService;
        private final Notifications notifications;
        private final Messages messages;
        private final String url;
        private final String user;
        private final String password;
        Runnable onComplete;

        public ConnectionTask(StandardView view,
                              ODataImportService oDataImportService,
                              Notifications notifications,
                              Messages messages,
                              String url,
                              String user,
                              String password,
                              Runnable onComplete) {
            super(60, TimeUnit.SECONDS, view);
            this.oDataImportService = oDataImportService;
            this.notifications = notifications;
            this.messages = messages;
            this.url = url;
            this.user = user;
            this.password = password;
            this.onComplete = onComplete;
        }

        @Override
        public Boolean run(TaskLifeCycle<Integer> taskLifeCycle) {
            oDataImportService.setConnectionSettings(url, user, password);
            return oDataImportService.extractMetadata();
        }

        @Override
        public boolean handleTimeoutException() {
            notifications.show(messages.getMessage(
                    "ru.kavlab.dataimportaddon.notifications/timeout"));
            return super.handleTimeoutException();
        }

        @Override
        public void done(Boolean result) {
            super.done(result);
            if (result) {
                onComplete.run();
                notifications.show(messages.getMessage(
                        "ru.kavlab.dataimportaddon.notifications/metadata-extracted"));
            } else {
                notifications.show(messages.getMessage(
                        "ru.kavlab.dataimportaddon.notifications/failed"));
            }
        }
    }
}
