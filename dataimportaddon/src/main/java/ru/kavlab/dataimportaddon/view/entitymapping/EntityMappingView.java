package ru.kavlab.dataimportaddon.view.entitymapping;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.codeeditor.CodeEditor;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.action.BaseAction;
import io.jmix.flowui.kit.component.codeeditor.CodeEditorMode;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.kavlab.dataimportaddon.app.service.mapping.MappingService;
import ru.kavlab.dataimportaddon.app.service.odata.ODataImportService;

import java.util.HashMap;
import java.util.Map;

@Route(value = "entity-mapping-view")
@ViewController(id = "EntityMappingView")
@ViewDescriptor(path = "entity-mapping-view.xml")
public class EntityMappingView extends StandardView {

    private final String EXTERNAL_ENTITY_FIELD = "entityNameField";
    private final String SCRIPT_FIELD = "scriptField";

    private String localEntityName;

    public enum DialogType {
        ENTITY, SCRIPT
    }

    private DialogType dialogType;

    @Autowired
    private UiComponents uiComponents;

    @ViewComponent
    private H3 viewTitle;
    @ViewComponent
    private VerticalLayout vBox;
    @Autowired
    private MappingService mappingService;
    @Autowired
    private ODataImportService dataImportService;

    private Map<String, String> values;

    @ViewComponent
    private BaseAction ok;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        if (localEntityName != null) {
            viewTitle.setText("Mapping for " + localEntityName);
        }

        values = new HashMap<>();

        vBox.add(createForm());
    }

    private FormLayout createForm() {
        FormLayout formLayout = uiComponents.create(FormLayout.class);
        formLayout.setId("form");
        formLayout.setWidth("100%");
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("100%", 1));

        var optMappingEntity = mappingService.getMappingEntityByLocalName(localEntityName);

        @SuppressWarnings("unchecked")
        ComboBox<String> comboBox = (ComboBox<String>) uiComponents.create(ComboBox.class);
        comboBox.setClearButtonVisible(true);
        comboBox.setId(EXTERNAL_ENTITY_FIELD);
        comboBox.setLabel("1C entity");
        comboBox.setMinWidth("300px");
        comboBox.setItems(dataImportService.getExternalMetadataNames());
        comboBox.addValueChangeListener(event ->
                event.getSource().getId().ifPresent(id -> {
                    if (event.getValue() == null) {
                        values.remove(id);
                    } else {
                        values.put(id, event.getValue());
                    }
                })
        );
        optMappingEntity.ifPresent(
                mappingEntity -> {
                    comboBox.setValue(mappingEntity.getEntityName1C());
                    values.put(EXTERNAL_ENTITY_FIELD, mappingEntity.getEntityName1C());
                }
        );
        formLayout.add(comboBox);

        if (dialogType != DialogType.ENTITY) {
            comboBox.setVisible(false);
        }

        CodeEditor codeEditor = uiComponents.create(CodeEditor.class);
        codeEditor.setId(SCRIPT_FIELD);
        codeEditor.setMode(CodeEditorMode.GROOVY);
        codeEditor.setMinWidth("500px");
        codeEditor.getStyle().setWidth("100%");
        codeEditor.addValueChangeListener(event ->
            event.getSource().getId().ifPresent(id -> values.put(id, event.getValue()))
        );
        codeEditor.setHelperText("""
                The script field supports the use of the variables:\s
                `Object newEntity`,\s
                `Map<String, Object> externalProperties`,\s
                `boolean skipEntity`""");
        optMappingEntity.ifPresent(
                mappingEntity -> {
                    codeEditor.setValue(mappingEntity.getScript());
                    values.put(SCRIPT_FIELD, mappingEntity.getScript());
                }
        );
        formLayout.add(codeEditor);

        if (dialogType != DialogType.SCRIPT) {
            codeEditor.setVisible(false);
        }

        return formLayout;
    }

    public void setLocalEntityName(String localEntityName) {
        this.localEntityName = localEntityName;
    }

    public void setDialogType(DialogType dialogType) {
        this.dialogType = dialogType;
    }

    @Subscribe("ok")
    public void onOk(final ActionPerformedEvent event) {
        if (dialogType == DialogType.ENTITY) {
            mappingService.setExternalEntityName(localEntityName, values.get(EXTERNAL_ENTITY_FIELD));
        } else {
            mappingService.setScript(localEntityName, values.get(SCRIPT_FIELD));
        }
        closeWithDefaultAction();
    }
}