/* $Id: CleanupTranslationFiles.java,v 1.3 2006/04/14 14:58:04 totmacherr Exp $
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.util;

import java.io.*;
import java.util.*;

/**
 * A Translation file cleaner
 * 
 * @version $Revision: 1.3 $
 */
public class CleanupTranslationFiles {
    /** A table of hex digits */
    private static final char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6',
        '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final String headerText = "#\n# PowerFolder translation file\n" +
            "#\n" +
            "# Guide:\n"
        + "# Missing translations are marked with ## in front of the line\n" +
                "# You just need to translate the english text behind the =\n" +
                "# Then remove the ## in front of the line.\n" +
                "#\n" +
                "# There is also a guide on our offical webpage under\n" +
                "# http://www.powerfolder.com/?q=node/i18n\n" +
                "#\n" +
                "# When you are completed please send your translation file to\n" +
                "# contact@powerfolder.com\n" +
                "#\n" +
                "# Thank you,\n" +
                "# Your PowerFolder Team\n" +
                "# http://www.powerfolder.com\n" +
                "#";

    private static final String baseName = "etc/Translation";
    private static final String outputName = "build/Translation";
    
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
            Properties de = loadTranslationFile(baseName + "_"
                + locale.getLanguage() + ".properties");
            writeTranslationFile(locale.getLanguage(), keys, de);
        }

        System.out.println("Streamlined " + originals.size()
            + " translations. " + (supportedLocales.length - 1)
            + " Translation files");
    }

    /**
     * @param originals
     * @param keys
     * @throws IOException 
     */
    private void writeTranslationFile(String localeName, List<String> keys,
        Properties translations) throws IOException
    {
        if (localeName == null) {
            localeName = "";
        } else {
            localeName = "_" + localeName;
        }
        // Now write the stuff
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(outputName + localeName
                + ".properties");
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
        
        for (String key : keys) {
            String val = translations.getProperty(key);
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
                if (!translationFound) {
                    out.write("##");
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
    }

    private Properties loadTranslationFile(String fileName) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(fileName));
        } catch (IOException e) {
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
