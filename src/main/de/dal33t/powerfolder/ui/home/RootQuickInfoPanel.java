/* $Id: RootQuickInfoPanel.java,v 1.5 2006/04/14 14:52:32 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.home;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * The panel the contains the most important and concentrated information about
 * the current powerfolder status
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RootQuickInfoPanel extends QuickInfoPanel {
    private JComponent picto;
    private JComponent headerText;
    private JLabel infoText1;
    private JLabel infoText2;

    // caching text that need no update
    private String syncText;
    private String comletedDownloadText;
    private String friendText;

    protected RootQuickInfoPanel(Controller controller) {
        super(controller);
    }

    /**
     * Initalizes the components
     */
    @Override
    protected void initComponents()
    {
        headerText = SimpleComponentFactory.createBiggerTextLabel(Translation
            .getTranslation("quickinfo.root.title"));

        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");

        picto = new JLabel(Icons.LOGO96X96);
        updateAllText();
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    /**
     * Updates the info fields
     */
    private void updateAllText() {
        infoText1.setText(getSyncText(true));
        infoText2.setText(getComletedDownloadText(true) + ", "
            + getFriendText(true));
    }

    /**
     * Updates the info fields, refresh node only
     */
    private void updateNodesText() {
        infoText1.setText(getSyncText(false));
        infoText2.setText(getComletedDownloadText(false) + ", "
            + getFriendText(true));
    }

    /**
     * Updates the info fields, refresh node only
     */
    private void updateSyncText() {
        infoText1.setText(getSyncText(true));
        infoText2.setText(getComletedDownloadText(true) + ", "
            + getFriendText(false));
    }

    private String getSyncText(boolean refresh) {
        if (refresh) {
            syncText = getController().getFolderRepository()
                .isAnyFolderSyncing() ? Translation
                .getTranslation("quickinfo.root.syncing") : Translation
                .getTranslation("quickinfo.root.insync");
        }
        return syncText;
    }

    private String getComletedDownloadText(boolean refresh) {
        if (refresh) {
            int nCompletedDls = getController().getTransferManager()
                .countCompletedDownloads();
            comletedDownloadText = Translation.getTranslation(
                "quickinfo.root.downloads", "" + nCompletedDls);
        }
        return comletedDownloadText;
    }

    private String getFriendText(boolean refresh) {
        if (refresh) {
            boolean online = getController().getNodeManager()
                .countConnectedNodes() > 0;
            int nOnlineFriends = getController().getNodeManager()
                .countOnlineFriends();
            friendText = online ? Translation.getTranslation(
                "quickinfo.root.friends", "" + nOnlineFriends) : Translation
                .getTranslation("quickinfo.root.offline");
        }
        return friendText;
    }

    // Implementing stuff *****************************************************

    @Override
    protected JComponent getPicto()
    {
        return picto;
    }

    @Override
    protected JComponent getHeaderText()
    {
        return headerText;
    }

    @Override
    protected JComponent getInfoText1()
    {
        return infoText1;
    }

    @Override
    protected JComponent getInfoText2()
    {
        return infoText2;
    }

    // Core listeners *********************************************************

    /**
     * Listens for changes from the nodemanger
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class MyNodeManagerListener implements NodeManagerListener {
        public void nodeRemoved(NodeManagerEvent e) {
            // updateNodesText();
        }

        public void nodeAdded(NodeManagerEvent e) {
            // updateNodesText();
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateNodesText();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateNodesText();
        }

        public void friendAdded(NodeManagerEvent e) {
            updateNodesText();
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateNodesText();
        }

        public void settingsChanged(NodeManagerEvent e) {
            // updateNodesText();
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    private class MyTransferManagerListener implements TransferManagerListener {

        public void downloadRequested(TransferManagerEvent event) {
            updateSyncText();
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateSyncText();
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateSyncText();
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateSyncText();
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateSyncText();
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateSyncText();
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateSyncText();
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateSyncText();
        }


        public void uploadAborted(TransferManagerEvent event) {
            updateSyncText();
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateSyncText();
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateSyncText();
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateSyncText();
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateSyncText();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

    }
}
