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
package de.dal33t.powerfolder.ui.actionold;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.BrowserLauncher;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class BuyProAction extends BaseAction {

    private static final Logger log = Logger.getLogger(BuyProAction.class.getName());

    public BuyProAction(Controller controller) {
        super("buy_pro", controller);
    }

    public void actionPerformed(ActionEvent e) {
        try {
            BrowserLauncher.openURL(Constants.POWERFOLDER_PRO_URL);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "IOException", ex);
        }
    }

}
