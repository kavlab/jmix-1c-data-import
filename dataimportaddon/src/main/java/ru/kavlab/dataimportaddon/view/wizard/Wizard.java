package ru.kavlab.dataimportaddon.view.wizard;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.LoadContext;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.backgroundtask.BackgroundTask;
import io.jmix.flowui.component.accordion.JmixAccordion;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.component.textfield.JmixNumberField;
import io.jmix.flowui.component.textfield.JmixPasswordField;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.upload.FileStorageUploadField;
import io.jmix.flowui.component.upload.receiver.FileTemporaryStorageBuffer;
import io.jmix.flowui.download.DownloadFormat;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.upload.TemporaryStorage;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.kavlab.dataimportaddon.app.background.ConnectionTaskFactory;
import ru.kavlab.dataimportaddon.app.background.ImportTaskFactory;
import ru.kavlab.dataimportaddon.app.configuration.DuplicateEntityPolicy;
import ru.kavlab.dataimportaddon.app.configuration.ImportErrorPolicy;
import ru.kavlab.dataimportaddon.app.data.MappingForListView;
import ru.kavlab.dataimportaddon.app.service.mapping.MappingServiceImpl;
import ru.kavlab.dataimportaddon.app.service.odata.ODataImportService;
import ru.kavlab.dataimportaddon.view.attributesmapping.AttributesMappingView;
import ru.kavlab.dataimportaddon.view.entitymapping.EntityMappingView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Route(value = "DataImportWizard", layout = DefaultMainViewParent.class)
@ViewController("Wizard")
@ViewDescriptor("wizard.xml")
public class Wizard extends StandardView {

    @Autowired
    private Notifications notifications;
    @Autowired
    private Dialogs dialogs;

    @ViewComponent
    private CollectionLoader<MappingForListView> entitiesMappingsDl;
    @ViewComponent
    private CollectionContainer<MappingForListView> entitiesMappingsDc;

    @ViewComponent
    private JmixPasswordField passwordField;
    @ViewComponent
    private TypedTextField<Object> urlField;
    @ViewComponent
    private TypedTextField<Object> userField;
    @ViewComponent
    private JmixTextArea settingsArea;
    @ViewComponent
    private JmixAccordion panels;
    @ViewComponent
    private AccordionPanel entitiesMappingPanel;
    @ViewComponent
    private AccordionPanel configurationPanel;
    @ViewComponent
    private JmixComboBox<String> loadingStrategy;
    @ViewComponent
    private JmixNumberField batchSizeField;
    @ViewComponent
    private JmixComboBox<String> errorStrategy;
    @ViewComponent
    private JmixButton entitiesNextBtn;
    @ViewComponent
    private JmixButton connectionNextBtn;
    @Autowired
    private MappingServiceImpl mappingService;
    @Autowired
    private ODataImportService dataImportService;

    @Value("${connection.base1c.url:}")
    private String baseUrl;
    @Value("${connection.base1c.user:}")
    private String baseUser;
    @Value("${connection.base1c.password:}")
    private String basePassword;

    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private TemporaryStorage temporaryStorage;
    @Autowired
    private Downloader downloader;

    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private ImportTaskFactory importTaskFactory;
    @Autowired
    private ConnectionTaskFactory connectionTaskFactory;

    @Subscribe
    public void onInit(final InitEvent event) {
        urlField.setHelperText("http://server/base");
        urlField.setValue(baseUrl);
        userField.setValue(baseUser);
        passwordField.setValue(basePassword);

        settingsArea.getStyle().set("resize", "both");
        settingsArea.getStyle().set("overflow", "auto");

        batchSizeField.addValueChangeListener(valueChangeEvent -> {
            mappingService.setBatchSize(valueChangeEvent.getValue().intValue());
            settingsArea.setValue(mappingService.getMappingSettingsAsString());
        });
        batchSizeField.setValue(0.0);

        loadingStrategy.setItems(mappingService.duplicateEntityPolicy());
        loadingStrategy.addValueChangeListener(valueChangeEvent -> {
            mappingService.setDuplicateEntityPolicy(valueChangeEvent.getValue());
            settingsArea.setValue(mappingService.getMappingSettingsAsString());
        });
        loadingStrategy.setValue(DuplicateEntityPolicy.SKIP.toString());

        errorStrategy.setItems(mappingService.errorStrategies());
        errorStrategy.addValueChangeListener(valueChangeEvent -> {
            mappingService.setErrorStrategy(valueChangeEvent.getValue());
            settingsArea.setValue(mappingService.getMappingSettingsAsString());
        });
        errorStrategy.setValue(ImportErrorPolicy.SKIP.toString());

        setAvailability();
    }

