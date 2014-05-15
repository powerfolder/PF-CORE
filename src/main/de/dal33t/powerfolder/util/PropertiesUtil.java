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
 * $Id: PropertiesUtil.java 13219 2010-08-02 17:01:22Z tot $
 */
package de.dal33t.powerfolder.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Utility for configuration stuff.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class PropertiesUtil {
    private PropertiesUtil() {
    }

    /**
     * Writes the config to the file. The file contains the configurations in a
     * sorted order.
     *
     * @param file
     * @param config
     * @throws IOException
     */
    public static void saveConfig(Path file, Properties config, String header)
        throws IOException
    {
        BufferedWriter fOut = Files.newBufferedWriter(file, Charset.forName("8859_1"));
        store0(config, fOut, header, true);
        fOut.close();
    }

    private static void store0(Properties props, BufferedWriter bw,
        String comments, boolean escUnicode) throws IOException
    {
        if (comments != null) {
            writeComments(bw, comments);
        }
        bw.write('#' + new Date().toString());
        bw.newLine();
        synchronized (props) {
            List<String> confKeys = new ArrayList<String>();
            for (Object key : props.keySet()) {
                confKeys.add((String) key);
            }
            Collections.sort(confKeys);
            for (String key : confKeys) {
                String val = (String) props.get(key);
                key = saveConvert(key, true, true, escUnicode);
                /*
                 * No need to escape embedded and trailing spaces for value,
                 * hence pass false to flag.
                 */
                val = saveConvert(val, false, false, escUnicode);
                bw.write(key + '=' + val);
                bw.newLine();
            }
        }
        bw.flush();
    }

    /*
     * Converts unicodes to encoded &#92;uxxxx and escapes special characters
     * with a preceding slash
     */
    private static String saveConvert(String theString, boolean escapeSpace,
        boolean escapeSpecials, boolean escapeUnicode)
    {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuilder outBuffer = new StringBuilder(bufLen);

        for (int x = 0; x < len; x++) {
            char aChar = theString.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if (aChar > 61 && aChar < 127) {
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
                    if (x == 0 || escapeSpace) {
                        outBuffer.append('\\');
                    }
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
                    if (escapeSpecials) {
                        outBuffer.append('\\');
                    }
                    outBuffer.append(aChar);
                    break;
                default :
                    if ((aChar < 0x0020 || aChar > 0x007e) & escapeUnicode) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(MathUtil
                            .toHexNibble(aChar >> 12 & 0xF));
                        outBuffer
                            .append(MathUtil.toHexNibble(aChar >> 8 & 0xF));
                        outBuffer
                            .append(MathUtil.toHexNibble(aChar >> 4 & 0xF));
                        outBuffer.append(MathUtil.toHexNibble(aChar & 0xF));
                    } else {
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    private static void writeComments(BufferedWriter bw, String comments)
        throws IOException
    {
        bw.write("#");
        int len = comments.length();
        char[] uu = new char[6];
        uu[0] = '\\';
        uu[1] = 'u';
        int last = 0;
        int current = 0;
        while (current < len) {
            char c = comments.charAt(current);
            if (c > '\u00ff' || c == '\n' || c == '\r') {
                if (last != current) {
                    bw.write(comments.substring(last, current));
                }
                if (c > '\u00ff') {
                    uu[2] = MathUtil.toHexNibble(c >> 12 & 0xf);
                    uu[3] = MathUtil.toHexNibble(c >> 8 & 0xf);
                    uu[4] = MathUtil.toHexNibble(c >> 4 & 0xf);
                    uu[5] = MathUtil.toHexNibble(c & 0xf);
                    bw.write(new String(uu));
                } else {
                    bw.newLine();
                    if (c == '\r' && current != len - 1
                        && comments.charAt(current + 1) == '\n')
                    {
                        current++;
                    }
                    if (current == len - 1
                        || comments.charAt(current + 1) != '#'
                        && comments.charAt(current + 1) != '!')
                    {
                        bw.write("#");
                    }
                }
                last = current + 1;
            }
            current++;
        }
        if (last != current) {
            bw.write(comments.substring(last, current));
        }
        bw.newLine();
    }
}
