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
* $Id: ActionModel.java 5419 2008-09-29 12:18:20Z harry $
*/
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.actionold.ReconnectAction;

/**
 * Holder of all singleton actions.
 */
public class ActionModel extends PFComponent {

    /**
     * Constructor.
     */
    public ActionModel(Controller controller) {
        super(controller);
    }

    private NewFolderAction newFolderAction;
    private FindComputersAction findComputersAction;
    private OpenPreferencesAction openPreferencesAction;
    private OpenAboutBoxAction openAboutBoxAction;
    private ConnectAction connectAction;
    private OpenChatAction openChatAction;
    private OpenSettingsInformationAction openSettingsInformationAction;
    private OpenFilesInformationAction openFilesInformationAction;
    private OpenMembersInformationAction openMembersInformationAction;
    private ReconnectAction reconnectAction;
    private AddFriendAction addFriendAction;
    private RemoveFriendAction removeFriendAction;

    public NewFolderAction getNewFolderAction() {
        if (newFolderAction == null) {
            newFolderAction = new NewFolderAction(getController());
        }
        return newFolderAction;
    }

    public FindComputersAction getFindComputersAction() {
        if (findComputersAction == null) {
            findComputersAction = new FindComputersAction(getController());
        }
        return findComputersAction;
    }

    public OpenPreferencesAction getOpenPreferencesAction() {
        if (openPreferencesAction == null) {
            openPreferencesAction = new OpenPreferencesAction(getController());
        }
        return openPreferencesAction;
    }

    public OpenAboutBoxAction getOpenAboutBoxAction() {
        if (openAboutBoxAction == null) {
            openAboutBoxAction = new OpenAboutBoxAction(getController());
        }
        return openAboutBoxAction;
    }

    public ConnectAction getConnectAction() {
        if (connectAction == null) {
            connectAction = new ConnectAction(getController());
        }
        return connectAction;
    }

    public OpenSettingsInformationAction getOpenSettingsInformationAction() {
        if (openSettingsInformationAction == null) {
            openSettingsInformationAction = new OpenSettingsInformationAction(getController());
        }
        return openSettingsInformationAction;
    }

    public OpenFilesInformationAction getOpenFilesInformationAction() {
        if (openFilesInformationAction == null) {
            openFilesInformationAction = new OpenFilesInformationAction(getController());
        }
        return openFilesInformationAction;
    }

    public OpenMembersInformationAction getOpenMembersInformationAction() {
        if (openMembersInformationAction == null) {
            openMembersInformationAction = new OpenMembersInformationAction(getController());
        }
        return openMembersInformationAction;
    }

    public OpenChatAction getOpenChatAction() {
        if (openChatAction == null) {
            openChatAction = new OpenChatAction(getController());
        }
        return openChatAction;
    }

    public ReconnectAction getReconnectAction() {
        if (reconnectAction == null) {
            reconnectAction = new ReconnectAction(getController());
        }
        return reconnectAction;
    }

    public AddFriendAction getAddFriendAction() {
        if (addFriendAction == null) {
            addFriendAction = new AddFriendAction(getController());
        }
        return addFriendAction;
    }

    public RemoveFriendAction getRemoveFriendAction() {
        if (removeFriendAction == null) {
            removeFriendAction = new RemoveFriendAction(getController());
        }
        return removeFriendAction;
    }
}
