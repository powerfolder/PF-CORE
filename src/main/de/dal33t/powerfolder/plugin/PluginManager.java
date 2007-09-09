package de.dal33t.powerfolder.plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

/** @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A> */
public class PluginManager extends PFComponent {
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
                log().debug(plugin.getName() + " stopped");
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
        log().info("Initalizing plugins: " + pluginsStr);
        StringTokenizer nizer = new StringTokenizer(pluginsStr, ",");
        while (nizer.hasMoreElements()) {
            String pluginClassName = nizer.nextToken().trim();
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
            log().info("Starting plugin: " + plugin.getName());
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
        log().warn("Initalizing plugins: " + pluginsStr);
        StringTokenizer nizer = new StringTokenizer(pluginsStr, ",");
        while (nizer.hasMoreElements()) {
            String pluginClassName = nizer.nextToken();
            Plugin plugin = initalizePlugin(pluginClassName);
            if (plugin != null) {
                log().debug("Found disabled plugin: " + plugin.getName());
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
                    log()
                        .error(
                            "failed to load: "
                                + pluginClassName
                                + "does not extends AbstractPFPlugin or implements Plugin",
                            e);
                    return null;
                }
            }
            return plugin;
        } catch (ClassNotFoundException e) {
            log().error(
                "Unable to find plugin class '" + pluginClassName + "'", e);
        } catch (InstantiationException e) {
            log().error(
                "Unable to find plugin class '" + pluginClassName + "'", e);
        } catch (InvocationTargetException e) {
            log().error(
                "Unable to find plugin class '" + pluginClassName + "'", e);
        } catch (IllegalAccessException e) {
            log().error(
                "Unable to find plugin class '" + pluginClassName + "'", e);
        }
        return null;
    }

    /** is this plugin enabled ? */
    public boolean isEnabled(Plugin plugin) {
        if (plugin == null) {
            return false;
        }
        return plugins.contains(plugin);
    }

    /**
     * @param enabled
     *            new status of the plugin
     */
    public void setEnabled(Plugin plugin, boolean enabled) {
        log().debug("enable: " + enabled + " " + plugin);
        if (enabled) {
            disabledPlugins.remove(plugin);
            plugins.add(plugin);
            plugin.start();
        } else {
            plugin.stop();
            plugins.remove(plugin);
            disabledPlugins.add(plugin);
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

}
