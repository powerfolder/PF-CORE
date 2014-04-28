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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;

/** @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A> */
public class PluginManager extends PFComponent {

    private static final Logger log = Logger.getLogger(PluginManager.class
        .getName());
    private static final String OLD_WEBINTERFACE_PLUGIN_CLASS_NAME = "de.dal33t.powerfolder.AB";
    private static final String PLUGIN_PACKAGE_PREFIX = "de.dal33t.powerfolder.";
    
    private List<Plugin> plugins;
    private List<Plugin> disabledPlugins;
    private PluginManagerListener listeners;

    public PluginManager(Controller controller) {
        super(controller);
        plugins = new CopyOnWriteArrayList<Plugin>();
        disabledPlugins = new CopyOnWriteArrayList<Plugin>();
        listeners = ListenerSupportFactory
            .createListenerSupport(PluginManagerListener.class);
    }

    // Start / Stop ***********************************************************

    public void init() {
        readEnabledPlugins();
        readDisabledPlugins();
    }

    /**
     * Starts the plugin manager, reads and starts all plugins.
     */
    public void start() {
        if (plugins.size() + disabledPlugins.size() == 0) {
            logFine("No plugins found to start. Maybe PluginManager not initialized?");
        }
        startEnabledPlugins();
    }

    /** stops all plugins */
    public void shutdown() {
        if (plugins != null) {
            for (Plugin plugin : plugins) {
                try {
                    plugin.stop();
                    plugin.destroy();
                } catch (Exception e) {
                    logSevere("Exception while stopping/destroying plugin: "
                        + plugin + ". " + e, e);
                }
                logFine(plugin.getName() + " stopped and destroyed");
            }
            plugins.clear();
        }
        if (disabledPlugins != null) {
            for (Plugin plugin : disabledPlugins) {
                try {
                    plugin.destroy();
                } catch (Exception e) {
                    logSevere("Exception while destroying plugin: " + plugin
                        + ". " + e, e);
                }
                logFine(plugin.getName() + " destroyed");
            }
            disabledPlugins.clear();
        }

    }

    /**
     * Initializes all plugins
     */
    private void readEnabledPlugins() {
        readAndInitPlugins(
            ConfigurationEntry.PLUGINS.getValue(getController()), plugins,
            "enabled");
    }

    /**
     * reads disabled plugins
     */
    private void readDisabledPlugins() {
        readAndInitPlugins(ConfigurationEntry.PLUGINS_DISABLED
            .getValue(getController()), disabledPlugins, "disabled");
    }

    private void readAndInitPlugins(String pluginsStr, List<Plugin> plugins,
        String typeInfo)
    {
        plugins.clear();
        if (StringUtils.isBlank(pluginsStr)) {
            return;
        }

        boolean containsPrefix = pluginsStr.contains(PLUGIN_PACKAGE_PREFIX);
        if (containsPrefix) {
            pluginsStr = pluginsStr.replaceAll(PLUGIN_PACKAGE_PREFIX, "");
        }

        logFine("Initalizing (" + typeInfo + ") plugins: " + pluginsStr);
        StringTokenizer nizer = new StringTokenizer(pluginsStr, ",");
        while (nizer.hasMoreElements()) {
            String pluginClassName = nizer.nextToken().trim();
            if (StringUtils.isBlank(pluginClassName)) {
                continue;
            }
            if (alreadyLoaded(pluginClassName)) {
                continue;
            }
            Plugin plugin = newPluginInstance(pluginClassName);
            if (plugin != null) {
                plugins.add(plugin);
                plugin.init();
            }
        }

        if (containsPrefix) {
            saveConfig();
        }
    }

