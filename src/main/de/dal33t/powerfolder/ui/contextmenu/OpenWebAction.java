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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.BrowserLauncher;

/**
 * Open a folder, file or directory on the web
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class OpenWebAction extends PFContextMenuAction {

    private static final Logger log = Logger.getLogger(OpenWebAction.class
        .getName());

    OpenWebAction(Controller controller) {
        super(controller);
    }

    @Override
    public void onSelection(String[] paths) {
        List<FileInfo> fileInfos = getFileInfos(paths);

        for (FileInfo fileInfo : fileInfos) {
            try {
                String name = fileInfo.getRelativeName();

                if (fileInfo.isFile()) {
                    name = fileInfo.getRelativeName().replace(
                        fileInfo.getFilenameOnly(), "");
                }

                String folderURL = getController().getOSClient()
                    .getFolderURLWithCredentials(fileInfo.getFolderInfo());
                String fileURL = folderURL + "/"
                    + URLEncoder.encode(name, "UTF-8");

                BrowserLauncher.openURL(getController(), fileURL);
            } catch (UnsupportedEncodingException uee) {
                log.warning("Failed to generate URL. " + uee);
            }
        }

        List<Folder> folders = getFolders(paths);

        for (Folder folder : folders) {
            String folderURL = getController().getOSClient()
                .getFolderURLWithCredentials(folder.getInfo());

            BrowserLauncher.openURL(getController(), folderURL);
        }
    }
}
