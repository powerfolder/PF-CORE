package de.dal33t.powerfolder.jni.osx;

import java.util.logging.Logger;

public class Util {

    public static Logger LOG = Logger.getLogger(Util.class.getName());

    public static boolean loaded = false;
    static {
        try {
            System.loadLibrary("osxnative");
            loaded = true;
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError err) {
            LOG.warning("Could not initialize JNI Mac util: " + err);
        }
    }

    public native static void addLoginItem(String path);
    public native static void removeLoginItem(String path);
    public native static boolean hasLoginItem(String path);
    public native static void addFavorite(String path);
    public native static void removeFavorite(String path);
}
