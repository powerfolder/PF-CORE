package de.dal33t.powerfolder.ui.webservice;

import java.util.Collections;
import java.util.List;

import javax.swing.ListModel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.list.ArrayListModel;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.compare.FolderComparator;
import de.dal33t.powerfolder.util.ui.SwingWorker;
import de.dal33t.powerfolder.webservice.WebServiceClient;

/**
 * UI Model for the webservice client.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class WebServiceClientModel extends PFUIComponent {
    private WebServiceClient client;
    private ArrayListModel mirroredFolders;
    private FolderMembershipListener membershipListener;

    public WebServiceClientModel(Controller controller) {
        super(controller);
        mirroredFolders = new ArrayListModel();
        client = controller.getWebServiceClient();
        initalizeEventhandling();
        updateMirroredFolders();
    }

    public ListModel getMirroredFoldersModel() {
        return mirroredFolders;
    }

    /**
     * @return true if the account data has been set
     */
    public boolean isAccountSet() {
        return !StringUtils.isEmpty(ConfigurationEntry.WEBSERVICE_USERNAME
            .getValue(getController()))
            && !StringUtils.isEmpty(ConfigurationEntry.WEBSERVICE_USERNAME
                .getValue(getController()));
    }

    /**
     * Checks the current webservice account and opens the login wizard if
     * problem occour.
     */
    public void checkAndSetupAccount() {
        if (!isAccountSet()) {
            PFWizard.openLoginWebServiceWizard(getController());
            return;
        }
        SwingWorker worker = new SwingWorker() {
            boolean loginOK;

            @Override
            public Object construct()
            {
                loginOK = getController().getWebServiceClient().checkLogin(
                    ConfigurationEntry.WEBSERVICE_USERNAME
                        .getValue(getController()),
                    ConfigurationEntry.WEBSERVICE_PASSWORD
                        .getValue(getController()));
                return null;
            }

            @Override
            public void finished()
            {
                if (!loginOK) {
                    PFWizard.openLoginWebServiceWizard(getController());
                }
            }
        };
        worker.start();
    }

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
        List<Folder> folders = client.getMirroredFolders();
        Collections.sort(folders, new FolderComparator());
        mirroredFolders.clear();
        mirroredFolders.addAll(folders);
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

        public void unjoinedFolderAdded(FolderRepositoryEvent e) {
        }

        public void unjoinedFolderRemoved(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }

    }

    private class MyFolderMembershipListener implements
        FolderMembershipListener
    {
        public void memberJoined(FolderMembershipEvent folderEvent) {
            if (client.isWebService(folderEvent.getMember())) {
                updateMirroredFolders();
            }
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            if (client.isWebService(folderEvent.getMember())) {
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
            if (client.isWebService(e.getNode())) {
                updateMirroredFolders();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (client.isWebService(e.getNode())) {
                updateMirroredFolders();
            }
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

    }
}
