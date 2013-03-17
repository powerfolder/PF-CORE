/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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
 * $Id: WinUtils.java 20555 2012-12-25 04:15:08Z glasgow $
 */
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.clientserver.ServerClient;

public class WebDAV {

    private WebDAV() {
    }

    public static String createConnection(ServerClient serverClient,
        String webDAVURL)
    {
        try {
            String cmd = "net use * \"" + webDAVURL + "\" /User:"
                + serverClient.getUsername() + " \""
                + serverClient.getPasswordClearText() + "\" /persistent:yes";
            Process process = Runtime.getRuntime().exec(cmd);
            byte[] out = StreamUtils
                .readIntoByteArray(process.getInputStream());
            String output = new String(out);
            byte[] err = StreamUtils
                .readIntoByteArray(process.getErrorStream());
            String error = new String(err);
            if (StringUtils.isEmpty(error)) {
                if (!StringUtils.isEmpty(output)) {
                    // Looks like the link succeeded :-)
                    return 'Y' + output;
                }
            } else {
                // Looks like the link failed :-(
                return 'N' + error;
            }
        } catch (Exception e) {
            // Looks like the link failed, badly :-(
            return 'N' + e.getMessage();
        }
        // Huh?
        return null;
    }

}
