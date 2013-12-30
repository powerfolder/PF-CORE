package de.dal33t.powerfolder.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class SplitConfig extends Properties {
    private static final long serialVersionUID = 1L;

    private Properties regular = new Properties();
    private Properties folders = new Properties();

    public Properties getRegular() {
        return regular;
    }

    public Properties getFolders() {
        return folders;
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
        if (String.valueOf(key).startsWith("f.")) {
            return folders.put(key, value);
        } else {
            return regular.put(key, value);
        }
    }

    @Override
    public synchronized Object remove(Object key) {
        Object value = regular.remove(key);
        if (value != null) {
            return value;
        }
        return folders.remove(key);
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
