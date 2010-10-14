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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JWindow;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * This class handles the display of notification messages. These are displayed
 * when powerFolder is minimized, giving the user a chance to 'Accept' the
 * message and perform an action.
 */
public class NoticeHandler extends PFComponent {

    private final Notice notice;

    /**
     * Constructor. Shows a message with an okay button.
     * 
     * @param controller
     * @param notice
     */
    public NoticeHandler(Controller controller, Notice notice) {
        super(controller);
        Reject.ifNull(notice, "Notice must not be null");
        this.notice = notice;
    }

    /**
     * Show the message using Slider
     */
    @SuppressWarnings("serial")
    public void show() {
        JWindow dialog = new JWindow();
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        final Slider slider = new Slider((JComponent) contentPane,
            PreferencesEntry.NOTIFICATION_DISPLAY.getValueInt(getController()),
            PreferencesEntry.NOTIFICATION_TRANSLUCENT
                .getValueInt(getController()), getController().isNotifyLeft());

        Action acceptAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                slider.close();
                getController().getUIController().getApplicationModel()
                    .getNoticesModel().activateNotice(notice);
            }
        };

        Action cancelAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                slider.close();
            }
        };

        String acceptOptionLabel;
        String cancelOptionLabel;

        if (notice.isActionable()) {
            acceptOptionLabel = Translation
                .getTranslation("notification_handler.display.text");
            cancelOptionLabel = Translation
                .getTranslation("notification_handler.ignore.text");
        } else {
            acceptOptionLabel = Translation.getTranslation("general.ok");
            cancelOptionLabel = null;
        }

        // Show it.
        NotificationForm notificationForm = new NotificationForm(notice
            .getTitle(), notice.getSummary(), acceptOptionLabel, acceptAction,
            cancelOptionLabel, cancelAction, true);
        contentPane.add(notificationForm, BorderLayout.CENTER);
        dialog.pack();
        slider.show();
    }

}
