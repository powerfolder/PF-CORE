package de.dal33t.powerfolder.web;

import java.util.Date;

import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Velocity does not allow for static classes like URLEncoder and Translation.
 * This is solved with this class. *
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

    /** XML does not allow the char '&' */
    public static String replaceAnd(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("&", "%"
            + Integer.toHexString((byte) '&').toUpperCase());
    }

    public static String formatBytesShort(long bytes) {
        return Format.formatBytesShort(bytes);
    }

    public static String formatDate(Date date) {
        return Format.formatDate(date); 
        
    }
}
