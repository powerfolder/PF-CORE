package de.dal33t.powerfolder.plugin;

import de.dal33t.powerfolder.ui.preferences.PreferencesDialog;
import de.dal33t.powerfolder.util.Reject;

/**
 * Plugin wrapper.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class PluginWrapper implements Plugin {
    private Plugin deligate;

    public PluginWrapper(Plugin plugin) {
        Reject.ifNull(plugin, "plugin is null");
        this.deligate = plugin;
    }

    public Plugin getDeligate() {
        return deligate;
    }

    public String getDescription() {
        return deligate.getDescription();
    }

    public String getName() {
        return deligate.getName();
    }

    public void start() {
        deligate.start();
    }

    public void stop() {
        deligate.stop();
    }

    public boolean hasOptionsDialog() {
        return deligate.hasOptionsDialog();
    }

    public void showOptionsDialog(PreferencesDialog prefDialog) {
        deligate.showOptionsDialog(prefDialog);
    }
}
