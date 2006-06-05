/* $Id: RootQuickInfoPanel.java,v 1.5 2006/04/14 14:52:32 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.home;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * The panel the contains the most important and concentrated information
 * about the current powerfolder status
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RootQuickInfoPanel extends QuickInfoPanel {
    private JComponent picto;
    private JComponent headerText;
    private JLabel infoText1;
    private JLabel infoText2;

    protected RootQuickInfoPanel(Controller controller) {
        super(controller);
    }

    /**
     * Initalizes the components
     * 
     * @return
     */
    @Override
    protected void initComponents()
    {
        headerText = SimpleComponentFactory.createBiggerTextLabel(Translation
            .getTranslation("quickinfo.root.title"));

        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");

        picto = new JLabel(Icons.LOGO96X96);

        updateText();
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getController().getTransferManager().addListener(new MyTransferManagerListener());
        getController().getNodeManager().addNodeManagerListener(new MyNodeManagerListener());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        String info1 = getController().getFolderRepository()
            .isAnyFolderSyncing() ? Translation
            .getTranslation("quickinfo.root.syncing") : Translation
            .getTranslation("quickinfo.root.insync");
        infoText1.setText(info1);

        int nCompletedDls = getController().getTransferManager()
            .countCompletedDownloads();
        String info2 = Translation.getTranslation("quickinfo.root.downloads",
            nCompletedDls);

        boolean online = getController().getNodeManager().countConnectedNodes() > 0;
        int nOnlineFriends = getController().getNodeManager()
            .countOnlineFriends();
        String friendsText = online ? Translation.getTranslation(
            "quickinfo.root.friends", nOnlineFriends) : Translation
            .getTranslation("quickinfo.root.offline");
        // FIXME: Correct i18n for right to left reading languages
        info2 += ", " + friendsText;

        infoText2.setText(info2);
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
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class MyNodeManagerListener implements NodeManagerListener {
        public void nodeRemoved(NodeManagerEvent e) {
            updateText();
        }

        public void nodeAdded(NodeManagerEvent e) {
            updateText();
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateText();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateText();
        }

        public void friendAdded(NodeManagerEvent e) {
            updateText();
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateText();
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateText();
        }
    }
    
    private class MyTransferManagerListener implements TransferManagerListener {

        public void downloadRequested(TransferManagerEvent event) {
            updateText();
            
        }

        public void downloadQueued(TransferManagerEvent event) {
            updateText();
            
        }

        public void downloadStarted(TransferManagerEvent event) {
            updateText();
            
        }

        public void downloadAborted(TransferManagerEvent event) {
            updateText();
            
        }

        public void downloadBroken(TransferManagerEvent event) {
            updateText();
            
        }

        public void downloadCompleted(TransferManagerEvent event) {
            updateText();
            
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            updateText();
            
        }

        public void pendingDownloadEnqueud(TransferManagerEvent event) {
            updateText();
            
        }

        public void uploadRequested(TransferManagerEvent event) {
            updateText();
            
        }

        public void uploadStarted(TransferManagerEvent event) {
            updateText();
        }

        public void uploadAborted(TransferManagerEvent event) {
            updateText();
            
        }

        public void uploadBroken(TransferManagerEvent event) {
            updateText();
            
        }

        public void uploadCompleted(TransferManagerEvent event) {
            updateText();
        }
        
    }
}
