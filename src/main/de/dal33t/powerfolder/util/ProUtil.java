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
     * @return true if the pro version is running.
     */
    public static final boolean isRunningProVersion() {
        return Util.class.getClassLoader().getResourceAsStream(
            "de/dal33t/powerfolder/ConfigurationProEntry.class") != null;
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
}
