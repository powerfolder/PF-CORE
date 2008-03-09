/* $Id: FolderCreateAction.java,v 1.6 2005/10/27 18:56:11 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.wizard.FolderCreatePanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;

/**
 * Shares a folder action
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class FolderCreateAction extends BaseAction {
    public FolderCreateAction(Controller controller) {
        super("foldercreate", controller);
    }

    public void actionPerformed(ActionEvent e) {
        FolderCreatePanel panel = new FolderCreatePanel(getController(), null);
        PFWizard wizard = new PFWizard(getController());
        wizard.open(panel);
    }
}