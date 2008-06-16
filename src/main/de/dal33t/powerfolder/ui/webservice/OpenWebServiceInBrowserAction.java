/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.ui.webservice;

import java.awt.event.ActionEvent;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
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
            String host = ConfigurationEntry.SERVER_HOST
                .getValue(getController());
            if (!StringUtils.isBlank(host)) {
                // FIXME: Does not work with port extensions
                int i = host.indexOf(":");
                if (i > 0) {
                    host = host.substring(0, i);
                }
                BrowserLauncher.openURL("http://" + host);
            } else {
                // Default
                BrowserLauncher.openURL(Constants.ONLINE_STORAGE_URL);
            }

        } catch (IOException e1) {
            log().error(e1);
        }
    }

}
