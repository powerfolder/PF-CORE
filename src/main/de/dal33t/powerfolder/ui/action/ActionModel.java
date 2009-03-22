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

/**
 * Holder of all simple singleton actions.
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
    private OpenDownloadsInformationAction openDownloadsInformationAction;
    private OpenUploadsInformationAction openUploadsInformationAction;
    private OpenInvitationReceivedWizardAction openInvitationReceivedWizardAction;
    private ActivateWarningAction activateWarningAction;
    private AskForFriendshipAction askForFriendshipAction;
    private OpenDebugInformationAction openDebugInformationAction;
    private SingleFileTransferOfferAction singleFileTransferOfferAction;

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

    public OpenDownloadsInformationAction getOpenDownloadsInformationAction() {
        if (openDownloadsInformationAction == null) {
            openDownloadsInformationAction = new OpenDownloadsInformationAction(getController());
        }
        return openDownloadsInformationAction;
    }

    public OpenUploadsInformationAction getOpenUploadsInformationAction() {
        if (openUploadsInformationAction == null) {
            openUploadsInformationAction = new OpenUploadsInformationAction(getController());
        }
        return openUploadsInformationAction;
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

    public OpenInvitationReceivedWizardAction
    getOpenInvitationReceivedWizardAction() {

        if (openInvitationReceivedWizardAction == null) {
            openInvitationReceivedWizardAction =
                    new OpenInvitationReceivedWizardAction(getController());
        }
        return openInvitationReceivedWizardAction;
    }

    public AskForFriendshipAction getAskForFriendshipAction() {
        if (askForFriendshipAction == null) {
            askForFriendshipAction = new AskForFriendshipAction(getController());
        }
        return askForFriendshipAction;
    }

    public ActivateWarningAction getActivateWarningAction() {
        if (activateWarningAction == null) {
            activateWarningAction = new ActivateWarningAction(getController());
        }
        return activateWarningAction;
    }

    public OpenDebugInformationAction getOpenDebugInformationAction() {
        if (openDebugInformationAction == null) {
        	openDebugInformationAction = new OpenDebugInformationAction(getController());
        }
        return openDebugInformationAction;
    }

    public SingleFileTransferOfferAction getSingleFileTransferOfferAction() {
        if (singleFileTransferOfferAction == null) {
        	singleFileTransferOfferAction = new SingleFileTransferOfferAction(getController());
        }
        return singleFileTransferOfferAction;
    }
}
