/* $Id: ToggleSilentModeAction.java,v 1.4 2005/10/27 19:00:37 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.Toolbar;

/**
 * Action which enables/disables silent mode
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class ToggleSilentModeAction extends BaseAction {

    private boolean smallToolbar;

    public ToggleSilentModeAction(Controller controller) {
        super(null, null, controller);
        smallToolbar = PreferencesEntry.SMALL_TOOLBAR.getValueBoolean(getController());
        // Setup
        adaptForScanSetting(getController().isSilentMode());
        // Bind to property
        getController().addPropertyChangeListener(
            Controller.PROPERTY_SILENT_MODE, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    boolean scansEnabled = (Boolean) evt.getNewValue();
                    adaptForScanSetting(scansEnabled);
                }
            });
    }

    private void adaptForScanSetting(boolean silentMode) {
        // Workaround for toolbar (for toolbar)
        if (silentMode) {
            configureFromActionId("disablesilentmode");
            if (smallToolbar) {
                ImageIcon scaledImage =
                        Icons.scaleIcon((ImageIcon) Icons.SLEEP,
                        Toolbar.SMALL_ICON_SCALE_FACTOR);
                putValue(Action.SMALL_ICON, scaledImage);
            } else {
                putValue(Action.SMALL_ICON, Icons.SLEEP);
            }
        } else {
            configureFromActionId("enablesilentmode");
            if (smallToolbar) {
                ImageIcon scaledImage =
                        Icons.scaleIcon((ImageIcon) Icons.WAKE_UP,
                        Toolbar.SMALL_ICON_SCALE_FACTOR);
                putValue(Action.SMALL_ICON, scaledImage);
            } else {
                putValue(Action.SMALL_ICON, Icons.WAKE_UP);
            }
        }

        if (smallToolbar) {
            putValue(Action.NAME, null);
        }
       
    }

    public void actionPerformed(ActionEvent e) {
        // Just toggle
        getController().setSilentMode(!getController().isSilentMode());
        log().verbose("Is silentmode: " + getController().isSilentMode());
    }

}