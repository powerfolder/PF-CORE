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
* $Id: OpenSettingsInformationAction.java 5514 2008-10-25 15:23:59Z harry $
*/
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;

import java.awt.event.ActionEvent;

/**
 * Creates an Action that displays a chat frame for a member.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 */
public class OpenChatAction extends BaseAction {

    public OpenChatAction(Controller controller) {
        super("action_open_chat", controller);
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source instanceof MemberInfo) {
            MemberInfo memberInfo = (MemberInfo) source;
            getController().getUIController().openChat(memberInfo);
        }
    }
}