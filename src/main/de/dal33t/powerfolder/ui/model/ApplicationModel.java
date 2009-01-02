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
package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.action.ActionModel;
import de.dal33t.powerfolder.ui.chat.ChatModel;
import de.dal33t.powerfolder.ui.webservice.ServerClientModel;

/**
 * Contains all core models for the application.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ApplicationModel extends PFUIComponent {

    private ActionModel actionModel;
    private ChatModel chatModel;
    private NodeManagerModel nodeManagerModel;
    private FolderRepositoryModel folderRepositoryModel;
    private TransferManagerModel transferManagerModel;
    private ServerClientModel serverClientModel;
    private ReceivedInvitationsModel receivedInvitationsModel;
    private ReceivedAskedForFriendshipModel receivedAskedForFriendshipModel;

    /**
     * Constructs a non-initialized application model. Before the model can used
     * call {@link #initialize()}
     * 
     * @param controller
     * @see #initialize()
     */
    public ApplicationModel(Controller controller) {
        super(controller);
        actionModel = new ActionModel(getController());
        chatModel = new ChatModel(getController());
        nodeManagerModel = new NodeManagerModel(getController());
        folderRepositoryModel = new FolderRepositoryModel(getController());
        transferManagerModel = new TransferManagerModel(getController()
            .getTransferManager());
        serverClientModel = new ServerClientModel(getController(),
            getController().getOSClient());
        receivedInvitationsModel = new ReceivedInvitationsModel(getController());
        receivedAskedForFriendshipModel =
                new ReceivedAskedForFriendshipModel(getController());
    }

    /**
     * Initializes this and all submodels
     */
    public void initialize() {
        folderRepositoryModel.initialize();
        transferManagerModel.initialize();
    }

    // Exposing ***************************************************************

    public ActionModel getActionModel() {
        return actionModel;
    }

    public ChatModel getChatModel() {
        return chatModel;
    }

    public NodeManagerModel getNodeManagerModel() {
        return nodeManagerModel;
    }

    public FolderRepositoryModel getFolderRepositoryModel() {
        return folderRepositoryModel;
    }

    public TransferManagerModel getTransferManagerModel() {
        return transferManagerModel;
    }

    public ServerClientModel getServerClientModel() {
        return serverClientModel;
    }

    public ReceivedInvitationsModel getReceivedInvitationModel() {
        return receivedInvitationsModel;
    }

    public ReceivedAskedForFriendshipModel getReceivedAskedForFriendshipModel() {
        return receivedAskedForFriendshipModel;
    }
}
