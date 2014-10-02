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

import java.util.logging.Logger;

import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.BrowserLauncher;

/**
 * Open a folder, file or directory on the web
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class OpenWebAction extends ContextMenuAction {

    private static final Logger log = Logger.getLogger(OpenWebAction.class.getName());
    private Controller controller;
    private FileInfo fInfo;

    OpenWebAction(final Controller controller, final FileInfo fInfo) {
        this.controller = controller;
        this.fInfo = fInfo;
    }

    @Override
    public void onSelection(String[] paths) {
        if (paths.length != 1) {
            log.info("More than one file selected");
            return;
        }

        String folderURL = controller.getOSClient().getFolderURL(fInfo.getFolderInfo());
        String fileURL = folderURL + fInfo.getRelativeName();

        BrowserLauncher.openURL(controller, fileURL);
    }
}
