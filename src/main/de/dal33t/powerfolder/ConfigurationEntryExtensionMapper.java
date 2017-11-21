package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public final class ConfigurationEntryExtensionMapper {

    public final HashMap<String, Field> fieldMapping;

    public ConfigurationEntryExtensionMapper(Class<?> clazz) {
        fieldMapping = new HashMap<>();

        for (Field field : clazz
            .getDeclaredFields())
        {
            ConfigurationEntryExtension extension = field
                .getAnnotation(ConfigurationEntryExtension.class);
            if (extension != null) {
                fieldMapping.put(extension.name(), field);
                if (StringUtils.isNotBlank(extension.oldName())) {
                    fieldMapping.put(extension.oldName(), field);
                }
            }
        }
    }
}
