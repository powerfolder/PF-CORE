package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.dialog.AboutDialog;

/**
 * Creates an Action event that displays the About Box dialog.
 * 
 * @author <a href=mailto:xsj@users.sourceforge.net">Daniel Harabor</a>
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * 
 * @version 1.0 	Last Modified: 10/04/05
 */


public class OpenAboutBoxAction extends BaseAction {
    public OpenAboutBoxAction(Controller controller) {
        super("about", controller);
    }

    public void actionPerformed(ActionEvent e) {
    	//AboutBoxDialog aboutbox = new AboutBoxDialog(getController(), true);
        AboutDialog aboutbox = new AboutDialog(getController());
        aboutbox.open();
    	//aboutbox.open();
    }
}