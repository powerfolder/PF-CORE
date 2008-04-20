/* $Id: FolderCreateAction.java,v 1.6 2005/10/27 18:56:11 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.wizard.ChooseDiskLocationPanel;
import de.dal33t.powerfolder.ui.wizard.FolderSetupPanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.Icons;

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
        FolderSetupPanel setupPanel = new FolderSetupPanel(getController(),
            null);
        ChooseDiskLocationPanel panel = new ChooseDiskLocationPanel(
            getController(), null, setupPanel);
        PFWizard wizard = new PFWizard(getController());
        wizard.getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.FILESHARING_PICTO);
        wizard.open(panel);
    }
}