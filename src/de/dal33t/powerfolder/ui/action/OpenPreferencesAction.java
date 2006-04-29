/* $Id: OpenPreferencesAction.java,v 1.6 2006/01/23 00:37:08 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.dialog.PreferencesPanel;

/**
 * Actions which is executed to open the preferences
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class OpenPreferencesAction extends BaseAction {
    private PreferencesPanel panel;
    
    public OpenPreferencesAction(Controller controller) {
        super("preferences", controller);
    }

    public void actionPerformed(ActionEvent e) {
        if (panel == null) {
            panel = new PreferencesPanel(getController());
        }
        panel.open();
    }
}