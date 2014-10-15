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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.notification.NotificationHandlerBase;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;

/**
 * This notification is shown, when the "share link" context menu item was clicked.<br />
 * <br />
 * It offeres one button to undo the "share link" operation and restore the clipboard.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class ShareFileNotificationHandler extends NotificationHandlerBase {

    ShareFileNotificationHandler(final Controller controller,
        final FileInfo fInfo, final String previousClipboardContents)
    {
        super(controller);

        setTitle(Translation
            .getTranslation("context_menu.share_link.notification.title"));
        setMessageText(Translation.getTranslation(
            "context_menu.share_link.notification.message",
            fInfo.getFilenameOnly()));

        setCancelOptionLabel(Translation
            .getTranslation("context_menu.share_link.notification.cancel_label"));
        setCancelAction(new AbstractAction() {
            private static final long serialVersionUID = 100L;

            @Override
            public void actionPerformed(ActionEvent e) {
                controller.getIOProvider().startIO(new Runnable() {
                    @Override
                    public void run() {
                        sliderClose();
                        Util.setClipboardContents(previousClipboardContents);

                        try {
                            controller.getOSClient().getFolderService()
                                .removeFileLink(fInfo);
                        } catch (RuntimeException re) {
                            logWarning("The server you use does not support file link removal. Please consider to update your server.");
                        }
                    }
                });
            }
        });
    }
}
