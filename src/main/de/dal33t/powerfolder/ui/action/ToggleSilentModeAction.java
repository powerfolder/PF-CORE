/* $Id: ToggleSilentModeAction.java,v 1.4 2005/10/27 19:00:37 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;

/**
 * Action which enables/disables silent mode
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class ToggleSilentModeAction extends BaseAction {

    public ToggleSilentModeAction(Controller controller) {
        super(null, null, controller);
        // Setup
        adaptForScanSetting(getController().isSilentMode());
        // Bind to property
        getController().addPropertyChangeListener(
            "silentMode", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    boolean scansEnabled = ((Boolean) evt.getNewValue())
                        .booleanValue();
                    adaptForScanSetting(scansEnabled);
                }
            });
    }

    private void adaptForScanSetting(boolean silentMode) {
        if (silentMode) {
            configureFromActionId("disablesilentmode");
            // Workaround for toolbar (for toolbar)
            putValue(Action.SMALL_ICON, Icons.SLEEP);
            //putValue(Action.NAME, null);
        } else {
            configureFromActionId("enablesilentmode");
            // Workaround for toolbar (for toolbar)
            putValue(Action.SMALL_ICON, Icons.WAKE_UP);
            //putValue(Action.NAME, null);
        }
       
    }

    public void actionPerformed(ActionEvent e) {
        // Just toggle
        getController().setSilentMode(!getController().isSilentMode());
        log().verbose("Is silentmode: " + getController().isSilentMode());
    }

}