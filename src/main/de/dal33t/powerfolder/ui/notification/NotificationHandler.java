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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.MainFrame;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.TimerTask;

/**
 * This class handles the display of notification messages.
 * These are displayed when powerFolder is minimized,
 * giving the user a chance to 'Accept' the message and perform an action.
 */
public class NotificationHandler extends PFComponent {

    /** The notification title */
    private final String title;

    /** The notification message */
    private final String message;

    /** The task to perform if the notification is accepted */
    private final TimerTask task;

    /** The label for the Accept button */
    private final String acceptOptionLabel;

    /** The optional label for the cancel button */
    private final String cancelOptionLabel;

    /**
     * Constructor. Shows a message with an okay button.
     *
     * @param controller
     * @param title
     * @param message
     */
    public NotificationHandler(Controller controller, String title,
                               String message) {
        super(controller);
        Reject.ifNull(title, "Title must not be null");
        Reject.ifNull(message, "Message must not be null");
        this.title = title;
        this.message = message;
        task = null;
        acceptOptionLabel = Translation.getTranslation("general.ok");
        cancelOptionLabel = null;
    }

    /**
     * Constructor. Shows a message with accept and cancel buttons.
     * If the accept button is clicked, the task runs.
     *
     * @param controller
     * @param title
     * @param message
     * @param task
     */
    public NotificationHandler(Controller controller, String title,
                               String message, TimerTask task) {
        super(controller);
        Reject.ifNull(title, "Title must not be null");
        Reject.ifNull(message, "Message must not be null");
        Reject.ifNull(task, "Task must not be null");
        this.title = title;
        this.message = message;
        this.task = task;
        acceptOptionLabel = Translation.getTranslation("notification_handler.display.text");
        cancelOptionLabel = Translation.getTranslation("notification_handler.ignore.text");
    }

    /**
     * Show the message using Slider
     *
     * @param message
     */
    public void show() {
        JWindow dialog = new JWindow();
        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        final Slider slider = new Slider((JComponent) contentPane);

        Action acceptAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                slider.close();

                // If task exists, deiconify and run.
                if (task != null) {
                    MainFrame mainFrame = getController().getUIController()
                            .getMainFrame();
                    if (mainFrame.isIconifiedOrHidden()) {
                        mainFrame.deiconify();
                    }
                    task.run();
                }
            }
        };

        Action cancelAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                slider.close();
            }
        };

        // Show it.
        NotificationForm notificationForm = new NotificationForm(
                title, message, acceptOptionLabel, acceptAction,
                cancelOptionLabel, cancelAction);
        contentPane.add(notificationForm, BorderLayout.CENTER);
        dialog.pack();
        slider.show();
    }

}