    private void setAvailability() {
        var isMetadataExtracted = dataImportService.isMetadataExtracted();

        entitiesMappingPanel.setEnabled(isMetadataExtracted);
        configurationPanel.setEnabled(isMetadataExtracted);
        connectionNextBtn.setEnabled(isMetadataExtracted);
        entitiesNextBtn.setEnabled(isMetadataExtracted);
    }

    @Subscribe("configurationPanel")
    public void onConfigurationPanelOpenedChange(final Details.OpenedChangeEvent event) {
        settingsArea.setValue(mappingService.getMappingSettingsAsString());
    }

    @Subscribe(id = "testConnectionBtn", subject = "clickListener")
    public void onTestConnectionBtnClick(final ClickEvent<JmixButton> event) {
        BackgroundTask<Integer, Boolean> task = connectionTaskFactory.create(
                urlField.getValue(),
                userField.getValue(),
                passwordField.getValue(),
                () -> {
                    setAvailability();
                    panels.open(entitiesMappingPanel);
                },
                Wizard.this
        );
        dialogs.createBackgroundTaskDialog(task)
                .withHeader("Connecting...")
                .withText("Please wait")
                .open();
    }

    @Subscribe(id = "connectionNextBtn", subject = "clickListener")
    public void onConnectionNextBtnClick(final ClickEvent<JmixButton> event) {
        panels.open(entitiesMappingPanel);
    }

    @Subscribe(id = "entitiesNextBtn", subject = "clickListener")
    public void onEntitiesNextBtnClick(final ClickEvent<JmixButton> event) {
        panels.open(configurationPanel);
    }

    @Subscribe(id = "startLoadingBtn", subject = "clickListener")
    public void onStartLoadingBtnClick(final ClickEvent<JmixButton> event) {
        int totalCount = 1;
        if (mappingService.getMappingSettings().getBatchSize() > 0) {
            totalCount = dataImportService.getTotalCountOfEntities();
        }

        BackgroundTask<Integer, Boolean> task = importTaskFactory.create(
                totalCount,
                () -> {},
                Wizard.this);

        dialogs.createBackgroundTaskDialog(task)
                .withHeader("Importing data")
                .withText("Please wait until all data has been imported")
                .withTotal(totalCount)
                .withShowProgressInPercentage(true)
                .withCancelAllowed(true)
                .open();
    }

    @Subscribe(id = "saveConfigToFile", subject = "clickListener")
    public void onSaveConfigToFileClick(final ClickEvent<JmixButton> event) {
        byte[] content = mappingService.getMappingSettingsAsString().getBytes(StandardCharsets.UTF_8);
        downloader.download(content, "import-addon.json", DownloadFormat.JSON);
    }

