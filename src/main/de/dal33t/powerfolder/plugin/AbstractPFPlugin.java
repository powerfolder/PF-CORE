package de.dal33t.powerfolder.plugin;

import javax.swing.JDialog;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

/**
 * For your convenience an implementation of the plugin interface that does not
 * have a options dialog and is an PFComponent. PFComoment gives you access to
 * the Controller. The Controller is the access point for all main program
 * elelements. Overwrite the hasOptionsDialog() and showOptionsDialog(JDialog
 * parent) if you do have an options dialog.
 */
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
