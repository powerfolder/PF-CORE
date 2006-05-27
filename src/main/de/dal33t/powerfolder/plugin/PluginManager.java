package de.dal33t.powerfolder.plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

/** @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A> */
public class PluginManager extends PFComponent {
    private List<Plugin> plugins;

    public PluginManager(Controller controller) {
        super(controller);
        initalizePlugins();
    }

    /**
     * Initalizes all plugins
     */
    private void initalizePlugins() {
        String pluginsStr = getController().getConfig().getProperty("plugins");
        if (StringUtils.isBlank(pluginsStr)) {
            return;
        }
        log().warn("Initalizing plugins: " + pluginsStr);
        plugins = Collections.synchronizedList(new ArrayList<Plugin>());
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

    /** returns all installed plugins */
    public List<Plugin> getPlugins() {
        if (plugins == null) {
            return null;
        }
        List<Plugin> pluginsCopy = new ArrayList<Plugin>();
        synchronized (plugins) {
            pluginsCopy.addAll(plugins);
        }
        return pluginsCopy;
    }

    /** the number of installed plugins */
    public int countPlugins() {
        if (plugins == null) {
            return 0;
        }
        return plugins.size();
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
}
