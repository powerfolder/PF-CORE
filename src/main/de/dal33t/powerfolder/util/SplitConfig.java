/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
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
 *
 * $Id$
 */
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.ConfigurationEntryExtension;
import de.dal33t.powerfolder.ConfigurationEntryExtensionMapper;
import de.dal33t.powerfolder.LDAPServerConfigurationEntry;
import de.dal33t.powerfolder.disk.FolderSettings;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * PFC-2444
 * @author Sprajc
 */
public class SplitConfig extends Properties {
    private static final Logger LOGGER = Logger.getLogger(SplitConfig.class.getName());
    private static final long serialVersionUID = 1L;

    private Properties regular = new Properties();
    private Properties folders = new Properties();

    private List<LDAPServerConfigurationEntry> ldapServers = new ArrayList<>();
    private ConfigurationEntryExtensionMapper ldapMapper = new ConfigurationEntryExtensionMapper(
        LDAPServerConfigurationEntry.class);

    public Properties getRegular() {
        return regular;
    }

    public Properties getFolders() {
        return folders;
    }

    /**
     * Get an {@link LDAPServerConfigurationEntry} for an {@code index} or
     * {@code null} if non found
     *
     * @param index
     *     The index of the {@link LDAPServerConfigurationEntry}
     *
     * @return The associated {@link LDAPServerConfigurationEntry}, or {@code
     * null} if not found.
     *
     * @see #getIndexOfLDAPEntry(String)
     */
    public LDAPServerConfigurationEntry getLDAPServer(int index) {
        if (ldapServers.isEmpty()) {
            return null;
        }
        for (LDAPServerConfigurationEntry server : ldapServers) {
            if (server.getIndex() == index) {
                return server;
            }
        }
        return null;
    }

    public List<LDAPServerConfigurationEntry> getLDAPServers() {
        return ldapServers;
    }

    // Overriding

    @Override
    public synchronized boolean contains(Object key) {
        return regular.contains(key) || folders.contains(key);
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return regular.containsKey(key) || folders.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return regular.containsValue(value) || folders.containsValue(value);
    }

