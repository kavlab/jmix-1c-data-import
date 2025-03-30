package ru.kavlab.autoconfigure.dataimportaddon;

import ru.kavlab.dataimportaddon.DiaConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({DiaConfiguration.class})
public class DiaAutoConfiguration {
}

