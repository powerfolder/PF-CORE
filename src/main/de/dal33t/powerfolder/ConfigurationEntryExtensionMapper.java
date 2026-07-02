/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 */
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public final class ConfigurationEntryExtensionMapper {

    public final HashMap<String, Field> fieldMapping;

    public ConfigurationEntryExtensionMapper(Class<?> clazz, boolean mapOldNames) {
        fieldMapping = new HashMap<>();

        map(clazz, mapOldNames);
    }

    public ConfigurationEntryExtensionMapper(Class<?> clazz) {
        fieldMapping = new HashMap<>();

        map(clazz, true);
    }

    private void map(Class<?> clazz, boolean mapOldNames) {
        for (Field field : clazz
                .getDeclaredFields()) {
            ConfigurationEntryExtension extension = field
                    .getAnnotation(ConfigurationEntryExtension.class);
            if (extension != null) {
                fieldMapping.put(extension.name(), field);

                if (mapOldNames &&
                        StringUtils.isNotBlank(extension.oldName())) {
                    fieldMapping.put(extension.oldName(), field);
                }
            }
        }

    }
}
