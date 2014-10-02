/*
 * Copyright 2004 - 2014 Christian Sprajc. All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.contextmenu;

import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * Set file to be "in use for edit" to display a message to the user.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class LockUnlockAction extends ContextMenuAction {

    private Controller controller;
    private FileInfo fInfo;

    LockUnlockAction(Controller controller, FileInfo fInfo) {
        this.controller = controller;
        this.fInfo = fInfo;
    }

    @Override
    public void onSelection(String[] paths) {
        // TODO: lock/unlock file here.
    }
}
