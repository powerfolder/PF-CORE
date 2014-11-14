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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.notification.NotificationHandlerBase;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;

/**
 * This handler is called, when the "share folder" context menu item was
 * clicked.<br />
 * <br />
 * There are one or two buttons, depending on the client configuration.<br />
 * One button offers to show the {@link Folder} on the Web (if allowed), the
 * other opens the invitation wizard (
 * {@link PFWizard#openSendInvitationWizard(Controller, FolderInfo)}).
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class ShareFolderNotificationHandler extends NotificationHandlerBase {

    ShareFolderNotificationHandler(final Controller controller,
        final FolderInfo foInfo)
    {
        super(controller);

        setTitle(Translation
            .getTranslation("context_menu.share_folder.notification.title"));
        setMessageText(Translation.getTranslation(
            "context_menu.share_folder.notification.message",
            foInfo.getLocalizedName(), foInfo.getFolder(controller)
                .getLocalBase().toString()));

        // Show in Web
        if (ConfigurationEntry.WEB_LOGIN_ALLOWED.getValueBoolean(controller)) {
            setAcceptOptionLabel(Translation
                .getTranslation("context_menu.share_folder.notification.accept_label"));
            setAcceptAction(new AbstractAction() {
                private static final long serialVersionUID = 100L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    controller.getIOProvider().startIO(new Runnable() {
                        @Override
                        public void run() {
                            sliderClose();
                            String url = controller.getOSClient()
                                .getFolderURLWithCredentials(foInfo);
                            BrowserLauncher.openURL(controller, url);
                        }
                    });
                }
            });
        }

        // Invite
        setCancelOptionLabel(Translation
            .getTranslation("context_menu.share_folder.notification.cancel_label"));
        setCancelAction(new AbstractAction() {
            private static final long serialVersionUID = 100L;

            @Override
            public void actionPerformed(ActionEvent e) {
                UIUtil.invokeLaterInEDT(new Runnable() {
                    @Override
                    public void run() {
                        sliderClose();
                        PFWizard.openSendInvitationWizard(controller, foInfo);
                        controller.getUIController().getMainFrame().toFront();
                    }
                });
            }
        });
    }
}
