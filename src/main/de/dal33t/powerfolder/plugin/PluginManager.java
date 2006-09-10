package de.dal33t.powerfolder.plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

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
        plugins = Collections.synchronizedList(new ArrayList<Plugin>());
        disabledPlugins = Collections.synchronizedList(new ArrayList<Plugin>());
        listeners = Collections
            .synchronizedList(new ArrayList<PluginManagerListener>());
        initializePlugins();
        readDisabledPlugins();
    }

    /**
     * Initializes all plugins
     */
    private void initializePlugins() {
        String pluginsStr = ConfigurationEntry.PLUGINS
            .getValue(getController());
        if (StringUtils.isBlank(pluginsStr)) {
            return;
        }
        log().info("Initalizing plugins: " + pluginsStr);
        StringTokenizer nizer = new StringTokenizer(pluginsStr, ",");
        while (nizer.hasMoreElements()) {
            String pluginClassName = nizer.nextToken();
            Plugin plugin = initalizePlugin(pluginClassName);
            if (plugin != null) {
                plugin.start();
                log().info("Started plugin: " + plugin.getName());
                plugins.add(plugin);
            }
        }
    }

    /**
     * reads disabled plugins
     */
    private void readDisabledPlugins() {
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
                log().info("Found disabled plugin: " + plugin.getName());
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
            Class generalClass = Class.forName(pluginClassName);
            Class pluginClass;
            Plugin plugin;
            try {
                // try to instantiate AbstractPFPlugin
                pluginClass = generalClass.asSubclass(AbstractPFPlugin.class);
                Constructor constr = pluginClass
                    .getConstructor(Controller.class);
                AbstractPFPlugin pluginObject = (AbstractPFPlugin) constr
                    .newInstance(getController());
                plugin = pluginObject;
            } catch (ClassCastException e) {
                // e.printStackTrace();
                // failed not a AbstractPFPlugin
                // maybe a Plugin?
                try {
                    pluginClass = generalClass.asSubclass(Plugin.class);
                    plugin = (Plugin) generalClass.newInstance();
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
        } catch (NoSuchMethodException e) {
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
            synchronized (disabledPlugins) {
                disabledPlugins.remove(plugin);
            }
            plugin.start();
            synchronized (plugins) {
                plugins.add(plugin);
            }
        } else {
            synchronized (plugins) {
                plugins.remove(plugin);
            }
            plugin.stop();
            synchronized (disabledPlugins) {
                disabledPlugins.add(plugin);
            }
        }
        String enabledPluginsPropertyValue = "";
        String seperator = "";
        synchronized (plugins) {
            for (Plugin plug : plugins) {
                enabledPluginsPropertyValue += seperator
                    + plug.getClass().getName();
                seperator = ";";
            }
        }
        ConfigurationEntry.PLUGINS.setValue(getController(),
            enabledPluginsPropertyValue);
        String disabledPluginsPropertyValue = "";
        seperator = "";
        synchronized (disabledPlugins) {
            for (Plugin plug : disabledPlugins) {
                disabledPluginsPropertyValue += seperator
                    + plug.getClass().getName();
                seperator = ";";
            }
        }
        ConfigurationEntry.PLUGINS_DISABLED.setValue(getController(),
            disabledPluginsPropertyValue);
        firePluginStatusChange(plugin);
    }

    /** returns all installed plugins */
    public List<Plugin> getPlugins() {
        List<Plugin> pluginsAll = new ArrayList<Plugin>();
        synchronized (plugins) {
            pluginsAll.addAll(plugins);
        }
        synchronized (disabledPlugins) {
            pluginsAll.addAll(disabledPlugins);
        }
        return pluginsAll;
    }

    /** the total number of installed plugins */
    public int countPlugins() {
        return plugins.size() + disabledPlugins.size();
    }

    /** stops all plugins */
    public void shutdown() {
        if (plugins != null) {
            synchronized (plugins) {
                for (Plugin plugin : plugins) {
                    plugin.stop();
                    log().debug(plugin.getName() + " stopped");
                }
            }
        }
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
