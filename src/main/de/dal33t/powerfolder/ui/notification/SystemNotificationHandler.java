/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 *
 * $Id: NotificationHandler.java 12455 2010-05-27 10:44:35Z harry $
 */
package de.dal33t.powerfolder.ui.notification;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * This class handles the display of system notification messages. These are displayed
 * when powerFolder is minimized, giving the user a chance to 'Accept' the
 * message and perform an action.
 */
public class SystemNotificationHandler extends NotificationHandlerBase {

    /**
     * Constructor. Shows a system message.
     *
     * @param controller
     * @param notice
     */
    public SystemNotificationHandler(Controller controller,
                                     final Notice notice) {
        super(controller);
        Reject.ifNull(notice, "Notice must not be null");
        setTitle(notice.getTitle());
        setMessageText(notice.getSummary());
        if (notice.isActionable()) {
            setAcceptAction(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    sliderClose();
                    getController().getUIController().getApplicationModel()
                        .getNoticesModel().activateNotice(notice);
                }
            });
            setAcceptOptionLabel(Translation
                .getTranslation("notification_handler.display.text"));
            setCancelOptionLabel(Translation
                .getTranslation("notification_handler.ignore.text"));
            setCancelAction(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    sliderClose();
                    getController().getUIController().getApplicationModel()
                        .getNoticesModel().markRead(notice);
                }
            });
        } else {
            setAcceptOptionLabel(Translation.getTranslation("general.ok"));
            setAcceptAction(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    sliderClose();
                }
            });
        }
    }
}
