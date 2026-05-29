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
 *
 */
package de.dal33t.powerfolder.util;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Basic class which provides accessor to tranlation files
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public class Translation {

    private static final Logger log = Logger.getLogger(Translation.class
            .getName());

    // Useful locales, which are not already included in Locale
    public static final Locale DUTCH = new Locale("nl");
    public static final Locale SPANISH = new Locale("es");
    public static final Locale RUSSIAN = new Locale("ru");
    public static final Locale SWEDISH = new Locale("sv");
    public static final Locale ARABIC = new Locale("ar");
    public static final Locale POLISH = new Locale("pl");
    public static final Locale PORTUGUESE = new Locale("pt");
    public static final Locale HUNGARIAN = new Locale("hu");
    public static final Locale TURKISH = new Locale("tr");
    public static final Locale BULGARIAN = new Locale("bg");
    public static final Locale CZECH = new Locale("cs");
    public static final Locale DANISH = new Locale("da");
    public static final Locale FINNISH = new Locale("fi");
    public static final Locale GREEK = new Locale("el");
    public static final Locale NORWEGIAN = new Locale("no");
    public static final Locale ROMANIAN = new Locale("ro");
    public static final Locale SLOVAK = new Locale("sk");
    public static final Locale CROATIAN = new Locale("hr");
    public static final Locale SLOVENIAN = new Locale("sl");
    public static final Locale SERBIAN = new Locale("sr");
    public static final Locale LITHUANIAN = new Locale("lt");
    public static final Locale LATVIAN = new Locale("lv");
    public static final Locale ESTONIAN = new Locale("et");
    public static final Locale JAPANESE = new Locale("ja");
    public static final Locale KOREAN = new Locale("ko");
    public static final Locale CHINESE_SIMPLIFIED = new Locale("zh", "CN");
    public static final Locale CHINESE_TRADITIONAL = new Locale("zh", "TW");
    public static final Locale UKRAINIAN = new Locale("uk");
    public static final Locale HINDI = new Locale("hi");
    public static final Locale VIETNAMESE = new Locale("vi");
    public static final Locale INDONESIAN = new Locale("id");
    public static final Locale THAI = new Locale("th");
    public static final Locale HEBREW = new Locale("he");
    public static final Locale IRISH = new Locale("ga");
    public static final Locale MALTESE = new Locale("mt");
    public static final Locale URDU = new Locale("ur");
    public static final Locale PERSIAN = new Locale("fa");

    /**
     * List of all supported locales
     */
    private static List<Locale> supportedLocales;

    private static Map<String, String> placeHolders = new ConcurrentHashMap<String, String>();

    static {
        setPlaceHolder("APPNAME", "PowerFolder");
        setPlaceHolder("APPDESCRIPTION", "Sync your World");
    }

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
    public static synchronized List<Locale> getSupportedLocales() {
        if (supportedLocales == null) {
            supportedLocales = new ArrayList<>();
            supportedLocales.add(ARABIC);           // العربية
            supportedLocales.add(BULGARIAN);         // Български
            supportedLocales.add(CHINESE_SIMPLIFIED); // 中文(简体)
            supportedLocales.add(CHINESE_TRADITIONAL);// 中文(繁體)
            supportedLocales.add(CROATIAN);          // Hrvatski
            supportedLocales.add(CZECH);             // Čeština
            supportedLocales.add(DANISH);            // Dansk
            supportedLocales.add(DUTCH);             // Nederlands
            supportedLocales.add(Locale.ENGLISH);    // English
            supportedLocales.add(ESTONIAN);          // Eesti
            supportedLocales.add(FINNISH);           // Suomi
            supportedLocales.add(Locale.FRENCH);     // Français
            supportedLocales.add(Locale.GERMAN);     // Deutsch
            supportedLocales.add(GREEK);             // Ελληνικά
            supportedLocales.add(HEBREW);            // עברית
            // UI framework not capable atm:
            // supportedLocales.add(HINDI);           // हिन्दी
            supportedLocales.add(HUNGARIAN);         // Magyar
            supportedLocales.add(INDONESIAN);        // Bahasa Indonesia
            supportedLocales.add(IRISH);             // Gaeilge
            supportedLocales.add(Locale.ITALIAN);    // Italiano
            supportedLocales.add(JAPANESE);          // 日本語
            supportedLocales.add(KOREAN);            // 한국어
            supportedLocales.add(LATVIAN);           // Latviešu
            supportedLocales.add(LITHUANIAN);        // Lietuvių
            supportedLocales.add(MALTESE);           // Malti
            supportedLocales.add(NORWEGIAN);         // Norsk
            supportedLocales.add(POLISH);            // Polski
            supportedLocales.add(PORTUGUESE);        // Português
            supportedLocales.add(ROMANIAN);          // Română
            supportedLocales.add(RUSSIAN);           // Русский
            supportedLocales.add(SERBIAN);           // Српски
            supportedLocales.add(SLOVAK);            // Slovenčina
            supportedLocales.add(SLOVENIAN);         // Slovenščina
            supportedLocales.add(SPANISH);           // Español
            supportedLocales.add(SWEDISH);           // Svenska
            // UI framework not capable atm:
            // supportedLocales.add(THAI);              // ไทย
            supportedLocales.add(TURKISH);           // Türkçe
            supportedLocales.add(UKRAINIAN);         // Українська
            supportedLocales.add(URDU);              // اردو
            supportedLocales.add(VIETNAMESE);        // Tiếng Việt
        }
        Collections.sort(supportedLocales, LocaleComparator.INSTANCE);
        return Collections.unmodifiableList(supportedLocales);
    }

    /**
     * Adds a new locale to the list of supported locales. For adding a new
     * language dynamically.
     *
     * @param locale
     */
    public static synchronized void addSupportedLocales(Locale locale) {
        Reject.ifNull(locale, "Locale");
        if (!getSupportedLocales().contains(locale)) {
            supportedLocales.add(locale);
        } else {
            log.warning("Not adding locale. Already supported: " + locale);
        }
    }

    /**
     * @return the currently active locale of the used resource bundle
     */
    public static Locale getActiveLocale() {
        Locale locale = getResourceBundle().getLocale();
        if (locale == null || StringUtils.isEmpty(locale.getLanguage())) {
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
     * @param locale the locale, or null to reset
     */
    public static void saveLocalSetting(Locale locale) {
        if (locale != null) {
            if (locale.getCountry().length() == 0) {
                Preferences.userNodeForPackage(Translation.class).put("locale",
                        locale.getLanguage());
            } else {
                Preferences.userNodeForPackage(Translation.class).put("locale",
                        locale.getLanguage() + '_' + locale.getCountry());
            }
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

    private static Object INIT_LOCK = new Object();

    /**
     * @return the currently active resource bundle
     */
    public static ResourceBundle getResourceBundle() {
        if (resourceBundle != null) {
            return resourceBundle;
        }
        synchronized (INIT_LOCK) {
            // Intalize bundle
            try {
                // Get language out of preferences
                String confLangStr = Preferences.userNodeForPackage(
                        Translation.class).get("locale", null);
                Locale confLang = null;
                if (confLangStr != null) {
                    String[] parts = confLangStr.split("_");
                    if (parts.length >= 2) {
                        confLang = new Locale(parts[0], parts[1]);
                    } else {
                        confLang = new Locale(confLangStr);
                    }
                }
                // Take default locale if config is empty
                if (confLang == null) {
                    confLang = Locale.getDefault();
                }
                // Workaround for EN
                if (confLangStr != null) {
                    if (confLangStr.equals("en_GB")) {
                        confLang = Locale.UK;
                    } else if (confLangStr.equals("en")) {
                        // Normal (USA) English
                        confLang = new Locale("");
                    }
                }
                resourceBundle = ResourceBundle.getBundle("Translation",
                        confLang, new UTF8Control());

                log.fine("Default Locale '" + Locale.getDefault()
                        + "', using '" + resourceBundle.getLocale()
                        + "', in config '" + confLang + '\'');
            } catch (MissingResourceException e) {
                log.log(Level.SEVERE, "Unable to load translation file", e);
            }
            return resourceBundle;
        }
    }

    public static synchronized String getCurrentLanguage() {

        Locale confLang = null;

        // Intalize bundle
        try {
            // Get language out of preferences
            String confLangStr = Preferences.userNodeForPackage(
                    Translation.class).get("locale", null);
            confLang = confLangStr != null
                    ? new Locale(confLangStr)
                    : null;
            // Take default locale if config is empty
            if (confLang == null) {
                confLang = Locale.getDefault();
            }
        } catch (MissingResourceException e) {
            log.log(Level.SEVERE, "Unable to load translation file", e);
        }

        return confLang.toString();
    }

    /**
     * Returns translation for this id
     *
     * @param id the id for the translation entry
     * @return the localized string
     */
    public static String get(String id) {
        ResourceBundle rb = getResourceBundle();
        if (rb == null) {
            return "- " + id + " -";
        }
        try {
            String translation = rb.getString(id);
            // log.warning("Translation for '" + id + "': " + translation);
            if (translation.contains("{")) {
                for (Entry<String, String> placeHolderEntry : placeHolders
                        .entrySet()) {
                    if (translation.contains(placeHolderEntry.getKey())) {
                        translation = translation.replace(
                                placeHolderEntry.getKey(),
                                placeHolderEntry.getValue());
                    }
                }
            }
            return translation;
        } catch (MissingResourceException e) {
            if (id != null && !id.startsWith("date_format.") && StringUtils.isNotBlank(id)) {
                // Only log non-date format errors.
                // Date format error may occur during logging, prevent
                // stackoverflow error.
                log.warning("Unable to find translation for ID '" + id + '\'');
                log.log(Level.FINER, "MissingResourceException", e);
            }
            return "- " + id + " -";
        }
    }

    /**
     * Returns a paramterized translation for this id.
     * <p>
     * Use <code>{0}</code> <code>{1}</code> etc as placeholders in property
     * files
     *
     * @param id
     * @param params the parameters to be included.
     * @return a paramterized translation for this id.
     */
    public static String get(String id, String... params) {
        String translation = get(id);
        int paramCount = 0;
        for (String param : params) {
            int i;
            String paramSymbol = "{" + paramCount++ + '}';
            while ((i = translation.indexOf(paramSymbol)) >= 0) {
                translation = translation.substring(0, i) + param
                        + translation.substring(i + 3, translation.length());
            }
        }
        return translation;
    }

    public static void setPlaceHolder(String id, String text) {
        if (text == null) {
            placeHolders.remove('{' + id + '}');
        } else {
            placeHolders.put('{' + id + '}', text);
        }
    }

    private static final class LocaleComparator implements Comparator<Locale> {
        private static LocaleComparator INSTANCE = new LocaleComparator();

        public int compare(Locale o1, Locale o2) {
            return o1.getDisplayName(o1).compareTo(o2.getDisplayName(o2));
        }
    }
}
