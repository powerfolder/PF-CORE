package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class ConfigurationEntryExtensionMapper {

    final HashMap<String, Field> fieldMapping;

    public ConfigurationEntryExtensionMapper(LDAPServerConfigurationEntry serverConfigurationEntry) {
        fieldMapping = createFieldMap(serverConfigurationEntry);
    }

    private HashMap<String, Field> createFieldMap(
        LDAPServerConfigurationEntry serverConfigurationEntry)
    {
        HashMap<String, Field> result = new HashMap<>();

        for (Field field : serverConfigurationEntry.getClass()
            .getDeclaredFields())
        {
            ConfigurationEntryExtension extension = field
                .getAnnotation(ConfigurationEntryExtension.class);
            if (extension != null) {
                result.put(extension.name(), field);
                if (StringUtils.isNotBlank(extension.oldName())) {
                    result.put(extension.oldName(), field);
                }
            }
        }

        return result;
    }


}
