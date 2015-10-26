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

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
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

    private Object application;
    private Object reOpenedListener;
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

    /**
     * Register a listener on the host OS. If the App Icon in the Dock is clicked,
     * the UI will be put to the foreground.
     * 
     * @param controller
     */
    public void setAppReOpenedListener(final Controller controller) {
        try {
            // Load the class com.apple.eawt.Application
            Class<?> appClass = Class.forName("com.apple.eawt.Application");

            // Get the actual application instance
            Method getApplication = appClass
                .getDeclaredMethod("getApplication");

            // Get the addAppEventListener method of com.apple.eawt.Application
            Method addAppEventListener = appClass.getMethod(
                "addAppEventListener", Class
                    .forName("com.apple.eawt.AppEventListener"));

            // Get the Interface of AppReOpenedListener
            Class<?> appReOpenedListener = Class
                .forName("com.apple.eawt.AppReOpenedListener");

            // Get the acutal Application instance
            application = getApplication.invoke(null, new Object[0]);

            // The functionallity that should be executed
            InvocationHandler openFrame = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable
                    {
                    // Skip, if this is the server.
                    if (!controller.isUIEnabled()) {
                        return null;
                    }

                    controller.getUIController().getMainFrame().toFront();
                    return null;
                    }
            };

            // Associate the InvocationHandler with the AppReOpenedListener Interface
            reOpenedListener = Proxy.newProxyInstance(
                appReOpenedListener.getClassLoader(),
                new Class<?>[]{appReOpenedListener}, openFrame);

            // Add the InvocationHandler as AppReOpenedListener
            addAppEventListener.invoke(application, reOpenedListener);
        } catch (ClassNotFoundException | SecurityException
            | NoSuchMethodException | IllegalAccessException
            | IllegalArgumentException | InvocationTargetException e)
        {
            logWarning("Could not add the AppReOpenedListener: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Unregister the listener, set by {@link #setAppReOpenedListener(Controller)}.
     * 
     * @param controller
     */
    public void removeAppReOpenedListener(Controller controller) {
        if (reOpenedListener == null) {
            return;
        }

        try {
            // Load the class com.apple.eawt.Application
            Class<?> appClass = Class.forName("com.apple.eawt.Application");

            Method removeAppEventListener = appClass.getMethod(
                "removeAppEventListener", Class
                .forName("com.apple.eawt.AppEventListener"));

            removeAppEventListener.invoke(application, reOpenedListener);
        } catch (ClassNotFoundException | NoSuchMethodException
            | SecurityException | IllegalAccessException
            | IllegalArgumentException | InvocationTargetException e)
        {
            logWarning("Could not remove the AppReOpenedListener: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Example: {@code "/Applications/PowerFolder.app/Contents/Resources"}
     * 
     * @return A string containing the path to the resources location within an app bundle without a
     *         trailing path separator
     * @throws UnsupportedOperationException
     *             @{@link #getBundleLocation()}
     */
    public String getRecourcesLocation() {
        String appLocation = getBundleLocationWithAppName();
        return appLocation + "/Contents/Resources";
    }

    /**
     * Example: {@code "/Applications/PowerFolder.app"}
     * 
     * @return A string containing the path to and name of the bundle.
     * @throws UnsupportedOperationException @{@link #getBundleLocation()}
     */
    public String getBundleLocationWithAppName()
        throws UnsupportedOperationException
    {
        String bundlePath = getBundleLocation();
        return bundlePath + ".app";
    }

    /**
     * Tries to retriev the location of the app bundle via
     * {@link com.apple.eio.FileManager#getPathToApplicationBundle()}. This
     * method is called using reflection.
     * Example: {@code "/Applications/PowerFolder"}
     * 
     * @return The string representation of the path to the bundle
     * @throws UnsupportedOperationException
     *             If any exception occures during invocation of the
     *             getPathToApplicationBundle() method.
     */
    public String getBundleLocation() throws UnsupportedOperationException {
        try {
            Class<?> c = Class.forName("com.apple.eio.FileManager");
            Method getPathToAppBundle = c
                .getMethod("getPathToApplicationBundle");
            return (String) getPathToAppBundle.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException
            | SecurityException | IllegalAccessException
            | IllegalArgumentException | InvocationTargetException e)
        {
            String message = Translation
                .get("exception.startup_item.unsupported_system.text",
                    System.getProperty("os.name"),
                    System.getProperty("os.version"));
            logWarning(message);
            throw new UnsupportedOperationException(message);
        }
    }

    /**
     * @param setup @code True to set the start up item, @code false to remove it.
     * @param controller
     * @throws UnsupportedOperationException
     *             If requesting the status of a start up item is not supported
     *             on the platform, or the executable ".app" could not be located.
     */
    public void setPFStartup(boolean setup, Controller controller)
        throws UnsupportedOperationException
    {
        if (!de.dal33t.powerfolder.jni.osx.Util.loaded) {
            logFine("JNI bindings not loaded");
            return;
        }

        String bundleLocation = getBundleLocation();
        Path pfile = Paths.get(bundleLocation).toAbsolutePath();

        if (Files.notExists(pfile)) {
            logFine("Reset bundle path");
            pfile = Paths.get(
                controller.getDistribution().getBinaryName() + ".app")
                .toAbsolutePath();
            if (Files.notExists(pfile)) {
                String message = Translation.get(
                    "exception.startup_item.executable_not_found.text", controller
                        .getDistribution().getBinaryName(), pfile.getFileName()
                        .toString());
                logWarning(message);
                throw new UnsupportedOperationException(message);
            }
        }
        if (setup) {
            de.dal33t.powerfolder.jni.osx.Util.addLoginItem(pfile.toAbsolutePath().toString());
        } else {
            de.dal33t.powerfolder.jni.osx.Util.removeLoginItem(pfile.toAbsolutePath().toString());
        }
    }

    /**
     * @param controller
     * @throws UnsupportedOperationException
     *             If requesting the status of a start up item is not supported
     *             on the platform, or the executable ".app" could not be located.
     */
    public boolean hasPFStartup(Controller controller) throws UnsupportedOperationException {
        if (!de.dal33t.powerfolder.jni.osx.Util.loaded) {
            logFine("JNI bindings not loaded");
            return false;
        }

        String bundleLocation = getBundleLocation();
        Path pfile = Paths.get(bundleLocation).toAbsolutePath();

        if (Files.notExists(pfile)) {
            logFine("Reset bundle path");
            pfile = Paths.get(
                controller.getDistribution().getBinaryName() + ".app")
                .toAbsolutePath();
            if (Files.notExists(pfile)) {
                String message = Translation.get(
                    "exception.startup_item.executable_not_found.text", controller
                        .getDistribution().getBinaryName(), pfile.getFileName()
                        .toString());
                logWarning(message);
                throw new UnsupportedOperationException(message);
            }
        }
        return de.dal33t.powerfolder.jni.osx.Util.hasLoginItem(pfile.toAbsolutePath().toString());
    }
}
