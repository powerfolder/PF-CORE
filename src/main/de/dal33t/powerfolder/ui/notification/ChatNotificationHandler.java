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
 * $Id$
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
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * This class handles the display of chat notification messages. These are displayed
 * when powerFolder is minimized, giving the user a chance to 'Accept' the
 * message and perform an action.
 */
public class ChatNotificationHandler extends PFComponent {

    /** The notification title */
    private final String title;

    /** The notification message */
    private final String message;

    /** The label for the Accept button */
    private final String acceptOptionLabel;

    /** The optional label for the cancel button */
    private final String cancelOptionLabel;

    private final boolean showButtons;

    private final MemberInfo memberInfo;

    /**
     * Constructor. Shows a message with an okay button.
     * 
     * @param controller
     * @param title
     * @param message
     * @param showButtons
     *          show the Accept or Cancel button
     */
    public ChatNotificationHandler(Controller controller, MemberInfo memberInfo,
                                   String title, String message,
                                   boolean showButtons)
    {
        super(controller);
        Reject.ifNull(title, "Title must not be null");
        Reject.ifNull(message, "Message must not be null");
        this.title = title;
        this.memberInfo = memberInfo;
        this.message = message;
        acceptOptionLabel = Translation.getTranslation("chat_notification_handler.reply");
        cancelOptionLabel = Translation.getTranslation("chat_notification_handler.ignore");
        this.showButtons = showButtons;
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
                getController().getUIController().getMainFrame().getUIComponent()
                        .setVisible(true);
                getController().getUIController().openChat(memberInfo);
            }
        };

        Action cancelAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                slider.close();
                getController().getUIController().clearBlink();
            }
        };

        // Show it.
        NotificationForm notificationForm = new NotificationForm(getController(),
                title, message, acceptOptionLabel, acceptAction,
                cancelOptionLabel, cancelAction, showButtons, true);
        contentPane.add(notificationForm, BorderLayout.CENTER);
        dialog.pack();
        slider.show();
    }

}
