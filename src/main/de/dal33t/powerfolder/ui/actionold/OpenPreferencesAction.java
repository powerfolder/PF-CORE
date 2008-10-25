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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.preferences.PreferencesDialog;

import java.awt.event.ActionEvent;

/**
 * Actions which is executed to open the preferences
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.7 $
 */
public class OpenPreferencesAction extends BaseAction {
    private PreferencesDialog panel;
    
    public OpenPreferencesAction(Controller controller) {
        super("action_open_preferences", controller);
    }

    public void actionPerformed(ActionEvent e) {
        if (panel == null) {
            panel = new PreferencesDialog(getController());
        }
        panel.open();
    }
}