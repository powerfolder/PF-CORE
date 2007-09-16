package de.dal33t.powerfolder.ui.webservice;

import java.awt.event.ActionEvent;
import java.io.IOException;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.BrowserLauncher;

public class OpenWebServiceInBrowserAction extends BaseAction {

    protected OpenWebServiceInBrowserAction(Controller controller) {
        super("openwebservice", controller);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            BrowserLauncher.openURL(Constants.ONLINE_STORAGE_URL);
        } catch (IOException e1) {
            log().error(e1);
        }
    }

}
