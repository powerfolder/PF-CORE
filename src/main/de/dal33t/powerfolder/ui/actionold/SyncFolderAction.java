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
import de.dal33t.powerfolder.ui.action.BaseAction;

import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Action to manually sync a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public class SyncFolderAction extends BaseAction {

    public SyncFolderAction(Controller controller) {
        super("scan_folder", controller);
        // Override icon
        putValue(SMALL_ICON, null);

        // Note: the accelerator is not actually used here;
        // It just puts the Alt-1 text on the Nav tree pop-up item.
        // See MainFrame.MySyncFolderAction.
        putValue(ACCELERATOR_KEY,
                KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
    }

    public void actionPerformed(ActionEvent e) {
//        getController().getUIController().getApplicationModel()
//                .getFolderRepositoryModel().scanSelectedFolder();
    }
}