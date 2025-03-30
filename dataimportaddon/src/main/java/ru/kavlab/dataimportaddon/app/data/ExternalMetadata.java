package ru.kavlab.dataimportaddon.app.data;

import java.util.List;

public record ExternalMetadata(
        String name,
        List<ExternalProperty> externalProperties
) {
}
