/* $Id: MyFoldersQuickInfoPanel.java,v 1.3 2006/04/14 14:52:32 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.myfolders;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Show concentrated information about the whole folder repository
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.3 $
 */
public class MyFoldersQuickInfoPanel extends QuickInfoPanel {
    private JComponent picto;
    private JComponent headerText;
    private JLabel infoText1;
    private JLabel infoText2;

    protected MyFoldersQuickInfoPanel(Controller controller) {
        super(controller);
    }

    /**
     * Initalizes the components
     */
    @Override
    protected void initComponents()
    {
        headerText = SimpleComponentFactory.createBiggerTextLabel(Translation
            .getTranslation("quickinfo.myfolders.title"));

        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");

        picto = new JLabel(Icons.MYFOLDERS_PICTO);

        updateText();
        registerListeners();
    }

    /**
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        StringBuffer foldersText = new StringBuffer();
        int nTotalFiles = 0;
        long nTotalBytes = 0;
        FolderRepository repo = getController().getFolderRepository();
        Folder[] folders = repo.getFolders();

        // FIXME: i18n support right to left reading languages
        for (Folder folder : folders) {
            if (folder.isTransferring()) {
                foldersText.append(folder.getName());
                foldersText.append(", ");
            }

            nTotalFiles += folder.getStatistic().getTotalFilesCount();
            nTotalBytes += folder.getStatistic().getTotalSize();
        }

        String text1;
        if (foldersText.length() == 0) {
            text1 = Translation.getTranslation("quickinfo.myfolders.insyncall");
        } else {
            foldersText.replace(foldersText.length() - 2, foldersText.length(),
                "...");
            text1 = Translation.getTranslation("quickinfo.myfolders.syncing",
                foldersText);
        }

        infoText1.setText(text1);

        String text2 = Translation.getTranslation(
            "quickinfo.myfolders.folders", Format.formatBytes(nTotalBytes),
            Integer.valueOf(folders.length));
        infoText2.setText(text2);
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

    // Core listeners *********************************************************

    /**
     * Listener on folder repo
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {
        public void folderRemoved(FolderRepositoryEvent e) {
            updateText();
        }

        public void folderCreated(FolderRepositoryEvent e) {
            updateText();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            updateText();
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            updateText();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    /**
     * Listener to transfer manager
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class MyTransferManagerListener extends TransferAdapter {

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

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