    @Subscribe("loadConfigFromFile")
    public void onLoadConfigFromFileFileUploadSucceeded(final FileUploadSucceededEvent<FileStorageUploadField> event) {
        Receiver receiver = event.getReceiver();
        if (receiver instanceof FileTemporaryStorageBuffer storageBuffer) {
            UUID fileId = Objects.requireNonNull(storageBuffer.getFileData()).getFileInfo().getId();
            File file = temporaryStorage.getFile(fileId);

            if (file != null) {
                String fileName = event.getFileName();
                if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
                    notifications.create("Invalid file format. Upload JSON.")
                            .withType(Notifications.Type.WARNING)
                            .show();
                    return;
                }

                String jsonContent;
                try {
                    jsonContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    notifications.create("Error reading file: " + e.getMessage())
                            .withType(Notifications.Type.ERROR)
                            .show();
                    return;
                }

                if (mappingService.loadMappingSettingsFromString(jsonContent)) {
                    settingsArea.setValue(mappingService.getMappingSettingsAsString());
                    batchSizeField.setValue(mappingService.getMappingSettings().getBatchSize().doubleValue());
                    errorStrategy.setValue(mappingService.getMappingSettings().getErrorStrategy().toString());
                    notifications.create("File uploaded successfully.")
                            .withType(Notifications.Type.SUCCESS)
                            .show();
                } else {
                    notifications.create("Invalid file format.")
                            .withType(Notifications.Type.ERROR)
                            .show();
                }
                entitiesMappingsDl.load();

                temporaryStorage.deleteFile(fileId);
            }

        }
    }

    @Install(to = "entitiesMappingsDl", target = Target.DATA_LOADER)
    private List<MappingForListView> entitiesMappingsDlLoadDelegate(final LoadContext<MappingForListView> ignoredLoadContext) {
        return mappingService.getMappingsForListView();
    }

    @Supply(to = "entitiesMappingsDataGrid.entityName1C", subject = "renderer")
    private Renderer<MappingForListView> entitiesMappingsDataGridEntityName1CRenderer() {
        return new ComponentRenderer<>(
                () -> uiComponents.create(Span.class),
                (span, mapping) -> {
                    if (mapping.getEntityName1C() != null) {
                        span.setText(mapping.getEntityName1C());
                    } else {
                        span.setText("");
                    }
                }
        );
    }

    @Supply(to = "entitiesMappingsDataGrid.attributesMapped", subject = "renderer")
    private Renderer<MappingForListView> entitiesMappingsDataGridAttributesMappedRenderer() {
        return new ComponentRenderer<>(
                () -> {
                    JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
                    checkbox.setReadOnly(true);
                    return checkbox;
                },
                (checkbox, mapping) -> checkbox.setValue(mapping.getAttributesMapped())
        );
    }

    @Supply(to = "entitiesMappingsDataGrid.scriptDefined", subject = "renderer")
    private Renderer<MappingForListView> entitiesMappingsDataGridScriptDefinedRenderer() {
        return new ComponentRenderer<>(
                () -> {
                    JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
                    checkbox.setReadOnly(true);
                    return checkbox;
                },
                (checkbox, mapping) -> checkbox.setValue(mapping.getScriptDefined())
        );
    }

    private JmixButton createDataGridButton(MappingForListView entity, String text) {
        JmixButton button = uiComponents.create(JmixButton.class);
        button.setText(text);
        button.setIcon(VaadinIcon.EYE.create());
        button.getElement().getThemeList().add("secondary");
        button.getElement().getThemeList().add("small");
        button.addClickListener(e -> {
                    if ("1C entity".equals(text)) {
                        openEditDialog(entity, EntityMappingView.DialogType.ENTITY);
                    } else if ("script".equals(text)) {
                        openEditDialog(entity, EntityMappingView.DialogType.SCRIPT);
                    } else {
                        openEditDialog(entity, null);
                    }
                }
        );
        return button;
    }

    @Supply(to = "entitiesMappingsDataGrid.entity1C", subject = "renderer")
    private Renderer<MappingForListView> entitiesMappingsDataGridEntity1CRenderer() {
        return new ComponentRenderer<>(entity -> createDataGridButton(entity, "1C entity"));
    }

    @Supply(to = "entitiesMappingsDataGrid.attributes", subject = "renderer")
    private Renderer<MappingForListView> entitiesMappingsDataGridAttributesRenderer() {
        return new ComponentRenderer<>(entity -> createDataGridButton(entity, "attributes"));
    }

    @Supply(to = "entitiesMappingsDataGrid.script", subject = "renderer")
    private Renderer<MappingForListView> entitiesMappingsDataGridScriptRenderer() {
        return new ComponentRenderer<>(entity -> createDataGridButton(entity, "script"));
    }

    protected void openEditDialog(MappingForListView entity, EntityMappingView.DialogType dialogType) {
        if (dialogType == null) {
            if (entity.getEntityName1C() != null && !entity.getEntityName1C().isEmpty()) {
                DialogWindow<AttributesMappingView> dialog =
                        dialogWindows.view(this, AttributesMappingView.class).build();
                AttributesMappingView attributesMappingView = dialog.getView();
                attributesMappingView.setLocalEntityName(entity.getEntityNameJmix());
                dialog.setMaxWidth("55%");
                dialog.addAfterCloseListener(e -> {
                    entitiesMappingsDc.replaceItem(entity);
                    entitiesMappingsDl.load();
                });
                dialog.open();
            }
        } else {
            DialogWindow<EntityMappingView> dialog =
                    dialogWindows.view(this, EntityMappingView.class).build();
            EntityMappingView entityMappingView = dialog.getView();
            entityMappingView.setLocalEntityName(entity.getEntityNameJmix());
            entityMappingView.setDialogType(dialogType);
            dialog.addAfterCloseListener(e -> {
                entitiesMappingsDc.replaceItem(entity);
                entitiesMappingsDl.load();
            });
            dialog.open();
        }
    }

    @Supply(to = "entitiesMappingsDataGrid.upload", subject = "renderer")
    private Renderer<MappingForListView> entitiesMappingsDataGridUploadRenderer() {
        return new ComponentRenderer<>(
                (mapping) -> {
                    JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
                    checkbox.setValue(mapping.getUpload());
                    checkbox.addValueChangeListener(event -> {
                        mapping.setUpload(event.getValue());
                        mappingService.setUpload(mapping.getEntityNameJmix(), event.getValue());
                        entitiesMappingsDc.replaceItem(mapping);
                    });
                    return checkbox;
                },
                (checkbox, mapping) -> {
                    JmixCheckbox jmixCheckbox = (JmixCheckbox) checkbox;
                    jmixCheckbox.setValue(mapping.getUpload());
                    return jmixCheckbox;
                }
        );
    }
}