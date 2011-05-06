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
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.Controller;

/**
 * Class representing the message notification form.
 */
@SuppressWarnings("serial")
public class NotificationForm extends JPanel {

    private final Controller controller;
    private JCheckBox neverShowChatCB;
    private JCheckBox neverShowSystemCB;

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
     */
    NotificationForm(Controller controller, String titleText, String messageText,
        String acceptOptionLabel, Action acceptAction,
        String cancelOptionLabel, Action cancelAction, boolean showButtons,
        boolean chat, boolean system) {
        this.controller = controller;
        setLayout(new BorderLayout());
        JPanel panel = createPanel(titleText, messageText, acceptOptionLabel,
            acceptAction, cancelOptionLabel, cancelAction, showButtons, chat,
                system);
        add(panel, BorderLayout.CENTER);
        setBorder(new LineBorder(Color.lightGray, 1));
    }

    /**
     * Create the UI for notification form
     */
    private JPanel createPanel(String titleText, String msgText,
        String acceptOptionLabel, Action acceptAction,
        String cancelOptionLabel, Action cancelAction, boolean showButtons,
        boolean showNeverAskForChat, boolean showNeverAskForSystem)
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
                "3dlu, pref, 6dlu, pref, 6dlu, pref, 3dlu, pref, 3dlu");
            internalWidth = 3;
            panel.setLayout(formLayout);
            if (showButtons) {
                panel.add(button, cc.xy(3, 8));
            }
        } else {
            formLayout = new FormLayout(
                "3dlu, 20dlu, 50dlu:grow, 10dlu, 50dlu:grow, 20dlu, 3dlu",
                "3dlu, pref, 6dlu, pref, 6dlu, pref, 3dlu, pref, 3dlu");
            internalWidth = 5;
            panel.setLayout(formLayout);
            panel.add(button, cc.xy(3, 8));

            if (showButtons) {
                button = new JButton();
                button.setAction(cancelAction);
                button.setText(cancelOptionLabel);
                panel.add(button, cc.xy(5, 8));
            }
        }

        if (showButtons) {
            if (showNeverAskForChat) {
                neverShowChatCB = new JCheckBox(Translation.getTranslation(
                        "notification_form.never_show_chat_notifications"));
                neverShowChatCB.addActionListener(new MyActionListener());
                panel.add(neverShowChatCB, cc.xywh(2, 6, internalWidth, 1));
            } else if (showNeverAskForSystem)  {
                neverShowSystemCB = new JCheckBox(Translation.getTranslation(
                        "notification_form.never_show_system_notifications"));
                neverShowSystemCB.addActionListener(new MyActionListener());
                panel.add(neverShowSystemCB, cc.xywh(2, 6, internalWidth, 1));
            }
        }

        panel.add(createHeaderPanel(titleText), cc.xywh(2, 2, internalWidth, 1));

        JTextArea textArea = new JTextArea(msgText);
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
            if (e.getSource() == neverShowChatCB) {
                controller.getUIController().getApplicationModel()
                        .getChatNotificationsValueModel().setValue(
                        !neverShowChatCB.isSelected());
            } else if(e.getSource() == neverShowSystemCB) {
                controller.getUIController().getApplicationModel()
                        .getSystemNotificationsValueModel().setValue(
                        !neverShowSystemCB.isSelected());
            }
        }
    }
}
