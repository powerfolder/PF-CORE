/* $Id: Translation.java,v 1.13 2006/04/15 13:42:18 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

/**
 * Basic class which provides accessor to tranlation files
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public class Translation {
    private static final Logger LOG = Logger.getLogger(Translation.class);

    // Useful locales, which are not already included in Locale
    public static Locale DUTCH = new Locale("nl");
    public static Locale SPANISH = new Locale("es");
    public static Locale RUSSIAN = new Locale("ru");
    public static Locale SWEDISH = new Locale("se");

    /** List of all supported locales */
    private static Locale[] supportedLocales;

    // The resource bundle, initalized lazy
    private static ResourceBundle resourceBundle;

    /**
     * 
     */
    private Translation() {
        super();
    }

    /**
     * @return the supported locales by PowerFolder
     */
    public static Locale[] getSupportedLocales() {
        if (supportedLocales == null) {
            supportedLocales = new Locale[10];
            supportedLocales[0] = Locale.ENGLISH;
            supportedLocales[1] = Locale.GERMAN;
            supportedLocales[2] = DUTCH;
            supportedLocales[3] = Locale.JAPANESE;
            supportedLocales[4] = Locale.ITALIAN;
            supportedLocales[5] = SPANISH;
            supportedLocales[6] = RUSSIAN;
            supportedLocales[7] = Locale.FRENCH;
            supportedLocales[8] = Locale.CHINESE;
            supportedLocales[9] = SWEDISH;
        }
        return supportedLocales;
    }

    /**
     * @return the currently active locale of the used resource bundle
     */
    public static Locale getActiveLocale() {
        Locale locale = getResourceBundle().getLocale();
        if (StringUtils.isEmpty(locale.getLanguage())) {
            // Workaround for english
            return Locale.ENGLISH;
        }
        return getResourceBundle().getLocale();
    }

    /**
     * Saves/Overrides the locale setting. Next time the resource bundle is
     * initalized, it tries to gain bundle with that locale. Otherwise fallback
     * to default locale
     * 
     * @param locale
     *            the locale, or null to reset
     */
    public static void saveLocalSetting(Locale locale) {
        if (locale != null) {
            Preferences.userNodeForPackage(Translation.class).put("locale",
                locale.getLanguage());
        } else {
            Preferences.userNodeForPackage(Translation.class).remove("locale");
        }
    }

    /**
     * Reset the resource bundle. Next call will return a freshly initalized RB
     */
    public static void resetResourceBundle() {
        resourceBundle = null;
    }

    public static void setResourceBundle(ResourceBundle newResourceBundle) {
        resourceBundle = newResourceBundle;
    }

    /**
     * @return the currently active resource bundle
     */
    public static ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            // Intalize bundle
            try {
                // Get language out of preferences
                String confLangStr = Preferences.userNodeForPackage(
                    Translation.class).get("locale", null);
                Locale confLang = confLangStr != null
                    ? new Locale(confLangStr)
                    : null;
                // Take default locale if config is empty
                if (confLang == null) {
                    confLang = Locale.getDefault();
                }
                // Workaround for EN
                if (confLangStr != null && confLangStr.startsWith("en")) {
                    confLang = new Locale("");
                }
                resourceBundle = ResourceBundle.getBundle("Translation",
                    confLang);

                LOG.info("Default Locale '" + Locale.getDefault()
                    + "', using '" + resourceBundle.getLocale()
                    + "', in config '" + confLang + "'");
            } catch (MissingResourceException e) {
                LOG.error("Unable to load translation file", e);
            }
        }
        return resourceBundle;
    }

    /**
     * Returns translation for this id
     * 
     * @param id
     *            the id for the translation entry
     * @return the localized string
     */
    public static String getTranslation(String id) {
        ResourceBundle rb = getResourceBundle();
        if (rb == null) {
            return "- " + id + " -";
        }
        try {
            String translation = rb.getString(id);
            // log().warn("Translation for '" + id + "': " + translation);
            return translation;
        } catch (MissingResourceException e) {
            LOG.warn("Unable to find translation for ID '" + id + "'");
            LOG.error(e);
            return "- " + id + " -";
        }
    }

    /**
     * Returns a paramterized translation for this id.
     * <p>
     * Use <code>{0}</code> as placeholder in property files
     * 
     * @param id
     * @param param1
     *            the parameter to be included.
     * @return a paramterized translation for this id.
     */
    public static String getTranslation(String id, Object param1) {
        String translation = Translation.getTranslation(id);
        int i;
        while ((i = translation.indexOf("{0}")) >= 0) {
            translation = translation.substring(0, i) + param1
                + translation.substring(i + 3, translation.length());
        }
        return translation;
    }

    /**
     * Returns a paramterized translation for this id.
     * <p>
     * Use <code>{0}</code> and <code>{1}</code> as placeholder in property
     * files
     * 
     * @param id
     * @param param1
     *            the parameter to be included.
     * @param param2
     *            the second parameter to be included.
     * @return a paramterized translation for this id.
     */
    public static String getTranslation(String id, Object param1, Object param2)
    {
        String translation = Translation.getTranslation(id, param1);
        int i;
        while ((i = translation.indexOf("{1}")) >= 0) {
            translation = translation.substring(0, i) + param2
                + translation.substring(i + 3, translation.length());
        }
        // log().warn("Translation for '" + id + "': " + translation);
        return translation;
    }

    /**
     * Returns a paramterized translation for this id.
     * <p>
     * Use <code>{0}</code> and <code>{1}</code> as placeholder in property
     * files
     * 
     * @param id
     * @param param1
     *            the parameter to be included.
     * @param param2
     *            the second parameter to be included.
     * @param param3
     *            the third parameter to be included.
     * @return a paramterized translation for this id.
     */
    public static String getTranslation(String id, Object param1,
        Object param2, Object param3)
    {
        String translation = Translation.getTranslation(id, param1, param2);
        int i;
        while ((i = translation.indexOf("{2}")) >= 0) {
            translation = translation.substring(0, i) + param3
                + translation.substring(i + 3, translation.length());
        }
        // log().warn("Translation for '" + id + "': " + translation);
        return translation;
    }
}