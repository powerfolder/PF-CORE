package de.dal33t.powerfolder.ui.webservice;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.wizard.PFWizard;

public class MirrorFolderAction extends BaseAction {

    protected MirrorFolderAction(Controller controller) {
        super("mirrorfolder", controller);
    }

    public void actionPerformed(ActionEvent e) {
        if (getController().getWebServiceClient().isAccountSet()) {
            PFWizard.openMirrorFolderWizard(getController());
        } else {
            PFWizard.openLoginWebServiceWizard(getController(), true);
        }
    }
}
