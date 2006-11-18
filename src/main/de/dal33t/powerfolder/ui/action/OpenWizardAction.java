/* $Id: OpenWizardAction.java,v 1.2 2006/02/12 18:52:46 jsallis Exp $
 */

package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.WhatToDoPanel;

/**
 * Action that invokes the wizard
 * 
 * @author <a href="mailto:jsallis@users.sourceforge.net">Jason Sallis</a>
 * @version $Revision: 1.2 $
 */
public class OpenWizardAction extends BaseAction {
    public OpenWizardAction(Controller controller) {
        super("wizard", controller);
    }

    public void actionPerformed(ActionEvent e) {
        PFWizard wizard = new PFWizard(getController());
        wizard.getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.FILESHARING_PICTO);

        // FolderInfo foInfo = new FolderInfo("testfolder", "testid", true);
        // wizard.getWizardContext().setAttribute(
        // ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE, foInfo);

        // wizard.open(new SendInvitationsPanel(getController()));
        wizard.open(new WhatToDoPanel(getController()));
    }
}
