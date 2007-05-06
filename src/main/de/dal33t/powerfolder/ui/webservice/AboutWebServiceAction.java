package de.dal33t.powerfolder.ui.webservice;

import java.awt.event.ActionEvent;
import java.io.IOException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.BrowserLauncher;

public class AboutWebServiceAction extends BaseAction {
    protected AboutWebServiceAction(Controller controller) {
        super("aboutwebservice", controller);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            BrowserLauncher
                .openURL("http://www.powerfolder.com/node/webservice");
        } catch (IOException e1) {
            log().error(e1);
        }
    }
}
