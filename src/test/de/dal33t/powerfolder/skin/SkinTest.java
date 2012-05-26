package de.dal33t.powerfolder.skin;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import de.dal33t.powerfolder.ui.util.Icons;

import junit.framework.TestCase;

public class SkinTest extends TestCase {
    private Set<String> availableIconKeys = new HashSet<String>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Field[] fields = Icons.class.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (!field.getType().equals(String.class)) {
                continue;
            }
            String v = (String) field.get(null);
            availableIconKeys.add(v);
        }
    }

    public void testLoadSkin() {
        ServiceLoader<Skin> skinLoader = ServiceLoader.load(Skin.class);
        List<Skin> skins = new ArrayList<Skin>();
        for (Iterator<Skin> it = skinLoader.iterator(); it.hasNext();) {
            skins.add(it.next());
        }
        assertEquals(2, skins.size());
    }

    public void testIcons() {
        ServiceLoader<Skin> skinLoader = ServiceLoader.load(Skin.class);
        for (Iterator<Skin> it = skinLoader.iterator(); it.hasNext();) {
            Skin skin = it.next();
            Properties p = skin.getIconsProperties();
            if (p == null) {
                // Uses default.
                continue;
            }
            boolean skinNameShown = false;
            for (Entry<Object, Object> entry : p.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                InputStream in = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(value);
                if (in == null) {
                    if (!skinNameShown) {
                        System.err.println("\nProblems in skin: "
                            + skin.getName());
                        skinNameShown = true;
                    }

                    if (!availableIconKeys.contains(key)
                        && !key.startsWith("action"))
                    {
                        fail("Skin: " + skin.getName() + ": Not longer used: "
                            + key + "=" + value);
                        System.err.println("Not longer used: " + key + "="
                            + value);
                    } else {
                        fail("Skin: " + skin.getName() + ": NOT FOUND: " + key
                            + "=" + value);
                        System.err.println("NOT FOUND: " + key + "=" + value);
                    }
                }
            }
        }
    }
}
