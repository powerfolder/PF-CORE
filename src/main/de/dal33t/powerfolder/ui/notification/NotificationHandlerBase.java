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
 * $Id: NotificationHandlerBase.java 12455 2010-05-27 10:44:35Z harry $
 */
package de.dal33t.powerfolder.ui.notification;

import java.awt.BorderLayout;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JWindow;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.util.Reject;

/**
 * This class handles the display of notification messages.
 */
public abstract class NotificationHandlerBase extends PFComponent {

    private String title;
    private String messageText;
    private String acceptOptionLabel;
    private Action acceptAction;
    private String cancelOptionLabel;
    private Action cancelAction;

    private final JWindow dialog;
    private final Slider slider;

    /**
     * Constructor
     *
     * @param controller
     */
    protected NotificationHandlerBase(Controller controller) {
        super(controller);
        dialog = new JWindow();
        slider = new Slider((JComponent) dialog.getContentPane(),
            PreferencesEntry.NOTIFICATION_DISPLAY.getValueInt(getController()),
            PreferencesEntry.NOTIFICATION_TRANSLUCENT
                .getValueInt(getController()), getController().isNotifyLeft());
    }

    /**
     * Show the message using Slider
     */
    public void show() {
        Reject.ifNull(title, "Title must not be null");
        Reject.ifNull(messageText, "MessageText must not be null");
        NotificationForm notificationForm = new NotificationForm(getController(),
                title, messageText, acceptOptionLabel,
                acceptAction, cancelOptionLabel, cancelAction);
        dialog.getContentPane().add(notificationForm, BorderLayout.CENTER);
        dialog.pack();
        slider.show();
    }

    protected void sliderClose() {
        slider.close();
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    protected void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    protected void setAcceptOptionLabel(String acceptOptionLabel) {
        this.acceptOptionLabel = acceptOptionLabel;
    }

    protected void setAcceptAction(Action acceptAction) {
        this.acceptAction = acceptAction;
    }

    protected void setCancelOptionLabel(String cancelOptionLabel) {
        this.cancelOptionLabel = cancelOptionLabel;
    }

    protected void setCancelAction(Action cancelAction) {
        this.cancelAction = cancelAction;
    }
}