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
 * $Id: CleanupTranslationFiles.java 20511 2012-12-12 23:19:38Z sprajc $
 */
package de.dal33t.powerfolder.util;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
    private static final String[] RETRANSLATE = {};

    private static final String headerText = "#\n# PowerFolder translation file\n"
        + "#\n"
        + "# Guide:\n"
        + "# Missing translations are marked with ## in front of the line\n"
        + "# You just need to translate the english text behind the =\n"
        + "# Then remove the ## in front of the line.\n"
        + "#\n"
        + "# Original English texts for existing translations are\n"
        + "# indicated with a proceeding #orig#.\n"
        + "# Please check if the meaning is still the same.\n"
        + "#\n"
        + "# To translate the texts in the right encoding please use\n"
        + "# the Properties Editor from this URL:\n"
        + "# http://propedit.sourceforge.jp/index_en.html\n"
        + "#\n"
        + "# GUIDE: Launch the program twice, one with the english file opened and the other\n"
        + "# with the file in your language. Enable \"show line number\" in the View menu\n"
        + "# and lineup the 2 programs next to each other.\n"
        + "# Select UTF-8 radiobutton when opening a\n"
        + "# file with Properties editor\n"
        + "#\n"
        + "# When you are completed please send your translation file to\n"
        + "# development@powerfolder.com\n"
        + "#\n"
        + "# Thank you,\n"
        + "# Your PowerFolder Team\n" + "# http://www.powerfolder.com\n" + "#";

    private static final String baseName = "src/etc/Translation";
    private static final String outputName = "src/etc/Translation";

    private Properties originals;
    private boolean deep = false;

    public void run() throws IOException {
        originals = loadTranslationFile(baseName + ".properties");
        List<String> keys = new ArrayList<String>();
        List<String> searchContents = new ArrayList<String>();
        for (Object string : originals.keySet()) {
            String key = (String) string;
            keys.add(key);
            if (key.startsWith("action_")) {
                int i = key.lastIndexOf('.');
                if (i > 0) {
                    key = key.substring(0, i);
                }
            }
           if (!key.startsWith("transfer_mode.")) {
               searchContents.add(key);
           }
        }
        Collections.sort(keys);
        if (deep) {
            Collection<String> usedOriginals = new HashSet<String>();
            findContent(searchContents, Paths.get("src/main"), usedOriginals);
            findContent(searchContents, Paths.get("../PowerFolder-Pro/src/pro"),
                usedOriginals);
            findContent(searchContents, Paths.get(
                "../PowerFolder-Pro/src/server"), usedOriginals);
            System.out.println("Found " + usedOriginals.size() + "/"
                + keys.size() + ". " + (keys.size() - usedOriginals.size())
                + " unused translations. Removing: ");

            Collection<String> unused = new HashSet<String>(searchContents);
            unused.removeAll(usedOriginals);

            for (String key : unused) {
                System.out.println(key);
                if (!keys.remove(key)) {
                    boolean r = keys.remove(key + ".key")
                        || keys.remove(key + ".label")
                        || keys.remove(key + ".description");
                    if (!r) {
                        System.err.println("Unable to remove " + key);
                    }
                }
            }
        }

        // writeTranslationFile(null, keys, originals);

        List<Locale> supportedLocales = Translation.getSupportedLocales();
        for (Locale locale : supportedLocales) {
            if (locale.getLanguage().equals("en")) {
                // Skip en
                continue;
            }
            Properties foreignProperties = loadTranslationFile(baseName + "_"
                + locale.getLanguage() + ".properties");
            writeTranslationFile(locale.getLanguage(), keys, foreignProperties);
        }

        writeTranslationFile(null, keys, originals);

        System.out.println("Streamlined " + originals.size()
            + " translations. " + (supportedLocales.size() - 1)
            + " Translation files");
    }

    private void writeTranslationFile(String theLocaleName, List<String> keys,
        Properties translations) throws IOException
    {
        boolean original;
        String localeName = theLocaleName;
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

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                new BufferedOutputStream(fOut), "8859_1"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } finally {
        }

        String lastPrefix = null;
        out.write(headerText);
        out.newLine();
        int missing = 0;

        for (String key : keys) {
            String val = translations.getProperty(key);
            String originalVal = originals.getProperty(key);
            if (Arrays.asList(RETRANSLATE).contains(key)) {
                val = null;
            }
            boolean translationFound = val != null;
            if (val == null) {
                val = originalVal;
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
//                if (translationFound) {
//                    out.write("#orig#");
//                    out.write(saveConvert(key, true) + "="
//                        + saveConvert(originalVal, false));
//                    out.newLine();
//                }
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

    private void findContent(Collection<String> contents, Path f,
        Collection<String> foundContents) throws FileNotFoundException
    {
        if (Files.isDirectory(f)) {
            Filter<Path> filter = new Filter<Path>() {
                @Override
                public boolean accept(Path entry) {
                    return Files.isDirectory(entry)
                        || entry.getFileName().toString().endsWith("java");
                }
            };

            try (DirectoryStream<Path> innerFiles = Files.newDirectoryStream(f, filter)) {
                for (Path innerFile : innerFiles) {
                    findContent(contents, innerFile, foundContents);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            try (InputStream in = Files.newInputStream(f);) {
                byte[] buf = StreamUtils.readIntoByteArray(in);
                String input = new String(buf, Convert.UTF8);
                for (String content : contents) {
                    if (input.contains(content)) {
                        foundContents.add(content);
                    }
                }
                System.out.println(f.toAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Properties loadTranslationFile(String fileName) {
        ExtendedProperties props = new ExtendedProperties();
        try {
            props.load(new FileInputStream(fileName));
        } catch (FileNotFoundException e) {
            System.err.print(fileName);
            try {
                System.err.println(": Creating new translation file");
                Files.createFile(Paths.get(fileName));
                return props;
            } catch (IOException e1) {
                e1.printStackTrace();
            }
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
