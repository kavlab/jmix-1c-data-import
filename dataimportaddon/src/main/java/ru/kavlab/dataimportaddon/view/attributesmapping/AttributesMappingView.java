package ru.kavlab.dataimportaddon.view.attributesmapping;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.action.BaseAction;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.kavlab.dataimportaddon.app.data.MappingEntity;
import ru.kavlab.dataimportaddon.app.data.PropertyFillType;
import ru.kavlab.dataimportaddon.app.service.mapping.MappingService;
import ru.kavlab.dataimportaddon.app.service.odata.ODataImportService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Route(value = "imp1c-attributes-mapping-view")
@ViewController("imp1c_AttributesMappingView")
@ViewDescriptor("imp1c-attributes-mapping-view.xml")
public class AttributesMappingView extends StandardView {

    protected String localEntityName;
    @Autowired
    private MappingService mappingService;
    @Autowired
    private ODataImportService dataImportService;
    @Autowired
    private UiComponents uiComponents;
    @ViewComponent
    private VerticalLayout vBox;
    @ViewComponent
    private BaseAction ok;
    @ViewComponent
    private H3 viewTitle;
    @Autowired
    private Messages messages;

    public void setLocalEntityName(String localEntityName) {
        this.localEntityName = localEntityName;
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        ok.withHandler(e -> closeWithDefaultAction());
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        if (localEntityName != null) {
            mappingService.getMappingEntityByLocalName(localEntityName)
                    .ifPresent(me -> {
                                viewTitle.setText(me.getEntityName1C() + " -> " + me.getEntityNameJmix());
                                vBox.addComponentAsFirst(createForm(me));
                            }
                    );
        }
    }

    private FormLayout createForm(MappingEntity mappingEntity) {
        FormLayout formLayout = uiComponents.create(FormLayout.class);
        formLayout.setId("form");
        formLayout.setWidth("100%");
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("100%", 1));

        var localProperties = mappingService.getLocalMetadata().get(mappingEntity.getEntityNameJmix());

        var externalProperties = dataImportService
                .getExternalPropertyNamesByEntity(mappingEntity.getEntityName1C());

        List<PropertyFillType> fullTypeList = List.of(
                PropertyFillType.CONSTANT,
                PropertyFillType.ATTRIBUTE);

        List<PropertyFillType> shortTypeList = List.of(
                PropertyFillType.ATTRIBUTE);

        AtomicBoolean firstRow = new AtomicBoolean(true);

        localProperties.forEach((propertyName, propertyInfo) -> {

            boolean showLabels = firstRow.getAndSet(false);

            HorizontalLayout hBox = uiComponents.create(HorizontalLayout.class);
            hBox.setId(propertyName + "HBox");
            hBox.setWidth("100%");
            hBox.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

            TextField tfJmixAttr = uiComponents.create(TextField.class);
            tfJmixAttr.setValue(propertyName);
            if (showLabels) {
                tfJmixAttr.setLabel(messages.getMessage(
                        "ru.kavlab.dataimportaddon.view.attributesmapping/field-jmix-attribute"));
            }
            tfJmixAttr.setReadOnly(true);

            @SuppressWarnings("unchecked")
            ComboBox<PropertyFillType> cbType = (ComboBox<PropertyFillType>) uiComponents.create(ComboBox.class);
            cbType.setClearButtonVisible(true);
            cbType.setId(propertyName + "TypeField");
            if (showLabels) {
                cbType.setLabel(messages.getMessage(
                        "ru.kavlab.dataimportaddon.view.attributesmapping/field-type"));
            }
            if (propertyInfo.getJavaType().equals(String.class)) {
                cbType.setItems(fullTypeList);
            } else {
                cbType.setItems(shortTypeList);
            }
            mappingService.getSelectedValue(mappingEntity.getEntityNameJmix(),
                            propertyName, MappingService.MappingField.TYPE)
                    .ifPresent(v -> cbType.setValue(PropertyFillType.fromId(v)));

            TextField tfConst = uiComponents.create(TextField.class);
            tfConst.setClearButtonVisible(true);
            tfConst.setId(propertyName + "ValueField");
            if (showLabels) {
                tfConst.setLabel(messages.getMessage(
                        "ru.kavlab.dataimportaddon.view.attributesmapping/field-value"));
            }
            if (cbType.getValue() == null) {
                tfConst.setVisible(false);
            } else {
                tfConst.setVisible(cbType.getValue().equals(PropertyFillType.CONSTANT));
            }
            mappingService.getSelectedValue(mappingEntity.getEntityNameJmix(),
                            propertyName, MappingService.MappingField.VALUE)
                    .ifPresent(tfConst::setValue);

            @SuppressWarnings("unchecked")
            ComboBox<String> cb1CAttr = (ComboBox<String>) uiComponents.create(ComboBox.class);
            cb1CAttr.setClearButtonVisible(true);
            cb1CAttr.setId(propertyName + "AttrField");
            if (showLabels) {
                cb1CAttr.setLabel(messages.getMessage(
                        "ru.kavlab.dataimportaddon.view.attributesmapping/field-1c-attr"));
            }
            cb1CAttr.setMinWidth("300px");
            cb1CAttr.setItems(externalProperties);
            if (cbType.getValue() == null) {
                cb1CAttr.setVisible(false);
            } else {
                cb1CAttr.setVisible(cbType.getValue().equals(PropertyFillType.ATTRIBUTE));
            }
            mappingService.getSelectedValue(mappingEntity.getEntityNameJmix(),
                            propertyName, MappingService.MappingField.ATTR)
                    .ifPresent(cb1CAttr::setValue);

            cbType.addValueChangeListener(e ->
                    hBox.getChildren().forEach(c -> c.getId().ifPresent(id -> {
                        if (e.getValue() == null) {
                            if (id.equals(propertyName + "ValueField") || id.equals(propertyName + "AttrField")) {
                                c.setVisible(false);
                            }
                        } else {
                            if (id.equals(propertyName + "ValueField")) {
                                c.setVisible(e.getValue().equals(PropertyFillType.CONSTANT));
                            } else if (id.equals(propertyName + "AttrField")) {
                                c.setVisible(e.getValue().equals(PropertyFillType.ATTRIBUTE));
                            }
                        }
                    })));

            hBox.add(tfJmixAttr);
            hBox.add(cbType);
            hBox.add(tfConst);
            hBox.add(cb1CAttr);

            formLayout.add(hBox);
        });

