/* $Id: FolderQuickInfoPanel.java,v 1.3 2006/04/06 13:48:05 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.folder;

import javax.swing.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;

import java.awt.*;

/**
 * Show concentrated information about the whole folder repository
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class FolderQuickInfoPanel extends PFUIComponent {

    /** Reduce sync perc to be the same size as the folder picto. */
    private static final double SCALE_FACTOR = 0.8;

    private JPanel panel;
    private JComponent picto;
    private JLabel headerText;
    private JLabel infoText1;
    private JLabel infoText2;
    private Folder currentFolder;
    private MyFolderListener myFolderListener;
    private JLabel syncStatusPicto;

    protected FolderQuickInfoPanel(Controller controller) {
        super(controller);
        myFolderListener = new MyFolderListener();
    }

    /**
     * Create the top part of the panel which contains the most concentrated
     * informations
     *
     * @return the component.
     */
    public JComponent getUIComponent() {
        if (panel == null) {
            // Init components
            initComponents();

            // Build ui
            FormLayout layout = new FormLayout("pref, 14dlu, pref, 14dlu, right:pref:grow",
                "top:pref, 7dlu, pref, 3dlu, top:pref:grow");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.DLU14_BORDER);
            CellConstraints cc = new CellConstraints();
            builder.add(picto, cc.xywh(1, 1, 1, 5));
            builder.add(syncStatusPicto, cc.xywh(5, 1, 1, 5));
            builder.add(headerText, cc.xy(3, 1));

            builder.add(infoText1, cc.xywh(3, 3, 1, 1));
            builder.add(infoText2, cc.xywh(3, 5, 1, 1));

            panel = builder.getPanel();
            panel.setBackground(Color.WHITE);
        }
        return panel;
    }

    /**
     * Initalizes the components
     */
    protected void initComponents() {
        headerText = SimpleComponentFactory.createBiggerTextLabel("");
        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");
        picto = new JLabel(Icons.FOLDER_PICTO);
        syncStatusPicto = new JLabel();
        clearPercentage();
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getUIController().getControlQuarter().getSelectionModel()
            .addSelectionChangeListener(new MySelectionChangeListener());
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        if (currentFolder != null) {
            headerText.setText(Translation.getTranslation(
                "quickinfo.folder.status_of_folder", currentFolder.getName()));

            boolean isMembersConnected = currentFolder.getConnectedMembers().length > 0;

            StringBuilder text1 = new StringBuilder();
            if (!isMembersConnected) {
                text1.append(Translation
                    .getTranslation("quickinfo.folder.disconnected"));
            } else if (currentFolder.isTransferring()) {
                text1.append(Translation
                    .getTranslation("quickinfo.folder.is_synchronizing"));
            } else {
                text1.append(Translation
                    .getTranslation("quickinfo.folder.is_in_sync"));
            }

            int nCompletedDls = countCompletedDownloads();
            if (nCompletedDls > 0) {
                // This is a hack(tm)
                text1.append(", "
                    + Translation.getTranslation(
                        "quickinfo.folder.downloads_recently_completed",
                        nCompletedDls));
            }
            
            infoText1.setText(text1.toString());

            FolderStatistic folderStatistic = currentFolder.getStatistic();
            String text2 = Translation.getTranslation(
                "quickinfo.folder.number_of_files_and_size",
                    String.valueOf(folderStatistic.getLocalFilesCount()),
                    Format.formatBytes(folderStatistic.getSize(getController()
                        .getMySelf())));

            infoText2.setText(text2);
            setSyncPercentage(currentFolder.getStatistic().getHarmonizedSyncPercentage());
        }
    }

    /**
     * Set the synchronization percentage image on the right of the panel.
     *
     * @param percentage
     */
    private void setSyncPercentage(double percentage) {
        if (percentage >= 100 || percentage < 0) {
            clearPercentage();
        } else {
            syncStatusPicto.setIcon(Icons.scaleIcon((ImageIcon) Icons.SYNC_ICONS[(int) percentage], SCALE_FACTOR));
            syncStatusPicto.setVisible(true);
            syncStatusPicto.setToolTipText((int) (percentage * 100.0) / 100.0 + " %");
        }
    }

    /**
     * Clear (invisible) the synchronization percentage image.
     */
    public void clearPercentage() {
        syncStatusPicto.setVisible(false);
    }

    // Overridden stuff *******************************************************

    protected JComponent getPicto() {
        return picto;
    }

    protected JComponent getHeaderText() {
        return headerText;
    }

    protected JComponent getInfoText1() {
        return infoText1;
    }

    protected JComponent getInfoText2() {
        return infoText2;
    }

    private void setFolder(Folder folder) {
        if (currentFolder != null) {
            currentFolder.removeFolderListener(myFolderListener);
        }
        currentFolder = folder;
        if (currentFolder != null) {
            currentFolder.addFolderListener(myFolderListener);
            updateText();
        }
    }

    // UI listeners
    private class MySelectionChangeListener implements SelectionChangeListener {

        public void selectionChanged(SelectionChangeEvent event) {
            SelectionModel selectionModel = (SelectionModel) event.getSource();
            Object selection = selectionModel.getSelection();

            if (selection instanceof Folder) {
                setFolder((Folder) selection);
            } else if (selection instanceof Directory) {
                setFolder(((Directory) selection).getRootFolder());
            }
        }
    }

    // Helper code ************************************************************

    private int countCompletedDownloads() {
        int completedDls = 0;
        for (Download dl : getController().getTransferManager()
            .getCompletedDownloadsCollection())
        {
            if (dl.getFile().getFolderInfo().equals(currentFolder.getInfo())) {
                completedDls++;
            }
        }
        return completedDls;
    }

    // Core listeners *********************************************************
    private class MyFolderListener implements FolderListener {

        public void folderChanged(FolderEvent folderEvent) {
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            updateText();
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
        }

        public void scanResultCommited(FolderEvent folderEvent) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    private class MyNodeManagerListener extends NodeManagerAdapter {
        @Override
        public void nodeConnected(NodeManagerEvent e) {
            if (currentFolder.hasMember(e.getNode())) {
                updateText();
            }
        }

        @Override
        public void nodeDisconnected(NodeManagerEvent e) {
            if (currentFolder.hasMember(e.getNode())) {
                updateText();
            }
        }

        @Override
        public boolean fireInEventDispathThread() {
            return true;
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

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}