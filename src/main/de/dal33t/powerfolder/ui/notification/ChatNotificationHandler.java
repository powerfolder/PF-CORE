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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Translation;

/**
 * This class handles the display of chat notification messages. These are displayed
 * when powerFolder is minimized, giving the user a chance to 'Accept' the
 * message and perform an action.
 */
public class ChatNotificationHandler extends NotificationHandlerBase {

    /**
     * Constructor. Shows a chat message.
     * 
     * @param controller
     * @param title
     * @param messageText
     */
    public ChatNotificationHandler(Controller controller,
                                   final MemberInfo memberInfo,
                                   String title, String messageText) {
        super(controller, true);
        setTitle(title);
        setMessageText(messageText);
        setAcceptOptionLabel(Translation.getTranslation("chat_notification_handler.reply"));
        setAcceptAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                sliderClose();
                getController().getUIController().getMainFrame().getUIComponent()
                        .setVisible(true);
                getController().getUIController().openChat(memberInfo);
            }
        });
        setCancelOptionLabel(Translation.getTranslation("chat_notification_handler.ignore"));
        setCancelAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                sliderClose();
                getController().getUIController().clearBlink();
            }
        });
    }
}
