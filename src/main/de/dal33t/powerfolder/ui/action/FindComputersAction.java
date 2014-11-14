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
* $Id: FindComputersAction.java 5419 2008-09-29 12:18:20Z harry $
*/
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.friends.FindComputersDialog;

import java.awt.event.ActionEvent;

/**
 * Action which opens search computer dialog.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class FindComputersAction extends BaseAction {
    public FindComputersAction(Controller controller) {
        super("exp.action_find_computers", controller);
    }

    public void actionPerformed(ActionEvent e) {
        FindComputersDialog dialog = new FindComputersDialog(getController());
        dialog.open();
    }
}
