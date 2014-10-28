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

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.SyncStatus;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.util.UIUtil;

/**
 * Open the selected file with the standard application of the OS and lock it.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class OpenColaborateAction extends PFContextMenuAction {

    private static final Logger log = Logger
        .getLogger(OpenColaborateAction.class.getName());

    OpenColaborateAction(Controller controller) {
        super(controller);
    }

    @Override
    public void onSelection(String[] paths) {
        if (!Desktop.isDesktopSupported()) {
            log.info("Won't be able to open file. Unsupported operation required.");
            return;
        }

        List<FileInfo> files = getFileInfos(paths);

        for (final FileInfo file : files) {
            if (file.isDiretory()
                || SyncStatus.of(getController(), file) == SyncStatus.IGNORED)
            {
                continue;
            }

            UIUtil.invokeLaterInEDT(new Runnable() {
                @Override
                public void run() {
                    try {
                        Path path = file.getDiskFile(getController()
                            .getFolderRepository());
                        Desktop.getDesktop().open(path.toFile());
                        file.lock(getController());
                    } catch (IOException ioe) {
                        log.warning("Could not open file " + file
                            + " for editing. " + ioe);
                    }
                }
            });
        }
    }

}
