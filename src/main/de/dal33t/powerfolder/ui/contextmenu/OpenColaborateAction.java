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
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * Open the file to colaborate in a web colaboration tool.
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
        List<FileInfo> fileInfos = getFileInfos(paths);

        for (FileInfo fileInfo : fileInfos) {
            String fileURL = getController().getOSClient().getOpenURL(fileInfo);
            if (StringUtils.isBlank(fileURL)) {
                log.fine("Could not get URL for file " + fileInfo);
                continue;
            }
            BrowserLauncher.openURL(getController(), fileURL);
        }
    }
}
