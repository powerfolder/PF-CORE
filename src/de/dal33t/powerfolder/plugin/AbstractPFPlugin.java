package de.dal33t.powerfolder.plugin;

import javax.swing.JDialog;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

public abstract class AbstractPFPlugin extends PFComponent implements Plugin {

    public AbstractPFPlugin(Controller controller) {
        super(controller);
    }

    /** overwrite this to return true if you implement showOptionsDialog */
    public boolean hasOptionsDialog() {
        return false;
    }

    public void showOptionsDialog(JDialog parent) {
        throw new IllegalStateException(
            "default no options dialog, overwrite this method");
    }

    public String toString() {
        return getName();
    }
}
