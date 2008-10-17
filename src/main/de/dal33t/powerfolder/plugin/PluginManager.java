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
package de.dal33t.powerfolder.plugin;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Reject;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A> */
public class PluginManager extends PFComponent {

    private static final Logger log = Logger.getLogger(PluginManager.class.getName());
    private List<Plugin> plugins;
    private List<Plugin> disabledPlugins;
    private List<PluginManagerListener> listeners;

    public PluginManager(Controller controller) {
        super(controller);
        plugins = new CopyOnWriteArrayList<Plugin>();
        disabledPlugins = new CopyOnWriteArrayList<Plugin>();
        listeners = Collections
            .synchronizedList(new ArrayList<PluginManagerListener>());
    }

    // Start / Stop ***********************************************************

    /**
     * Starts the plugin manager, reads and starts all plugins.
     */
    public void start() {
        readEnabledPlugins();
        readDisabledPlugins();
        startEnabledPlugins();
    }

    /** stops all plugins */
    public void shutdown() {
        if (plugins != null) {
            for (Plugin plugin : plugins) {
                plugin.stop();
                log.fine(plugin.getName() + " stopped");
            }
        }
        plugins.clear();
        disabledPlugins.clear();
    }

    /**
     * Initializes all plugins
     */
    private void readEnabledPlugins() {
        plugins.clear();
        String pluginsStr = ConfigurationEntry.PLUGINS
            .getValue(getController());
        if (StringUtils.isBlank(pluginsStr)) {
            return;
        }
        log.info("Initalizing plugins: " + pluginsStr);
        StringTokenizer nizer = new StringTokenizer(pluginsStr, ",");
        while (nizer.hasMoreElements()) {
            String pluginClassName = nizer.nextToken().trim();
            if (alreadyLoaded(pluginClassName)) {
                continue;
            }
            Plugin plugin = initalizePlugin(pluginClassName);
            if (plugin != null) {
                plugins.add(plugin);
            }
        }
    }

    /**
     * Starts the enabled plugins
     */
    private void startEnabledPlugins() {
        for (Plugin plugin : plugins) {
            log.info("Starting plugin: " + plugin.getName());
            plugin.start();
        }
    }

    /**
     * reads disabled plugins
     */
    private void readDisabledPlugins() {
        disabledPlugins.clear();
        String pluginsStr = ConfigurationEntry.PLUGINS_DISABLED
            .getValue(getController());
        if (StringUtils.isBlank(pluginsStr)) {
            return;
        }
        log.fine("Read disabled plugins: " + pluginsStr);
        StringTokenizer nizer = new StringTokenizer(pluginsStr, ",");
        while (nizer.hasMoreElements()) {
            String pluginClassName = nizer.nextToken();
            if (alreadyLoaded(pluginClassName)) {
                continue;
            }
            Plugin plugin = initalizePlugin(pluginClassName);
            if (plugin != null) {
                log.fine("Found disabled plugin: " + plugin.getName());
                disabledPlugins.add(plugin);
            }
        }
    }

