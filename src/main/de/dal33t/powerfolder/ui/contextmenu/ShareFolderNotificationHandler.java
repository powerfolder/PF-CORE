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
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.notification.NotificationHandlerBase;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.BrowserLauncher;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class ContextMenuNotificationHandler extends NotificationHandlerBase {

    protected ContextMenuNotificationHandler(final Controller controller,
        final FolderInfo foInfo, String title, String message,
        String acceptLabel, String cancelLabel)
    {
        super(controller);

        setTitle(title);
        setMessageText(message);

        setAcceptOptionLabel(acceptLabel);
        setCancelOptionLabel(cancelLabel);

        // Show in Web
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

        // Invite
        setCancelAction(new AbstractAction() {
            private static final long serialVersionUID = 100L;

            @Override
            public void actionPerformed(ActionEvent e) {
                UIUtil.invokeLaterInEDT(new Runnable() {
                    @Override
                    public void run() {
                        sliderClose();
                        PFWizard.openSendInvitationWizard(controller, foInfo);
                    }
                });
            }
        });
    }
}
