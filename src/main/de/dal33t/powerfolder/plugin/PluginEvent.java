package de.dal33t.powerfolder.plugin;

import java.util.EventObject;

public class PluginEvent extends EventObject {
    private Plugin plugin;
    public PluginEvent(Object source, Plugin plugin) {
        super(source);
        this.plugin = plugin;
    }
    
    public Plugin getPlugin() {
        return plugin;
    }

}