    /**
     * Initalized a plugin by classname
     * 
     * @param pluginClassName
     *            the classname of the plugin
     */
    private Plugin newPluginInstance(String pluginClassName) {
        if (StringUtils.isBlank(pluginClassName)) {
            throw new IllegalArgumentException("Plugin string blank");
        }
        if (OLD_WEBINTERFACE_PLUGIN_CLASS_NAME
            .equalsIgnoreCase(pluginClassName))
        {
            logFine("Not loading web interface. "
                + "It not longer available in v4.0 of PowerFolder.");
            return null;
        }

        if (log.isLoggable(Level.FINE)) {
            logFine("Initializing plugin: " + pluginClassName);
        }
        try {
            if (!pluginClassName.contains(".")) {
                pluginClassName = PLUGIN_PACKAGE_PREFIX + pluginClassName;
            }
            Class<?> pluginClass = Class.forName(pluginClassName);
            Plugin plugin;
            try {
                // NoSuchMethodException
                // try to instantiate AbstractPFPlugin
                Constructor<?> constr = pluginClass
                    .getConstructor(Controller.class);
                plugin = (Plugin) constr.newInstance(getController());
            } catch (NoSuchMethodException e) {
                // No constructor with Controller as parameter.
                try {
                    plugin = (Plugin) pluginClass.newInstance();
                } catch (ClassCastException e2) {
                    // failed, not a Plugin to...!
                    log
                        .log(
                            Level.SEVERE,
                            "failed to load: "
                                + pluginClassName
                                + "does not extends AbstractPFPlugin or implements Plugin",
                            e);
                    return null;
                }
            }
            return plugin;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while initializing plugin '"
                + pluginClassName + '\'', e);
        }
        return null;
    }

    /**
     * Starts the enabled plugins
     */
    private void startEnabledPlugins() {
        for (Plugin plugin : plugins) {
            logFine("Starting plugin: " + plugin.getName());
            try {
                plugin.start();
            } catch (Exception e) {
                logSevere("Exception while starting plugin: " + plugin + ". "
                    + e, e);
            }
        }
    }

    /**
     * is this plugin enabled ?
     * 
     * @param plugin
     * @return true if yes
     */
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
     * @param thePlugin
     * @param enabled
     *            new status of the plugin
     */
    public void setEnabled(Plugin thePlugin, boolean enabled) {
        Plugin plugin = findPlugin(thePlugin);
        String pluginName = plugin.getClass().getName();
        int lastDot = pluginName.lastIndexOf('.');
        pluginName = pluginName.substring(lastDot + 1);
        logFine("enable: " + enabled + ' ' + pluginName);
        if (enabled) {
            disabledPlugins.remove(plugin);
            if (!plugins.contains(plugin)) {
                plugins.add(plugin);
            }
            try {
                plugin.start();
            } catch (Exception e) {
                logSevere("Exception while starting plugin: " + pluginName + ". "
                    + e, e);
            }
        } else {
            plugins.remove(plugin);
            if (!disabledPlugins.contains(plugin)) {
                disabledPlugins.add(plugin);
            }
            try {
                plugin.stop();
            } catch (Exception e) {
                logSevere("Exception while stopping plugin: " + pluginName + ". "
                    + e, e);
            }
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
        String pluginName = "";
        for (Plugin plug : plugins) {
            // Only take the name of the Class
            pluginName = plug.getClass().getName();
            int lastDot = pluginName.lastIndexOf('.');
            pluginName = pluginName.substring(lastDot + 1);
            // --
            enabledPluginsPropertyValue += seperator + pluginName;
            seperator = ",";
        }
        ConfigurationEntry.PLUGINS.setValue(getController(),
            enabledPluginsPropertyValue);

        String disabledPluginsPropertyValue = "";
        seperator = "";
        for (Plugin plug : disabledPlugins) {
            // Only take the name of the Class
            pluginName = plug.getClass().getName();
            int lastDot = pluginName.lastIndexOf('.');
            pluginName = pluginName.substring(lastDot + 1);
            // --
            disabledPluginsPropertyValue += seperator + pluginName;
            seperator = ",";
        }
        ConfigurationEntry.PLUGINS_DISABLED.setValue(getController(),
            disabledPluginsPropertyValue);
        getController().saveConfig();
    }

    /**
     * returns all installed plugins
     * 
     * @return the list of all plugins
     */
    public List<Plugin> getPlugins() {
        List<Plugin> pluginsAll = new ArrayList<Plugin>();
        pluginsAll.addAll(plugins);
        pluginsAll.addAll(disabledPlugins);
        return pluginsAll;
    }

    /**
     * the total number of installed plugins
     * 
     * @return the number of plugins
     */
    public int countPlugins() {
        return plugins.size() + disabledPlugins.size();
    }

    /**
     * @param searchPlugin
     * @return the first plugin with the given instance or it's wrapper.
     */
    public Plugin findPlugin(Plugin searchPlugin) {
        Reject.ifNull(searchPlugin, "searchPlugin");
        for (Plugin plugin : plugins) {
            if (searchPlugin.equals(plugin)) {
                return plugin;
            }
            if (plugin instanceof PluginWrapper) {
                if (((PluginWrapper) plugin).getDeligate().equals(searchPlugin))
                {
                    return plugin;
                }
            }
        }
        for (Plugin plugin : disabledPlugins) {
            if (searchPlugin.equals(plugin)) {
                return plugin;
            }
            if (plugin instanceof PluginWrapper) {
                if (((PluginWrapper) plugin).getDeligate().equals(searchPlugin))
                {
                    return plugin;
                }
            }
        }
        return searchPlugin;
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
        ListenerSupportFactory.addListener(listeners, pluginManagerListener);
    }

    public void removePluginManagerListener(
        PluginManagerListener pluginManagerListener)
    {
        ListenerSupportFactory.removeListener(listeners, pluginManagerListener);
    }

    private void firePluginStatusChange(Plugin plugin) {
        listeners.pluginStatusChanged(new PluginEvent(this, plugin));
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
