/* $Id: FolderQuickInfoPanel.java,v 1.3 2006/04/06 13:48:05 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.folder;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.event.TransferManagerListener;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionChangeListener;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Show concentrated information about the whole folder repository
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class FolderQuickInfoPanel extends QuickInfoPanel {
    private JComponent picto;
    private JLabel headerText;
    private JLabel infoText1;
    private JLabel infoText2;
    private Folder currentFolder;
    private MyFolderListener myFolderListener;

    protected FolderQuickInfoPanel(Controller controller) {
        super(controller);
        myFolderListener = new MyFolderListener();
    }

    /**
     * Initalizes the components
     */
    @Override
    protected void initComponents()
    {
        headerText = SimpleComponentFactory.createBiggerTextLabel("");
        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");
        picto = new JLabel(Icons.FOLDER_PICTO);
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
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        if (currentFolder != null) {
            headerText.setText(Translation.getTranslation(
                "quickinfo.folder.status_of_folder", currentFolder.getName()));

            String text1;
            if (currentFolder.isSynchronizing()) {
                text1 = Translation
                    .getTranslation("quickinfo.folder.is_synchronizing");
            } else {
                text1 = Translation
                    .getTranslation("quickinfo.folder.is_in_sync");
            }

            infoText1.setText(text1);

            FolderStatistic folderStatistic = currentFolder.getStatistic();
            String text2 = Translation.getTranslation(
                "quickinfo.folder.number_of_files_and_size", ""
                    + folderStatistic.getTotalNormalFilesCount(), Format
                    .formatBytes(folderStatistic.getSize(getController()
                        .getMySelf())));

            int nCompletedDls = countCompletedDownloads();
            if (nCompletedDls > 0) {
                // This is a hack(tm)
                text2 += ", "
                    + Translation.getTranslation(
                        "quickinfo.folder.downloads_recently_completed",
                        nCompletedDls);
            }
            infoText2.setText(text2);
        }
    }

    // Overridden stuff *******************************************************

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