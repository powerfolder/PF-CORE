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

import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.Controller;

/**
 * Class representing the message notification form.
 */
@SuppressWarnings("serial")
public class NotificationForm extends JPanel {

    private final Controller controller;
    private JCheckBox neverShowSystemNotificationCB;
    private final String messageText;

    /**
     * Constructor. Displays a panel with title and message. Also accept and
     * optional cancel button.
     *
     * @param controller
     * @param titleText
     * @param messageText
     * @param acceptOptionLabel
     * @param acceptAction
     * @param cancelOptionLabel
     * @param cancelAction
     */
    NotificationForm(Controller controller, String titleText, String messageText,
                     String acceptOptionLabel, Action acceptAction,
                     String cancelOptionLabel, Action cancelAction) {
        this.controller = controller;
        // Trim message to 200 chars max.
        this.messageText = messageText.length() > 200 ?
                messageText.substring(0, 200) + "..." : messageText;
        setLayout(new BorderLayout());
        JPanel panel = createPanel(titleText, acceptOptionLabel,
                acceptAction, cancelOptionLabel, cancelAction);
        add(panel, BorderLayout.CENTER);
        setBorder(new LineBorder(Color.lightGray, 1));
    }

    /**
     * Create the UI for notification form
     */
    private JPanel createPanel(String titleText,
                               String acceptOptionLabel, Action acceptAction,
                               String cancelOptionLabel, Action cancelAction) {

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        CellConstraints cc = new CellConstraints();

        FormLayout formLayout;
        int internalWidth;
        if (acceptOptionLabel == null && cancelOptionLabel == null) {
            // No buttons
            formLayout = new FormLayout(
                //     content
                "3dlu, 150dlu:grow, 3dlu",
                    "3dlu, pref, 15dlu, pref, 3dlu");
                    //     head         msg
            internalWidth = 1;
        } else if (acceptOptionLabel != null && cancelOptionLabel != null) {
            // Two buttons
            formLayout = new FormLayout(
                //            button             button
                "3dlu, 10dlu, 80dlu:grow, 10dlu, 80dlu:grow, 10dlu, 3dlu",
                    "3dlu, pref, 15dlu, pref, 15dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");
                    //     head         msg          hr          cb          btn
            internalWidth = 5;
        } else {
            // One button
            formLayout = new FormLayout(
                //            button
                "3dlu, 45dlu, 80dlu:grow, 45dlu, 3dlu",
                    "3dlu, pref, 15dlu, pref, 15dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");
                    //     head         msg          hr          cb          btn
            internalWidth = 3;
        }
        panel.setLayout(formLayout);

        // Heading
        panel.add(createHeaderPanel(titleText), cc.xyw(2, 2, internalWidth));

        // Message
        JTextArea textArea = new JTextArea();
        textArea.setText(messageText);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        panel.add(textArea, new CellConstraints(2, 4, internalWidth, 1,
            CellConstraints.DEFAULT, CellConstraints.TOP));

        // If there are no buttons, don't show anything else.
        if (acceptOptionLabel != null || cancelOptionLabel != null) {

            // Separator
            panel.add(new JSeparator(), cc.xyw(2, 6, internalWidth));

            neverShowSystemNotificationCB = new JCheckBox(Translation.getTranslation(
                    "notification_form.never_show_system_notifications"));
            neverShowSystemNotificationCB.addActionListener(new MyActionListener());
            panel.add(neverShowSystemNotificationCB, cc.xyw(2, 8, internalWidth));

            // Buttons
            if (acceptOptionLabel != null && cancelOptionLabel != null) {
                // Two buttons
                JButton acceptButton = new JButton();
                acceptButton.setAction(acceptAction);
                acceptButton.setText(acceptOptionLabel);
                panel.add(acceptButton, cc.xy(3, 10));

                JButton cancelButton = new JButton();
                cancelButton.setAction(cancelAction);
                cancelButton.setText(cancelOptionLabel);
                panel.add(cancelButton, cc.xy(5, 10));
            } else {
                // Single button (accept or cancel)
                if (acceptOptionLabel != null) {
                    // Accept
                    JButton acceptButton = new JButton();
                    acceptButton.setAction(acceptAction);
                    acceptButton.setText(acceptOptionLabel);
                    panel.add(acceptButton, cc.xy(3, 10));
                } else {
                    // Cancel
                    JButton cancelButton = new JButton();
                    cancelButton.setAction(cancelAction);
                    cancelButton.setText(cancelOptionLabel);
                    panel.add(cancelButton, cc.xy(3, 10));
                }
            }
        }

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
            if (e.getSource() == neverShowSystemNotificationCB) {
                controller.getUIController().getApplicationModel()
                        .getSystemNotificationsValueModel().setValue(
                        !neverShowSystemNotificationCB.isSelected());
            }
        }
    }
}
