/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: Util.java 8866 2009-08-05 17:07:17Z tot $
 */
package de.dal33t.powerfolder.util;

import java.lang.reflect.Method;
import java.security.PublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Utility to get information about Pro stuff.
 * 
 * @author sprajc
 */
public class ProUtil {
    private static final Logger LOG = Logger.getLogger(Util.class.getName());

    private ProUtil() {
    }
    
    /**
     * Remove this hack.
     * 
     * @param controller
     * @return
     */
    public static final boolean isZyncro(Controller controller) {
        return controller.getDistribution().getBinaryName().contains("yncro")
            || controller.getDistribution().getName().contains("yncro");
    }

    public static final boolean isSwitchData(Controller controller) {
        return controller.getDistribution().getBinaryName().toLowerCase().trim().contains("switchdata")
            || controller.getDistribution().getName().toLowerCase().trim().contains("switchdata");
    }
    
    public static final boolean isServerConfig(Controller controller) {
        return controller.getConfig().get("plugin.server.maintenancefolderid") != null;
    }

    /**
     * @param controller
     * @return the primary buy now URL
     */
    public static final String getBuyNowURL(Controller controller) {
        String simpleURL = ConfigurationEntry.PROVIDER_BUY_URL
            .getValue(controller);
        if (StringUtils.isBlank(simpleURL)) {
            return null;
        } else {
            return simpleURL;
        }
        // ServerClient client = controller.getOSClient();
        // if (StringUtils.isBlank(client.getUsername())
        // && client.getPassword() != null)
        // {
        // return simpleURL;
        // }

        // String loginURL = client.getLoginURLWithCredentials();
        // HACK(tm) Redirect to https://my.powerfolder.com/upgrade.html does
        // not work!
        // return loginURL;

        // String url = simpleURL;
        // url = loginURL;
        // if (loginURL.contains("?")) {
        // url += '&';
        // } else {
        // url += '?';
        // }
        // url += "originalURI=" + Util.endcodeForURL(simpleURL);
        // return url;
    }

    /**
     * @return true if the pro version is running.
     */
    public static final boolean isRunningProVersion() {
        return Util.class.getClassLoader().getResourceAsStream(
            "de/dal33t/powerfolder/ConfigurationProEntry.class") != null;
    }

    /**
     * @return true if the server version is running.
     */
    public static final boolean isRunningServerVersion() {
        return Util.class.getClassLoader().getResourceAsStream(
            "de/dal33t/powerfolder/ConfigurationServerEntry.class") != null;
    }

    /**
     * @param controller
     * @return true if running a trial or non-registered version.
     */
    public static final boolean isTrial(Controller controller) {
        if (!isRunningProVersion()) {
            return true;
        }
        try {
            Class<?> c = Class.forName(Constants.PRO_LOADER_PLUGIN_CLASS);
            Method m = c.getMethod("isTrial", Controller.class);
            return (Boolean) m.invoke(null, controller);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception. " + e, e);
        }
        return true;
    }

    /**
     * @param controller
     * @return true if running a trial or non-registered version.
     */
    public static final boolean isAllowedToRun(Controller controller) {
        try {
            Class<?> c = Class.forName(Constants.PRO_LOADER_PLUGIN_CLASS);
            Method m = c.getMethod("isAllowedToRun", Controller.class);
            return (Boolean) m.invoke(null, controller);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception. " + e, e);
        }
        return true;
    }

    public static final PublicKey getPublicKey(Controller controller,
        MemberInfo node)
    {
        if (!ProUtil.isRunningProVersion()) {
            return null;
        }
        try {
            Class<?> c = Class.forName(Constants.ENCRYPTION_PLUGIN_CLASS);
            Method m = c.getMethod("getPublicKey", Controller.class,
                MemberInfo.class);
            return (PublicKey) m.invoke(null, controller, node);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception. " + e, e);
        }
        return null;
    }

    /**
     * Adds a key for a node to the keystore if the key is new.
     * 
     * @param controller
     * @param node
     * @param key
     * @return true if new key was inserted otherwise false.
     */
    public static final boolean addNodeToKeyStore(Controller controller,
        MemberInfo node, PublicKey key)
    {
        try {
            Class<?> c = Class.forName(Constants.ENCRYPTION_PLUGIN_CLASS);
            Method m = c.getMethod("addNodeToKeyStore", Controller.class,
                MemberInfo.class, PublicKey.class);
            return (Boolean) m.invoke(null, controller, node, key);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception. " + e, e);
        }
        return false;
    }

    /**
     * #2219
     */
    public static final String rtrvePwssd(Controller controller, String input) {
        try {
            Class<?> c = Class.forName(Constants.PRO_LOADER_PLUGIN_CLASS);
            Method m = c
                .getMethod("rtrvePwssd", Controller.class, String.class);
            return (String) m.invoke(null, controller, input);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Exception. " + e, e);
        }
        return null;
    }
}
