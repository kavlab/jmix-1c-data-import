package ru.kavlab.autoconfigure.dataimportaddon;

import ru.kavlab.dataimportaddon.Imp1cConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({Imp1cConfiguration.class})
public class DiaAutoConfiguration {
}

