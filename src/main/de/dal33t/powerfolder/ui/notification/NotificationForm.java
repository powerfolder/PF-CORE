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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.border.LineBorder;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.Controller;

/**
 * Class representing the message notification form.
 */
@SuppressWarnings("serial")
public class NotificationForm extends JPanel {

    private final Controller controller;
    private JCheckBox neverShowChatNotificationCB;
    private JCheckBox displayChatMessageCB;
    private JCheckBox neverShowSystemNotificationCB;
    private final String messageText;

    /**
     * Constructor. Displays a panel with title and message. Also accept and
     * optional cancel button.
     * 
     * @param titleText
     * @param messageText
     * @param acceptOptionLabel
     * @param acceptAction
     * @param cancelOptionLabel
     * @param cancelAction
     * @param chat chat, not system
     */
    NotificationForm(Controller controller, String titleText, String messageText,
        String acceptOptionLabel, Action acceptAction,
        String cancelOptionLabel, Action cancelAction, boolean showButtons,
        boolean chat) {
        this.controller = controller;
        this.messageText = messageText.length() > 200 ?
                messageText.substring(0, 200) + "..." : messageText;
        setLayout(new BorderLayout());
        JPanel panel = createPanel(titleText, acceptOptionLabel,
            acceptAction, cancelOptionLabel, cancelAction, showButtons, chat);
        add(panel, BorderLayout.CENTER);
        setBorder(new LineBorder(Color.lightGray, 1));
    }

    /**
     * Create the UI for notification form
     */
    private JPanel createPanel(String titleText,
        String acceptOptionLabel, Action acceptAction,
        String cancelOptionLabel, Action cancelAction, boolean showButtons,
        boolean chat)
    {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        CellConstraints cc = new CellConstraints();

        JButton button = new JButton();
        button.setAction(acceptAction);
        button.setText(acceptOptionLabel);
        FormLayout formLayout;
        int internalWidth;
        if (cancelOptionLabel == null) {
            formLayout = new FormLayout(
                "3dlu, 50dlu, 50dlu:grow, 50dlu, 3dlu",
                    "3dlu, pref, 6dlu, pref, 3dlu, pref, 3dlu, pref, pref, 3dlu, pref, 3dlu");
                    //     head        msg         hr          cb    cb          btn
            internalWidth = 3;
            panel.setLayout(formLayout);
            if (showButtons) {
                panel.add(button, cc.xy(3, 11));
            }
        } else {
            formLayout = new FormLayout(
                "3dlu, 20dlu, 50dlu:grow, 10dlu, 50dlu:grow, 20dlu, 3dlu",
                "3dlu, pref, 6dlu, pref, 3dlu, pref, 3dlu, pref, pref, 3dlu, pref, 3dlu");
                //     head        msg         hr          cb    cb          btn
            internalWidth = 5;
            panel.setLayout(formLayout);
            panel.add(button, cc.xy(3, 11));

            if (showButtons) {
                button = new JButton();
                button.setAction(cancelAction);
                button.setText(cancelOptionLabel);
                panel.add(button, cc.xy(5, 11));
            }
        }

        panel.add(new JSeparator(), cc.xyw(2, 6, internalWidth));

        boolean showChatMessage = (Boolean) controller.getUIController()
                .getApplicationModel().getShowChatMessageValueModel()
                .getValue();

        if (showButtons) {
            if (chat) {
                neverShowChatNotificationCB = new JCheckBox(Translation.getTranslation(
                        "notification_form.never_show_chat_notifications"));
                neverShowChatNotificationCB.addActionListener(new MyActionListener());
                panel.add(neverShowChatNotificationCB, cc.xyw(2, 8, internalWidth));

                displayChatMessageCB = new JCheckBox(Translation.getTranslation(
                        "notification_form.display_chat_messages"));
                displayChatMessageCB.setSelected(showChatMessage);
                displayChatMessageCB.addActionListener(new MyActionListener());
                panel.add(displayChatMessageCB, cc.xyw(2, 9, internalWidth));
            } else {
                neverShowSystemNotificationCB = new JCheckBox(Translation.getTranslation(
                        "notification_form.never_show_system_notifications"));
                neverShowSystemNotificationCB.addActionListener(new MyActionListener());
                panel.add(neverShowSystemNotificationCB, cc.xyw(2, 8, internalWidth));
            }
        }

        panel.add(createHeaderPanel(titleText), cc.xyw(2, 2, internalWidth));

        boolean showMessage = !chat || showChatMessage;
        String message = showMessage ? messageText :
                Translation.getTranslation("chat.notification.message");
        JTextArea textArea = new JTextArea();
        textArea.setText(message);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        panel.add(textArea, new CellConstraints(2, 4, internalWidth, 1,
            CellConstraints.DEFAULT, CellConstraints.TOP));

        return panel;
    }

    /**
     * Create header subpanel with icon and text
     */
    private static JPanel createHeaderPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(null);
        FormLayout formlayout1 = new FormLayout("pref, 3dlu, pref", "pref");
        CellConstraints cc = new CellConstraints();
        panel.setLayout(formlayout1);

        JLabel label = new JLabel();
        label.setText(Translation.getTranslation("notification_form.title",
            title));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(label, cc.xy(3, 1));

        JLabel logo = new JLabel(Icons.getIconById(Icons.SMALL_LOGO));
        logo.setSize(new Dimension(Icons.getIconById(Icons.SMALL_LOGO)
            .getIconWidth(), Icons.getIconById(Icons.SMALL_LOGO)
            .getIconHeight()));
        panel.add(logo, cc.xy(1, 1));

        return panel;
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == neverShowChatNotificationCB) {
                controller.getUIController().getApplicationModel()
                        .getChatNotificationsValueModel().setValue(
                        !neverShowChatNotificationCB.isSelected());
                displayChatMessageCB.setEnabled(!neverShowChatNotificationCB.isSelected());
            } else if (e.getSource() == neverShowSystemNotificationCB) {
                controller.getUIController().getApplicationModel()
                        .getSystemNotificationsValueModel().setValue(
                        !neverShowSystemNotificationCB.isSelected());
            } else if (e.getSource() == displayChatMessageCB) {
                controller.getUIController().getApplicationModel()
                        .getShowChatMessageValueModel().setValue(
                        displayChatMessageCB.isSelected());
            }
        }
    }
}
