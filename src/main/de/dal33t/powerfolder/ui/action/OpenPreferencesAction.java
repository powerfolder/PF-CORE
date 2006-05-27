/* $Id: OpenPreferencesAction.java,v 1.7 2006/04/30 19:33:33 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.preferences.PreferencesDialog;

/**
 * Actions which is executed to open the preferences
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class OpenPreferencesAction extends BaseAction {
    private PreferencesDialog panel;
    
    public OpenPreferencesAction(Controller controller) {
        super("preferences", controller);
    }

    public void actionPerformed(ActionEvent e) {
        if (panel == null) {
            panel = new PreferencesDialog(getController());
        }
        panel.open();
    }
}