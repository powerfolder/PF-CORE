package de.dal33t.powerfolder.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jdesktop.jdic.desktop.Desktop;
import org.jdesktop.jdic.desktop.DesktopException;

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
 * Bytekeeper 04.08: Changed to jdesktop style
 * 
 * @version $Revision: 1.5 $
 */
public class BrowserLauncher {
    
    public static void openURL(String url) throws IOException {
        try {
            Desktop.browse(new URL(url));
        } catch (MalformedURLException e) {
            throw new IOException(e);
        } catch (DesktopException e) {
            throw new IOException(e);
        } catch (UnsatisfiedLinkError e) {
            throw new IOException(e);
        }
    }
}