    /**
     * Initalized a plugin by classname
     * 
     * @param pluginClassName
     *            the classname of the plugin
     */
    private Plugin initalizePlugin(String pluginClassName) {
        if (StringUtils.isBlank(pluginClassName)) {
            throw new IllegalArgumentException("Plugin string blank");
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Initializing plugin: " + pluginClassName);
        }
        try {
            Class pluginClass = Class.forName(pluginClassName);
            Plugin plugin;
            try {
                // NoSuchMethodException
                // try to instantiate AbstractPFPlugin
                Constructor constr = pluginClass
                    .getConstructor(Controller.class);
                plugin = (Plugin) constr.newInstance(getController());
            } catch (NoSuchMethodException e) {
                // No constructor with Controller as parameter.
                try {
                    plugin = (Plugin) pluginClass.newInstance();
                } catch (ClassCastException e2) {
                    // failed, not a Plugin to...!
                    log.log(Level.SEVERE,
                            "failed to load: "
                                + pluginClassName
                                + "does not extends AbstractPFPlugin or implements Plugin",
                            e);
                    return null;
                }
            }
            return plugin;
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE,
                "Unable to find plugin class '" + pluginClassName + '\'', e);
        } catch (InstantiationException e) {
            log.log(Level.SEVERE,
                "Unable to find plugin class '" + pluginClassName + '\'', e);
        } catch (InvocationTargetException e) {
            log.log(Level.SEVERE,
                "Unable to find plugin class '" + pluginClassName + '\'', e);
        } catch (IllegalAccessException e) {
            log.log(Level.SEVERE,
                "Unable to find plugin class '" + pluginClassName + '\'', e);
        }
        return null;
    }

    /** is this plugin enabled ? */
    public boolean isEnabled(Plugin plugin) {
        if (plugin == null) {
            return false;
        }
        for (Plugin canidate : plugins) {
            if (canidate.equals(plugin)) {
                return true;
            }
            if (canidate instanceof PluginWrapper) {
                PluginWrapper wrapper = (PluginWrapper) canidate;
                if (wrapper.getDeligate().equals(plugin)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param enabled
     *            new status of the plugin
     */
    public void setEnabled(Plugin plugin, boolean enabled) {
        log.fine("enable: " + enabled + ' ' + plugin);
        if (enabled) {
            disabledPlugins.remove(plugin);
            plugins.add(plugin);
            plugin.start();
        } else {
            plugins.remove(plugin);
            disabledPlugins.add(plugin);
            plugin.stop();
        }
        saveConfig();
        firePluginStatusChange(plugin);
    }

    /**
     * Writes the config file.
     */
    public void saveConfig() {
        String enabledPluginsPropertyValue = "";
        String seperator = "";
        for (Plugin plug : plugins) {
            enabledPluginsPropertyValue += seperator
                + plug.getClass().getName();
            seperator = ",";
        }
        ConfigurationEntry.PLUGINS.setValue(getController(),
            enabledPluginsPropertyValue);

        String disabledPluginsPropertyValue = "";
        seperator = "";
        for (Plugin plug : disabledPlugins) {
            disabledPluginsPropertyValue += seperator
                + plug.getClass().getName();
            seperator = ",";
        }
        ConfigurationEntry.PLUGINS_DISABLED.setValue(getController(),
            disabledPluginsPropertyValue);
        getController().saveConfig();
    }

    /** returns all installed plugins */
    public List<Plugin> getPlugins() {
        List<Plugin> pluginsAll = new ArrayList<Plugin>();
        pluginsAll.addAll(plugins);
        pluginsAll.addAll(disabledPlugins);
        return pluginsAll;
    }

    /** the total number of installed plugins */
    public int countPlugins() {
        return plugins.size() + disabledPlugins.size();
    }

    /**
     * @param searchClass
     * @return the first plugin with the given class.
     */
    public Plugin findPluginByClass(Class<?> searchClass) {
        Reject.ifNull(searchClass, "Clazz is null");
        for (Plugin plugin : plugins) {
            if (plugin instanceof PluginWrapper) {
                plugin = ((PluginWrapper) plugin).getDeligate();
            }
            if (searchClass.isInstance(plugin)) {
                return plugin;
            }
        }
        for (Plugin plugin : disabledPlugins) {
            if (plugin instanceof PluginWrapper) {
                plugin = ((PluginWrapper) plugin).getDeligate();
            }
            if (searchClass.isInstance(plugin)) {
                return plugin;
            }
        }
        return null;
    }

    public void addPluginManagerListener(
        PluginManagerListener pluginManagerListener)
    {
        listeners.add(pluginManagerListener);
    }

    public void removePluginManagerListener(
        PluginManagerListener pluginManagerListener)
    {
        listeners.remove(pluginManagerListener);
    }

    private void firePluginStatusChange(Plugin plugin) {
        for (PluginManagerListener listener : listeners) {
            listener.pluginStatusChanged(new PluginEvent(this, plugin));
        }
    }

    private boolean alreadyLoaded(String pluginClassName) {
        for (Plugin p : plugins) {
            if (p.getClass().getName().equals(pluginClassName)) {
                return true;
            }
        }
        for (Plugin p : disabledPlugins) {
            if (p.getClass().getName().equals(pluginClassName)) {
                return true;
            }
        }
        return false;
    }

}
