package de.dal33t.powerfolder.web;

import java.net.URLEncoder;
import java.util.Date;

import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Velocity does not allow for static classes like URLEncoder and Translation.
 * This is solved with this class. 
 */
public class VelocityTools {
    private static VelocityTools instance;

    private VelocityTools() {
    }

    public static VelocityTools getInstance() {
        if (instance == null) {
            instance = new VelocityTools();
        }
        return instance;
    }

    /**
    * Convenience function to access an element of an array.
    *
    * @param index
    * @param array
    * @return Element at the specified array index.
    */
    public static Object getElement(int index, Object[] array)
    {
        return array[index];
    }
    
    public String translate(String key) {
        return Translation.getTranslation(key);
    }

    public String translate(String key, String param1) {
        return Translation.getTranslation(key, param1);
    }

    public String translate(String key, String param1, String param2) {
        return Translation.getTranslation(key, param1, param2);
    }

    public String translate(String key, String param1, String param2,
        String param3)
    {
        return Translation.getTranslation(key, param1, param2, param3);
    }

    public static String URLEncode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }

    public static String formatBytesShort(long bytes) {
        return Format.formatBytesShort(bytes);
    }

    public static String formatDate(Date date) {
        return Format.formatDate(date);

    }
}
