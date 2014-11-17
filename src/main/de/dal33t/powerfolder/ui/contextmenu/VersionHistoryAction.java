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

import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.wizard.PFWizard;

/**
 * Open the restore wizard to show the version history of a file.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class VersionHistoryAction extends PFContextMenuAction {

    VersionHistoryAction(Controller controller) {
        super(controller);
    }

    @Override
    public void onSelection(String[] paths) {
        final List<FileInfo> fileInfos = getFileInfos(paths);

        getController().getIOProvider().startIO(new Runnable() {
            @Override
            public void run() {
                PFWizard.openMultiFileRestoreWizard(getController(), fileInfos);
            }
        });
    }
}
