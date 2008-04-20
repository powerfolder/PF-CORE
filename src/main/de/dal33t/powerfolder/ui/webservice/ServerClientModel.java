package de.dal33t.powerfolder.ui.webservice;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.ListModel;

import com.jgoodies.binding.list.ArrayListModel;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.ui.SwingWorker;

/**
 * UI Model for the Online Storage client.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ServerClientModel extends PFUIComponent {
    private ServerClient client;
    private ArrayListModel mirroredFolders;
    private FolderMembershipListener membershipListener;

    public ServerClientModel(Controller controller, ServerClient client)
    {
        super(controller);
        Reject.ifNull(client, "Client is null");
        mirroredFolders = new ArrayListModel();
        this.client = client;
        initalizeEventhandling();
        updateMirroredFolders();
    }

    public ListModel getMirroredFoldersModel() {
        return mirroredFolders;
    }

    /**
     * Checks the current webservice account and opens the login wizard if
     * problem occour.
     */
    public void checkAndSetupAccount() {
        checkAndSetupAccount(false);
    }

    /**
     * Checks the current webservice account and opens the login wizard if
     * problem occour.
     * 
     * @param folderSetupAfterwards
     *            true if folder setup should shown after correct login
     */
    public void checkAndSetupAccount(final boolean folderSetupAfterwards) {

        // Don't do account if lan only mode.
        if (getController().isLanOnly()) {
            return;
        }

        if (!client.isAccountSet()) {
            PFWizard.openLoginWebServiceWizard(getController(),
                folderSetupAfterwards);
            return;
        }
        SwingWorker worker = new SwingWorker() {
            boolean loginOK;

            @Override
            public Object construct() {
                loginOK = client.login(
                    ConfigurationEntry.WEBSERVICE_USERNAME
                        .getValue(getController()),
                    ConfigurationEntry.WEBSERVICE_PASSWORD
                        .getValue(getController())).isLoginOK();
                return null;
            }

            @Override
            public void finished() {
                if (!loginOK) {
                    PFWizard.openLoginWebServiceWizard(getController(),
                        folderSetupAfterwards);
                }
            }
        };
        worker.start();
    }

    public Action getMirrorFolderAction() {
        return new MirrorFolderAction(getController());
    }

    // Internal methods *******************************************************

    private void initalizeEventhandling() {
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
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
        Collections.sort(folders, new FolderComparator());
        mirroredFolders.clear();
        mirroredFolders.addAll(folders);
    }

    // Actions ****************************************************************

    private class MirrorFolderAction extends BaseAction {

        protected MirrorFolderAction(Controller controller) {
            super("mirrorfolder", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (client.isAccountSet()) {
                PFWizard.openMirrorFolderWizard(getController());
            } else {
                PFWizard.openLoginWebServiceWizard(getController(), true);
            }
        }
    }

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

        public boolean fireInEventDispathThread() {
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

        public boolean fireInEventDispathThread() {
            return true;
        }

    }

    private class MyNodeManagerListener implements NodeManagerListener {

        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            if (client.isServer(e.getNode())) {
                updateMirroredFolders();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (client.isServer(e.getNode())) {
                updateMirroredFolders();
            }
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

    }
}
