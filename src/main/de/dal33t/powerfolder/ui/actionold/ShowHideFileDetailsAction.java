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

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.HasDetailsPanel;

/**
 * Action for toggeling a visibility of a file details panel. Makes it
 * visible/non-visible on actionPerformed
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
@SuppressWarnings("serial")
public class ShowHideFileDetailsAction extends BaseAction {

    private HasDetailsPanel hasDetailsPanel;

    public ShowHideFileDetailsAction(HasDetailsPanel hasDetailsPanel,
        Controller controller) {
        super("show_hide_file_details", controller);
        if (hasDetailsPanel == null) {
            throw new NullPointerException("File details panel is null");
        }
        this.hasDetailsPanel = hasDetailsPanel;
    }

    public void actionPerformed(ActionEvent e) {
        // Toggle visibility
        hasDetailsPanel.toggleDetails();
    }
}