    @Override
    public synchronized Enumeration<Object> elements() {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object get(Object key) {
        Object value = regular.get(key);
        if (value == null) {
            value = folders.get(key);
        }
        return value;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String v = getProperty(key);
        if (v == null) {
            return defaultValue;
        }
        return v;
    }

    @Override
    public String getProperty(String key) {
        return (String) get(key);
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        String keyValue = String.valueOf(key);
        if (keyValue.startsWith(FolderSettings.PREFIX_V4)) {
            return folders.put(key, value);
        } else if (keyValue
            .startsWith(LDAPServerConfigurationEntry.LDAP_ENTRY_PREFIX))
        {
            return addLDAPEntry(keyValue, value);
        } else {
            return regular.put(key, value);
        }
    }

    /**
     * Inspect the {@code key} for its index ({@link #getIndexOfLDAPEntry}). If
     * there is already an existing {@link LDAPServerConfigurationEntry} in
     * {@link #ldapServers} for that index, add the information to that object.
     * Otherwise create a new {@link LDAPServerConfigurationEntry} and add it to
     * {@link #ldapServers}.
     *
     * @param key
     *     The key of the LDAP config entry
     * @param value
     *     The value of that LDAP config entry
     *
     * @return The {@link LDAPServerConfigurationEntry} if information was added
     * to it, {@code null} otherwise.
     *
     * @author Maximilian Krickl
     * @since 11.5 SP 5
     */
    Object addLDAPEntry(String key, Object value) {
        int index = getIndexOfLDAPEntry(key);
        if (index == -1) {
            LOGGER.info(
                "Could not read index from ldap configuration key " + key +
                    " = " + value + ". Trying to migrate to new entry.");

            if (key.startsWith("ldap.")) {
                regular.put(key, value);
                index = 0;
                key = key.replace("ldap.", "ldap.0.");
            } else if (key.startsWith("ldap2.")) {
                regular.put(key, value);
                index = 1;
                key = key.replace("ldap2.", "ldap.1.");
            } else if (key.startsWith("ldap3.")) {
                regular.put(key, value);
                index = 2;
                key = key.replace("ldap3.", "ldap.2.");
            } else {
                LOGGER.warning("Could not migrate malformed LDAP config entry " +
                    key);
                return null;
            }
        }

        String extension = getExtensionFromKey(key);
        LDAPServerConfigurationEntry serverConfig = getLDAPServerConfigurationEntryOrCreate(index);

        Field field = ldapMapper.fieldMapping.get(extension);

        if (field != null) {
            try {
                LOGGER.fine(
                    "Set " + field.getName() + " to " + key);
                key = setValueToField(serverConfig, field, key, value);

                regular.put(key, value);

                return serverConfig;
            } catch (IllegalAccessException iae) {
                LOGGER.warning(
                    "Could not access field '" + field.getName() + "' for " +
                        key + ". " + iae);
                return null;
            } catch (NoSuchMethodException nsme) {
                LOGGER.warning(
                    "Could not migrate '" + field.getName() + "' for " + key +
                        ". " + nsme);
                return null;
            } catch (InvocationTargetException ite) {
                LOGGER.warning(
                    "Could not migrate '" + field.getName() + "' for " + key +
                        ". " + ite);
                return null;
            }
        } else {
            LOGGER.warning(
                "Extension " + extension + " of config entry " + key +
                    " unknown");
            return null;
        }
    }

    private String setValueToField(LDAPServerConfigurationEntry serverConfig,
        Field field, String key, Object value)
        throws IllegalAccessException, NoSuchMethodException,
        InvocationTargetException
    {
        field.setAccessible(true);

        // migrate from old type to new if necessary
        ConfigurationEntryExtension cee = field
            .getAnnotation(ConfigurationEntryExtension.class);
        Class<?> oldType = cee.oldType();

        boolean isDefaultValue = oldType == Object.class;
        boolean typesDiffer = field.getType() != oldType;
        boolean matchesOldExtension = StringUtils.isNotBlank(cee.oldName()) &&
            key.endsWith(cee.oldName());

        // If oldType is not the default AND oldType differs from the type of the field
        if (!isDefaultValue && typesDiffer &&
            matchesOldExtension)
        {
            Method migrationMethod = serverConfig.getClass().getMethod(
                cee.migrationMethodName(), oldType);

            if (oldType == Boolean.class) {
                migrationMethod
                    .invoke(serverConfig, Boolean.parseBoolean(value.toString()));
            } else if (oldType == Integer.class) {
                migrationMethod
                    .invoke(serverConfig, Integer.parseInt(value.toString()));
            } else {
                migrationMethod.invoke(serverConfig, value);
            }

            return key;
        }

        if (matchesOldExtension) {
            key = key.replace(cee.oldName(), cee.name());
        }

        // set simple types
        if (field.getType() == Boolean.class) {
            field.set(serverConfig,
                Boolean.parseBoolean(value.toString()));
        } else if (field.getType() == Integer.class) {
            field.set(serverConfig, Integer.parseInt(value.toString()));
        } else {
            field.set(serverConfig, value);
        }

        return key;
    }

    /**
     * Get an existing {@link LDAPServerConfigurationEntry} for the passed {@code index}
     * or create a new one, if not found.
     *
     * @param index
     *     The index of the config entry key.
     *
     * @return An {@link LDAPServerConfigurationEntry} associated with the
     * {@code index}.
     *
     * @see #getIndexOfLDAPEntry(String)
     */
    LDAPServerConfigurationEntry getLDAPServerConfigurationEntryOrCreate(
        int index)
    {
        LDAPServerConfigurationEntry serverConfig = getLDAPServer(index);
        if (serverConfig == null) {
            serverConfig = new LDAPServerConfigurationEntry(index, regular);
            ldapServers.add(serverConfig);
            LOGGER.fine("Created new LDAP Server Configuration at index " + index);
        } else {
            LOGGER.fine("Found existing LDAP Server Configuration at index " + index);
        }
        return serverConfig;
    }

    /**
     * Get the extension from an LDAP config entry key.
     * <br /><br />
     * An LDAP config entry is constructed from a prefix, an index and a name
     * separated by a dot.
     *
     * @param key
     *     The key
     *
     * @return The {@code extension} of the key. If {@code keyAsString} is
     * {@code null} or blank, a blank String ('') is returned.
     *
     * @author Maximilian Krickl
     * @since 11.5 SP 5
     * @see #getIndexOfLDAPEntry(String)
     */
    String getExtensionFromKey(String key) {
        if (StringUtils.isBlank(key)) {
            return "";
        }
        return key.replaceFirst(
            LDAPServerConfigurationEntry.LDAP_ENTRY_PREFIX + "\\.\\d*\\.", "");
    }

    /**
     * LDAP configuration entries are constructed from a prefix, an index and a
     * name separated by a dot, e.g. {@code ldap.3.server.url} or more generic
     * {@code <prefix>.<index>.<id>}.
     *
     * @param key The key containing the index
     *
     * @return An integer greater or equal to zero for the index, or -1 if no
     * index was found.
     *
     * @author Maximilian Krickl
     * @since 11.5 SP 5
     */
    int getIndexOfLDAPEntry(String key) {
        String[] keyComponents = key.split("\\.");
        if (keyComponents.length < 2) {
            return -1;
        }
        try {
            return Integer.parseInt(keyComponents[1]);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    @Override
    public synchronized Object remove(Object key) {
        int index = -1;
        if ((index = getIndexOfLDAPEntry(String.valueOf(key))) > -1) {
            return removeLDAPEntry(index, String.valueOf(key));
        }
        Object value = regular.remove(key);
        if (value != null) {
            return value;
        }
        return folders.remove(key);
    }

    /**
     * Remove an LDAP-related key from the configuration.
     *
     * @param index
     *     The index of the LDAP entry
     * @param key
     *     The key of the LDAP config entry
     *
     * @return The previous entry or {@code null} if non was found.
     *
     * @author Maximilian Krickl
     * @since 11.5 SP 5
     */
    Object removeLDAPEntry(int index, String key) {
        LDAPServerConfigurationEntry serverConfig = null;
        if (ldapServers.isEmpty()) {
            return null;
        }

        serverConfig = getLDAPServer(index);
        if (serverConfig == null) {
            return null;
        }

        String extension = getExtensionFromKey(key);
        Field field = ldapMapper.fieldMapping.get(extension);

        if (field != null) {
            try {
                Object prev = field.get(serverConfig);
                field.setAccessible(true);
                field.set(serverConfig, null);
                return prev;
            } catch (IllegalAccessException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public synchronized int size() {
        return regular.size() + folders.size();
    }

    @Override
    public synchronized void clear() {
        regular.clear();
        folders.clear();
    }

    @Override
    public synchronized boolean isEmpty() {
        return regular.isEmpty() && folders.isEmpty();
    }

    @Override
    public Set<Object> keySet() {
        HashSet<Object> set = new HashSet<>(size());
        set.addAll(regular.keySet());
        set.addAll(folders.keySet());
        return set;
    }

    @Override
    public Collection<Object> values() {
        Collection<Object> col = new HashSet<>();
        col.addAll(regular.values());
        col.addAll(folders.values());
        return col;
    }

    @Override
    public Set<String> stringPropertyNames() {
        Set<String> set = new HashSet<>();
        set.addAll(regular.stringPropertyNames());
        set.addAll(folders.stringPropertyNames());
        return set;
    }

    @Override
    public synchronized void putAll(Map<? extends Object, ? extends Object> map)
    {
        for (java.util.Map.Entry<? extends Object, ? extends Object> set : map
            .entrySet())
        {
            put(set.getKey(), set.getValue());
        }
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        Set<Entry<Object, Object>> set = new HashSet<>();
        set.addAll(regular.entrySet());
        set.addAll(folders.entrySet());
        return set;
    }

    @Override
    @Deprecated()
    public synchronized Enumeration<Object> keys() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated()
    public void list(PrintStream out) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated()
    public void list(PrintWriter out) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated()
    public Enumeration<?> propertyNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated()
    public void store(OutputStream out, String comments) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated()
    public void store(Writer writer, String comments) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated()
    public void storeToXML(OutputStream os, String comment, String encoding)
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated()
    public void storeToXML(OutputStream os, String comment) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((folders == null) ? 0 : folders.hashCode());
        result = prime * result + ((regular == null) ? 0 : regular.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        SplitConfig other = (SplitConfig) obj;
        if (folders == null) {
            if (other.folders != null)
                return false;
        } else if (!folders.equals(other.folders))
            return false;
        if (regular == null) {
            if (other.regular != null)
                return false;
        } else if (!regular.equals(other.regular))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SplitConfig [regular=" + regular + ", folders=" + folders + "]";
    }
}