        return formLayout;
    }

    @Subscribe("ok")
    public void onOk(final ActionPerformedEvent event) {
        if (localEntityName != null) {
            findFormComponent(vBox).ifPresent(this::processFormComponent);
        }
    }

    private Optional<Component> findFormComponent(VerticalLayout container) {
        return container.getChildren()
                .filter(component -> component.getId().isPresent()
                        && component.getId().get().equals("form"))
                .findFirst();
    }

    private void processFormComponent(Component formComponent) {
        formComponent.getChildren()
                .filter(component -> component instanceof HorizontalLayout
                        && component.getId().isPresent()
                        && component.getId().get().endsWith("HBox"))
                .forEach(component -> processHBox((HorizontalLayout) component));
    }

    private void processHBox(HorizontalLayout hBox) {
        if (hBox.getId().isEmpty()) return;

        String hBoxId = hBox.getId().get();
        String property = hBoxId.replace("HBox", "");
        Map<String, String> cache = new HashMap<>();

        // Process all child elements that are fields
        hBox.getChildren()
                .filter(field -> field.getId().isPresent() && field.getId().get().endsWith("Field"))
                .forEach(field -> processField(field, property, cache));

        // Convert type value if it is set
        String typeId = cache.get("Type");
        PropertyFillType propertyFillType = (typeId == null ? null : PropertyFillType.fromId(typeId));

        if (propertyFillType != null) {
            mappingService.setPropertyValue(
                    localEntityName,
                    property,
                    propertyFillType,
                    cache.get("Value"),
                    cache.get("Attr")
            );
        }
    }

    private void processField(Component field, String property, Map<String, String> cache) {
        field.getId().ifPresent(fieldId -> {
            String fieldName = fieldId.replace("Field", "");
            String fieldType = fieldName.replaceFirst(property, "");

            if (field instanceof ComboBox<?> comboBox) {
                if (fieldName.endsWith("Type")) {
                    @SuppressWarnings("unchecked")
                    ComboBox<PropertyFillType> typeComboBox = (ComboBox<PropertyFillType>) comboBox;
                    PropertyFillType value = typeComboBox.getValue();
                    cache.put(fieldType, value != null ? value.getId() : null);
                } else {
                    @SuppressWarnings("unchecked")
                    ComboBox<String> valueComboBox = (ComboBox<String>) comboBox;
                    String value = valueComboBox.getValue();
                    cache.put(fieldType, value);
                }
            } else if (field instanceof TextField textField) {
                String textValue = textField.getValue();
                cache.put(fieldType, textValue);
            }
        });
    }
}