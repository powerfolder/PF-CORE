/*
s * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: WinUtils.java 14924 2011-03-10 20:09:51Z tot $
 */
package de.dal33t.powerfolder.util.os.mac;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.net.HTTPProxySettings;
import de.dal33t.powerfolder.util.logging.Loggable;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Utilities for Mac OS X
 * 
 * @author <A HREF="mailto:bytekeeper@powerfolder.com">Dennis Waldherr</A>
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 * @version $Revision$
 */
public class MacUtils extends Loggable {

    private static MacUtils instance;
    private MacUtils() {
    }

    /**
     * @return true if this platform supports the macutils helpers.
     */
    public static boolean isSupported() {
        return getInstance() != null;
    }

    /**
     * @return the instance or NULL if not supported on this platform.
     */
    public static synchronized MacUtils getInstance() {
        if (!OSUtil.isMacOS()) {
            return null;
        }

        if (instance == null) {
            if (OSUtil.isMacOS()) {
                instance = new MacUtils();
            }
        }
        return instance;
    }

    /**
     * Parse the system proxy configuration.
     */
    public void parseProxyConfig(Controller controller) {
        try {
            Process proxyEthernet = Runtime.getRuntime().exec(
                new String[]{"networksetup", "-getwebproxy", "Ethernet"});

            try (BufferedReader br = new BufferedReader(new InputStreamReader(proxyEthernet.getInputStream()))) {
                String line = br.readLine();
                if (line != null && !line.startsWith("Enabled: Yes")) {
                    throw new UnsupportedOperationException();
                }

                String proxyHost = null;
                int proxyPort = 0;

                while (line != null) {
                    if (line.startsWith("Server: ")) {
                        proxyHost = line.split(" ")[1];
                    } else if (line.startsWith("Port: ")) {
                        proxyPort = Integer.valueOf(line.split(" ")[1])
                            .intValue();
                    }

                    line = br.readLine();
                }

                HTTPProxySettings.saveToConfig(controller, proxyHost,
                    proxyPort, null, null);
            } catch (IOException e) {
                logWarning("Parsing of ethernet webproxy failed. " + e, e);
            } catch (UnsupportedOperationException uoe) {
                logFine("No proxy configuration found.");
            }
        } catch (IOException e) {
            logWarning("Could not parse proxy settings. " + e, e);
        }
    }

    /**
     * Create a 'PowerFolders' link in Links, pointing to the PowerFolder base
     * dir.
     * 
     * @param setup
     * @param controller
     * @throws IOException
     */
    public void setPFPlaces(boolean setup, Controller controller)
        throws IOException
    {
        if (!de.dal33t.powerfolder.jni.osx.Util.loaded) {
            logFine("JNI bindings not loaded");
            return;
        }

        Path baseDir = controller.getFolderRepository()
            .getFoldersBasedir();

        if (setup) {
            logInfo("Setting Favorite item: " + baseDir.toAbsolutePath().toString());
            de.dal33t.powerfolder.jni.osx.Util.addFavorite(baseDir.toAbsolutePath().toString());
        } else {
            de.dal33t.powerfolder.jni.osx.Util.removeFavorite(baseDir.toAbsolutePath().toString());
        }
    }

    public void setAppReOpenedListener(final Controller controller) {
        try {
            // Load the class com.apple.eawt.Application
            Class<?> appClass = Class.forName("com.apple.eawt.Application");

            // Get the actual application instance
            Method getApplication = appClass
                .getDeclaredMethod("getApplication");

            // The functionallity that should be executed
            InvocationHandler openFrame = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable
                {
                    // Skip, if this is the server.
                    if (controller.getMySelf().isServer()) {
                        return null;
                    }

                    controller.getUIController().getMainFrame().toFront();
                    return null;
                }
            };

            // Get the addAppEventListener method of com.apple.eawt.Application
            Method addAppEventListener = appClass.getMethod(
                "addAppEventListener", Class
                    .forName("com.apple.eawt.AppEventListener"));

            // Get the Interface of AppReOpenedListener
            Class<?> appReOpenedListener = Class
                .forName("com.apple.eawt.AppReOpenedListener");

            // Get the acutal Application instance
            Object application = getApplication.invoke(null, new Object[0]);

            // Associate the InvocationHandler with the AppReOpenedListener Interface
            Object listener = Proxy.newProxyInstance(
                appReOpenedListener.getClassLoader(),
                new Class<?>[]{appReOpenedListener}, openFrame);

            // Add the InvocationHandler as AppReOpenedListener
            addAppEventListener.invoke(application, listener);
        } catch (ClassNotFoundException | SecurityException
            | NoSuchMethodException | IllegalAccessException
            | IllegalArgumentException | InvocationTargetException e)
        {
            logWarning("Could not add the AppReOpenedListener: " + e);
            e.printStackTrace();
        }
    }

    public void setPFStartup(boolean setup, Controller controller)
        throws IOException
    {
        if (!de.dal33t.powerfolder.jni.osx.Util.loaded) {
            logFine("JNI bindings not loaded");
            return;
        }
        String bundleLocation = null;

        try {
            Class<?> c = Class.forName("com.apple.eio.FileManager");
            Method getPathToAppBundle = c
                .getMethod("getPathToApplicationBundle");
            bundleLocation = (String) getPathToAppBundle.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException
            | SecurityException | IllegalAccessException
            | IllegalArgumentException | InvocationTargetException e)
        {
            throw new IOException(
                "Enabling start up item is not supported on your system. Your system is a "
                    + System.getProperty("os.name") + " "
                    + System.getProperty("os.version"));
        }
        
        Path pfile = Paths.get(bundleLocation).toAbsolutePath();
        if (Files.notExists(pfile)) {
            logFine("Reset bundle path");
            pfile = Paths.get(
                controller.getDistribution().getBinaryName() + ".app")
                .toAbsolutePath();
            if (Files.notExists(pfile)) {
                throw new IOException("Couldn't find executable! "
                    + "Note: Setting up a startup shortcut only works "
                    + "when "
                    + controller.getDistribution().getBinaryName()
                    + " was started by " + pfile.getFileName());
            }
        }
        if (setup) {
            de.dal33t.powerfolder.jni.osx.Util.addLoginItem(pfile.toAbsolutePath().toString());
        } else {
            de.dal33t.powerfolder.jni.osx.Util.removeLoginItem(pfile.toAbsolutePath().toString());
        }
    }
}
