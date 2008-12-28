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
package de.dal33t.powerfolder.ui.webservice;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.list.ArrayListModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.ui.SwingWorker;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * UI Model for the Online Storage client.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ServerClientModel extends PFUIComponent {
    private ServerClient client;
    private ArrayListModel<Folder> mirroredFolders;
    private PresentationModel<Account> accountModel;
    private FolderMembershipListener membershipListener;

    public ServerClientModel(Controller controller, ServerClient client) {
        super(controller);

        Reject.ifNull(client, "Client is null");
        mirroredFolders = new ArrayListModel<Folder>();
        this.client = client;
        this.accountModel = new PresentationModel<Account>(client.getAccount());
        initalizeEventhandling();
        updateMirroredFolders();
    }

    public ServerClient getClient() {
        return client;
    }

    public ListModel getMirroredFoldersModel() {
        return mirroredFolders;
    }

    public PresentationModel<Account> getAccountModel() {
        return accountModel;
    }

    /**
     * Checks the current webservice account and opens the login wizard if
     * problem occour.
     */
    public void checkAndSetupAccount() {

        // Don't do account if lan only mode.
        if (getController().isLanOnly()) {
            return;
        }

        if (client.isLastLoginOK()) {
            return;
        }

        if (!client.isLastLoginKnown()) {
            PFWizard.openLoginWebServiceWizard(getController(), client);
            return;
        }

        SwingWorker worker = new SwingWorker() {
            @Override
            public Object construct() {
                return client.loginWithLastKnown().isValid();
            }

            @Override
            public void finished() {
                if (get() == null || !(Boolean) get()) {
                    PFWizard.openLoginWebServiceWizard(getController(), client);
                }
            }
        };
        worker.start();
    }

    // Internal methods *******************************************************

    private void initalizeEventhandling() {
        client.addListener(new MyServerClientListener());
        membershipListener = new MyFolderMembershipListener();

        // Setup folder membership stuff
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            folder.addMembershipListener(membershipListener);
        }
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
    }

    private void updateMirroredFolders() {
        List<Folder> folders = client.getJoinedFolders();
        Collections.sort(folders, FolderComparator.INSTANCE);
        mirroredFolders.clear();
        mirroredFolders.addAll(folders);
    }

//    private void updateNavTree() {
//        // Ugly
//        RootNode rootNode = getUIController().getApplicationModel()
//            .getNavTreeModel().getRootNode();
//        TreeModelEvent te = new TreeModelEvent(this, new Object[]{rootNode,
//            rootNode.WEBSERVICE_NODE});
//        getUIController().getApplicationModel().getNavTreeModel()
//            .fireTreeNodesChangedEvent(te);
//    }

    // Core listener **********************************************************

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

        public void folderCreated(FolderRepositoryEvent e) {
            e.getFolder().addMembershipListener(membershipListener);
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            e.getFolder().removeMembershipListener(membershipListener);
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

    }

    private class MyFolderMembershipListener implements
        FolderMembershipListener
    {
        public void memberJoined(FolderMembershipEvent folderEvent) {
            if (client.isServer(folderEvent.getMember())) {
                updateMirroredFolders();
            }
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            if (client.isServer(folderEvent.getMember())) {
                updateMirroredFolders();
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    private class MyServerClientListener implements ServerClientListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void accountUpdated(ServerClientEvent event) {
            accountModel.setBean(event.getAccountDetails().getAccount());
            updateMirroredFolders();
        }

        public void login(ServerClientEvent event) {
            accountModel.setBean(event.getAccountDetails().getAccount());
            updateMirroredFolders();
        }

        public void serverConnected(ServerClientEvent event) {
            updateMirroredFolders();
//            updateNavTree();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateMirroredFolders();
//            updateNavTree();
        }
    }
}
