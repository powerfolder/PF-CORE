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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.SyncStatus;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.Util;

/**
 * The action performed, when the "share link" context menu item was clicked.<br />
 * <br />
 * It remembers the previous clipboard entry, sets the file link as new
 * clipboard entry and shows a notification.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class ShareLinkAction extends ContextMenuAction {

    private static final Logger log = Logger.getLogger(ShareLinkAction.class
        .getName());
    private Controller controller;

    ShareLinkAction(Controller controller) {
        this.controller = controller;
    }

    @Override
    public void onSelection(String[] paths) {
        try {
            for (String pathName : paths) {
                Path path = Paths.get(pathName);

                for (Folder folder : controller.getFolderRepository()
                    .getFolders())
                {
                    if (!path.startsWith(folder.getLocalBase())) {
                        continue;
                    }

                    final ServerClient client = controller.getOSClient();
                    final FileInfo fInfo = FileInfoFactory.lookupInstance(
                        folder, path);
                    if (SyncStatus.of(controller, fInfo, folder) == SyncStatus.IGNORED)
                    {
                        log.fine("File " + fInfo
                            + " is ignored. Not trying to create link");
                        continue;
                    }

                    controller.getIOProvider().startIO(new Runnable() {
                        @Override
                        public void run() {
                            String previousEntry = Util.getClipboardContents();
                            String url = client.getFolderService()
                                .getDownloadLink(fInfo);
                            Util.setClipboardContents(url);

                            ShareFileNotificationHandler handler = new ShareFileNotificationHandler(
                                controller, fInfo, previousEntry);
                            handler.show();
                        }
                    });
                }
            }
        } catch (RuntimeException re) {
            log.log(Level.WARNING, "Problem while trying to share link. " + re,
                re);
        }
    }
}
