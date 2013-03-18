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

import java.awt.Desktop;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Bare Bones Browser Launch
 * <p>
 * Version 1.5
 * <p>
 * December 10, 2005
 * <p>
 * Supports: Mac OS X, GNU/Linux, Unix, Windows XP
 * <p>
 * Example Usage: String url = "http:www.centerkey.com/";
 * BareBonesBrowserLaunch.openURL(url);
 * <p>
 * Public Domain Software -- Free to Use as You Like
 * 
 * @version $Revision: 1.5 $
 */
public class BrowserLauncher {

    private static final Logger log = Logger.getLogger(BrowserLauncher.class
        .getName());

    private static final String errMsg = "Error attempting to launch web browser";

    public static void openURL(String url) throws IOException {
        if (java6impl(url)) {
            return;
        }
        String osName = System.getProperty("os.name");
        try {
            if (OSUtil.isMacOS()) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                    new Class[]{String.class});
                openURL.invoke(null, new Object[]{url});
            } else if (OSUtil.isWindowsSystem()) {
                Runtime.getRuntime().exec(
                    "rundll32 url.dll,FileProtocolHandler " + url);
            } else { // assume Unix or Linux
                String[] browsers = {"firefox", "opera", "konqueror",
                    "epiphany", "mozilla", "netscape"};
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++)
                    if (Runtime.getRuntime()
                        .exec(new String[]{"which", browsers[count]}).waitFor() == 0)
                        browser = browsers[count];
                if (browser == null) {
                    throw new Exception("Could not find web browser");
                }
                Runtime.getRuntime().exec(new String[]{browser, url});
            }
        } catch (Exception e) {
            throw (IOException) new IOException(errMsg).initCause(e);
        }
    }

    private static boolean java6impl(String url) throws IOException {
        try {
            if (Desktop.isDesktopSupported()) {
                log.fine("Using Java6 Desktop.browse()");
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (LinkageError err) {
            log.log(Level.FINER, "LinkageError", err);
        } catch (URISyntaxException e) {
            throw (IOException) new IOException("Error:" + e.toString())
                .initCause(e);
        }
        return false;
    }
}