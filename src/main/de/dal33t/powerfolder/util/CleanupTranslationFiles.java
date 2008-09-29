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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * A Translation file cleaner
 * 
 * @version $Revision: 1.3 $
 */
public class CleanupTranslationFiles {
    /** A table of hex digits */
    private static final char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6',
        '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Removes the translation of the following keys to force retranslation
     */
    private static final String[] RETRANSLATE = {"quickinfo.webservice.foldersmirrored"};
    // "preferences.dialog.showpreviewpanel.tooltip",
    //
    //        
    // "preferences.dialog.startPanel.myFolders", "quickinfo.myfolders.title",
    // "title.my.folders", "about.dialog.professional_folder_sharing_tool",
    // "create_shortcut.description", "dialog.add_member_to_friend_list.explain",
    // "dialog.add_member_to_friend_list.question",
    // "filelist.status.no_files_available_add_files_and_invite",
    // "folder_create.description", "folder_create.dialog.backup_error.text",
    // "folder_create.dialog.backup_error.title",
    // "folder_create.dialog.save_invitation", "folder_create.dialog.settings",
    // "folder_create.dialog.title", "folder_create.error.already_taken",
    // "folder_create.dirempty.text", "folder_create.error.it_is_base_dir",
    // "folder_create.name_empty.text", "folder_create.name_empty.title",
    // "folder_create.progress.text", "folder_create.success",
    // "folder_exception.dialog.text", "folder_exception.dialog.title",
    // "folder_join.description", "folder_join.dialog.title",
    // "folder_leave.description", "folder_leave.description",
    // "folder_leave.dialog.text", "folder_leave.dialog.title"};

    private static final String headerText = "#\n# PowerFolder translation file\n"
        + "#\n"
        + "# Guide:\n"
        + "# Missing translations are marked with ## in front of the line\n"
        + "# You just need to translate the english text behind the =\n"
        + "# Then remove the ## in front of the line.\n"
        + "#\n"
        + "# Pro version translations start with the prefix 'pro.'\n"
        + "#\n"
        + "# There is also a guide on our offical webpage under\n"
        + "# http://www.powerfolder.com/node/i18n\n"
        + "#\n"
        + "# When you are completed please send your translation file to\n"
        + "# translation@powerfolder.com\n"
        + "#\n"
        + "# Thank you,\n"
        + "# Your PowerFolder Team\n" + "# http://www.powerfolder.com\n" + "#";

    private static final String baseName = "src/etc/Translation";
    private static final String outputName = "src/etc/Translation";

    private Properties originals;

    public void run() throws IOException {
        originals = loadTranslationFile(baseName + ".properties");
        List<String> keys = new ArrayList<String>();
        for (Object string : originals.keySet()) {
            keys.add((String) string);
        }
        Collections.sort(keys);

        writeTranslationFile(null, keys, originals);

        Locale[] supportedLocales = Translation.getSupportedLocales();
        for (Locale locale : supportedLocales) {
            if (locale.getLanguage().equals("en")) {
                // Skip en
                continue;
            }
            Properties foreignProperties = loadTranslationFile(baseName + "_"
                + locale.getLanguage() + ".properties");
            writeTranslationFile(locale.getLanguage(), keys, foreignProperties);
        }

        System.out.println("Streamlined " + originals.size()
            + " translations. " + (supportedLocales.length - 1)
            + " Translation files");
    }

    private void writeTranslationFile(String localeName, List<String> keys,
        Properties translations) throws IOException
    {
        boolean original;
        if (localeName == null) {
            localeName = "";
            original = true;
        } else {
            localeName = "_" + localeName;
            original = false;
        }

        // Now write the stuff
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(outputName + localeName + ".properties");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        BufferedWriter out;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                new BufferedOutputStream(fOut), "8859_1"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        String lastPrefix = null;
        out.write(headerText);
        out.newLine();
        int missing = 0;

        for (String key : keys) {
            String val = translations.getProperty(key);
            if (Arrays.asList(RETRANSLATE).contains(key)) {
                val = null;
            }
            boolean translationFound = val != null;
            if (val == null) {
                val = originals.getProperty(key);
            }

            // Get prefix
            String prefix;
            int prefixEnd = key.indexOf('.');
            if (prefixEnd >= 0) {
                prefix = key.substring(0, prefixEnd);
            } else {
                prefix = key;
                System.err.println("Check translation key: " + key);
            }

            try {
                if (!prefix.equals(lastPrefix)) {
                    out.newLine();
                }
                if (!translationFound && !original) {
                    out.write("##");
                    missing++;
                }
                out.write(saveConvert(key, true) + "="
                    + saveConvert(val, false));
                out.newLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            lastPrefix = prefix;
        }

        try {
            out.flush();
            out.close();
            fOut.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Result for " + localeName + ": " + missing
            + " missing translations.");
    }

    private Properties loadTranslationFile(String fileName) {
        ExtendedProperties props = new ExtendedProperties();
        try {
            props.load(new FileInputStream(fileName));
        } catch (IOException e) {
            System.err.println(fileName);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println(fileName);
            e.printStackTrace();
        }
        return props;
    }

    /**
     * Copied from SUN Properties
     * 
     * @param theString
     * @param escapeSpace
     * @return
     */
    private String saveConvert(String theString, boolean escapeSpace) {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuffer outBuffer = new StringBuffer(bufLen);

        for (int x = 0; x < len; x++) {
            char aChar = theString.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    outBuffer.append('\\');
                    outBuffer.append('\\');
                    continue;
                }
                outBuffer.append(aChar);
                continue;
            }
            switch (aChar) {
                case ' ' :
                    if (x == 0 || escapeSpace)
                        outBuffer.append('\\');
                    outBuffer.append(' ');
                    break;
                case '\t' :
                    outBuffer.append('\\');
                    outBuffer.append('t');
                    break;
                case '\n' :
                    outBuffer.append('\\');
                    outBuffer.append('n');
                    break;
                case '\r' :
                    outBuffer.append('\\');
                    outBuffer.append('r');
                    break;
                case '\f' :
                    outBuffer.append('\\');
                    outBuffer.append('f');
                    break;
                case '=' : // Fall through
                case ':' : // Fall through
                case '#' : // Fall through
                case '!' :
                    outBuffer.append('\\');
                    outBuffer.append(aChar);
                    break;
                default :
                    if ((aChar < 0x0020) || (aChar > 0x007e)) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex((aChar >> 12) & 0xF));
                        outBuffer.append(toHex((aChar >> 8) & 0xF));
                        outBuffer.append(toHex((aChar >> 4) & 0xF));
                        outBuffer.append(toHex(aChar & 0xF));
                    } else {
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    /**
     * Convert a nibble to a hex character
     * 
     * @param nibble
     *            the nibble to convert.
     */
    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        CleanupTranslationFiles instance = new CleanupTranslationFiles();
        instance.run();
    }

}
