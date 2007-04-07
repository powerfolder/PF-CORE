package de.dal33t.powerfolder.util.os;

import snoozesoft.systray4j.SysTrayMenu;

public class OSUtil {
    //no instances
    private OSUtil() {
        
    }
    
    /**
     * Answers if current system is running windows
     * 
     * @return
     */
    public static boolean isWindowsSystem() {
        String os = System.getProperty("os.name");
        return (os != null) ? os.toLowerCase().indexOf("windows") >= 0 : false;
    }

    /**
     * Answers if the operating system is win Me or older (98, 95)
     * 
     * @return
     */
    public static boolean isWindowsMEorOlder() {
        String os = System.getProperty("os.name");
        return os.endsWith("Me") || os.endsWith("98") || os.endsWith("95");
    }

    /**
     * Answers if the operating system is mac os
     * 
     * @return
     */
    public static boolean isMacOS() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("mac");
    }
    
    /**
     * Answers if the operating system is a linux os
     * 
     * @return
     */
    public static boolean isLinux() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().indexOf("linux") != -1;
    }

    /**
     * Determines if this is a web start via Java WebStart
     * 
     * @return true if started via web
     */
    public static boolean isWebStart() {
        return !System.getProperty("using.webstart", "false").equals("false");
    }

    /**
     * Systray only on win2000 and newer. win 98/ME gives a "could not create
     * main-window error"
     */
    public static boolean isSystraySupported() {
        return isWindowsSystem() && !isWindowsMEorOlder()
            && SysTrayMenu.isAvailable();
    }

}
