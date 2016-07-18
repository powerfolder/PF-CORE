/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
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

    /** List of all supported locales */
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
            supportedLocales = new ArrayList<Locale>();
            supportedLocales.add(Locale.ENGLISH);
            supportedLocales.add(Locale.GERMAN);
            supportedLocales.add(POLISH);
            supportedLocales.add(PORTUGUESE);
            supportedLocales.add(SPANISH);
            supportedLocales.add(Locale.ITALIAN);
            // supportedLocales.add(RUSSIAN);
            // supportedLocales.add(Locale.FRENCH);
            // supportedLocales.add(Locale.CHINESE);
            // supportedLocales.add(DUTCH);
            // supportedLocales.add(Locale.JAPANESE);
            // supportedLocales.add(SWEDISH);
            // supportedLocales.add(ARABIC);
            // supportedLocales.add(HUNGARIAN);
            // supportedLocales.add(TURKISH);
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
     * @param locale
     *            the locale, or null to reset
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

    /**
     * @return the currently active resource bundle
     */
    public static synchronized ResourceBundle getResourceBundle() {
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
                if (confLangStr != null) {
                    if (confLangStr.equals("en_GB")) {
                        confLang = Locale.UK;
                    } else if (confLangStr.equals("en")) {
                        // Normal (USA) English
                        confLang = new Locale("");
                    }
                }
                resourceBundle = ResourceBundle.getBundle("Translation",
                    confLang);

                log.info("Default Locale '" + Locale.getDefault()
                    + "', using '" + resourceBundle.getLocale()
                    + "', in config '" + confLang + '\'');
            } catch (MissingResourceException e) {
                log.log(Level.SEVERE, "Unable to load translation file", e);
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
                    .entrySet())
                {
                    if (translation.contains(placeHolderEntry.getKey())) {
                        translation = translation.replace(
                            placeHolderEntry.getKey(),
                            placeHolderEntry.getValue());
                    }
                }
            }
            return translation;
        } catch (MissingResourceException e) {
            if (id != null && !id.startsWith("date_format.")) {
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
     * @param params
     *            the parameters to be included.
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