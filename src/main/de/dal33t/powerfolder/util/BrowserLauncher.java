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

import de.dal33t.powerfolder.Controller;
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

    /**
     * Opens the browser in background thread. This method does not BLOCK. Can
     * safely be used from UI-EDT Thread.
     * 
     * @param controller
     * @param url
     */
    public static void openURL(Controller controller, final String url) {
        open(controller, new URLProducer() {
            @Override
            public String url() {
                return url;
            }
        });
    }

    /**
     * Opens the browser in background thread. This method does not BLOCK. Can
     * safely be used from UI-EDT Thread.
     * 
     * @param controller
     * @param producer
     */
    public static void open(Controller controller, final URLProducer producer) {
        Reject.ifNull(producer, "producer");
        // PFC-2349 : Don't freeze UI
        if (controller != null && controller.getIOProvider() != null) {
            controller.getIOProvider().startIO(new Runnable() {
                public void run() {
                    try {
                        BrowserLauncher.openURL(producer.url());
                    } catch (IOException e) {
                        log.log(Level.WARNING, "Unable to open web browser. "
                            + e);
                    }
                }
            });
        } else {
            // Fallback
            try {
                openURL(producer.url());
            } catch (IOException e) {
                log.log(Level.WARNING, "Unable to open web browser. " + e);
            }
        }
    }

    /**
     * Opens the given URL in the system browser. Method does BLOCK. Never call
     * directly from User Interface code! Use
     * {@link #open(Controller, URLProducer)} instead
     * 
     * @param url
     * @throws IOException
     * @Deprecated favor {@link #openURL(Controller, String)} or
     *             {@link #open(Controller, URLProducer)}
     */
    public static void openURL(String url) throws IOException {
        if (StringUtils.isBlank(url)) {
            log.fine("Not opening blank url!");
            return;
        }
        if (java6impl(url)) {
            return;
        }
        try {
            if (OSUtil.isMacOS()) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                    new Class[]{String.class});
                openURL.invoke(null, url);
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
        log.fine("Launching " + url);
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
    
    public static interface URLProducer {
        String url();
    }
}