package de.dal33t.powerfolder.ui.folder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.*;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.FolderLeaveAction;
import de.dal33t.powerfolder.ui.action.SyncFolderAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.EstimatedTime;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SyncProfileUtil;
import de.dal33t.powerfolder.util.ui.TimeEstimator;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

/**
 * Shows information about the (Joined) Folder and gives the user some actions
 * he can do on the folder.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class HomeTab extends PFUIComponent implements FolderTab {

    private Folder folder;

    /** Contains the selected folder. */
    private SelectionModel folderModel;

    private MyFolderListener myFolderListener;

    private QuickInfoPanel quickInfo;
    private JComponent panel;
    private JPanel folderDetailsPanel;
    private JPanel toolbar;
    private JButton leaveFolderButton;
    private JButton sendInvitationButton;
    private JButton previewJoinButton;
    private JButton syncFolderButton;
    private BaseAction openLocalFolder;

    private JTextField localFolderField;
    private JButton localFolderButton;
    private JLabel expectedFilesCountLabel;
    private JLabel totalNormalFilesCountLabel;
    private JLabel totalSizeLabel;
    private JLabel fileCountLabel;
    private JLabel sizeLabel;
    private JLabel syncPercentageLabel;
    private JLabel syncETALabel;

    private TimeEstimator syncETAEstimator;

    private final boolean previewMode;
    
    public HomeTab(Controller controller, boolean previewMode) {
        super(controller);
        folderModel = new SelectionModel();
        this.previewMode = previewMode;
        myFolderListener = new MyFolderListener();
        syncETAEstimator = new TimeEstimator(Constants.ESTIMATION_WINDOW_MILLIS);
    }

    /** set the folder to display */
    public void setFolder(Folder folder) {
        folder.removeFolderListener(myFolderListener);
        this.folder = folder;
        folder.addFolderListener(myFolderListener);
        folderModel.setSelection(folder);

        update();
    }

    public String getTitle() {
        return Translation.getTranslation("folderpanel.hometab.title");
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();

            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickInfo.getUIComponent());
            builder.setToolbar(toolbar);
            builder.setContent(folderDetailsPanel);

            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {

        quickInfo = new FolderQuickInfoPanel(getController());
        leaveFolderButton = new JButton(new FolderLeaveAction(getController(),
            folderModel));
        sendInvitationButton = new JButton(getUIController()
            .getInviteUserAction());
        previewJoinButton = new JButton(getUIController()
            .getPreviewJoinAction());
        Action syncFolderAction = new SyncFolderAction(getController());
        syncFolderButton = new JButton(syncFolderAction);
        openLocalFolder = new OpenLocalFolder(getController());
        JLabel locFolderLabel = new JLabel(Translation
            .getTranslation("folderpanel.hometab.local_folder_location"));
        expectedFilesCountLabel = new JLabel();
        totalNormalFilesCountLabel = new JLabel();
        totalSizeLabel = new JLabel();
        fileCountLabel = new JLabel();
        sizeLabel = new JLabel();
        syncPercentageLabel = new JLabel();
        syncETALabel = new JLabel();
        localFolderField = new JTextField();
        localFolderField.setEditable(false);
        localFolderButton = new JButton("...");
        localFolderButton.addActionListener(new MyActionListener());

        toolbar = createToolBar();

        FormLayout layout = new FormLayout(
            "4dlu, pref, 4dlu, 120dlu, 4dlu, pref, pref:grow",
            "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("4dlu, 0, 0, 0"));
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.addLabel(Translation
            .getTranslation("folderpanel.hometab.synchronisation_percentage"),
            cc.xy(2, row));
        builder.add(syncPercentageLabel, cc.xyw(4, row, 4));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("folderpanel.hometab.synchronisation_eta"), cc.xy(
            2, row));
        builder.add(syncETALabel, cc.xyw(4, row, 2));

        row += 2;
        builder.addLabel(Translation.getTranslation("folderpanel.hometab."
            + "number_of_local_files_in_folder"), cc.xy(2, row));
        builder.add(totalNormalFilesCountLabel, cc.xyw(4, row, 2));

        row += 2;
        builder.addLabel(Translation.getTranslation("folderpanel.hometab."
            + "number_of_available_files_at_other_members"), cc.xy(2, row));
        builder.add(expectedFilesCountLabel, cc.xyw(4, row, 2));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("folderpanel.hometab.local_size"), cc.xy(2, row));
        builder.add(sizeLabel, cc.xyw(4, row, 2));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("folderpanel.hometab.total_size"), cc.xy(2, row));
        builder.add(totalSizeLabel, cc.xyw(4, row, 2));

        row += 2;
        builder.add(locFolderLabel, cc.xy(2, row));
        builder.add(localFolderField, cc.xy(4, row));
        if (!previewMode) {
            // Cannot move folder in preview mode.
            builder.add(localFolderButton, cc.xy(6, row));
        }

        folderDetailsPanel = builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();

        // Should not be able to do much
        // until folder is properly joined.
        if (!previewMode) {
            bar.addGridded(syncFolderButton);
            if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
                bar.addRelatedGap();
                bar.addGridded(new JButton(openLocalFolder));
            }

            bar.addRelatedGap();
            bar.addGridded(sendInvitationButton);
        }

        if (previewMode) {
            bar.addGridded(previewJoinButton);
        }
        bar.addRelatedGap();
        bar.addGridded(leaveFolderButton);

        // TODO: Disable webservice button in RC2
        // bar.addRelatedGap();
        // bar.addGridded(webServiceButton);

        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    /**
     * Controls the movement of a folder directory.
     */
    private void moveLocalFolder() {

        // Find out if the user wants to move the content of the current folder
        // to the new one.
        boolean moveContent = shouldMoveContent();
        File originalDirectory = folder.getLocalBase();

        // Select the new folder.
        JFileChooser fileChooser = DialogFactory.createFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setSelectedFile(originalDirectory);
        fileChooser.setDialogTitle(Translation
            .getTranslation("folderpanel.hometab.choose_local_folder.title"));
        int result = fileChooser.showOpenDialog(localFolderButton);
        if (result == JFileChooser.APPROVE_OPTION) {
            File newDirectory = fileChooser.getSelectedFile();
            if (newDirectory != null && !newDirectory.equals(originalDirectory))
            {

                // Check for any problems with the new folder.
                if (checkNewLocalFolder(newDirectory)) {

                    // Confirm move.
                    if (shouldMoveLocal(newDirectory))
                    {
                        try {
                            ActivityVisualizationWorker worker = new MyActivityVisualizationWorker(
                                moveContent, originalDirectory, newDirectory);
                            worker.start();
                        } catch (Exception e) {
                            // Probably failed to create temp directory.
                            DialogFactory.genericDialog(
                                    getController().getUIController().getMainFrame().getUIComponent(),
                                    Translation.getTranslation("folderpanel.hometab.move_error.title"),
                                    Translation.getTranslation("folderpanel.hometab.move_error.temp"),
                                    getController().isVerbose(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Displays an error if the folder move failed.
     * 
     * @param e
     *            the error
     * @param tempDirectory
     *            temp directory. different message if files there.
     */
    private void displayError(Exception e, File tempDirectory) {
        if (tempDirectory != null && tempDirectory.exists()
            && tempDirectory.listFiles().length > 0)
        {
            DialogFactory.genericDialog(getController().getUIController().getMainFrame().getUIComponent(),
                    Translation.getTranslation("folderpanel.hometab.move_error.title"),
                Translation.getTranslation("folderpanel.hometab.move_error.other_temp",
                        e.getMessage(),
                        tempDirectory.getAbsolutePath()),
                    GenericDialogType.WARN);
        } else {
            DialogFactory.genericDialog(getController().getUIController().getMainFrame().getUIComponent(),
                    Translation.getTranslation("folderpanel.hometab.move_error.title"),
                    Translation.getTranslation("folderpanel.hometab.move_error.other", e.getMessage()),
                    GenericDialogType.WARN);
        }
    }

    /**
     * Moves the contents of a folder to another via a temporary directory.
     * 
     * @param moveContent
     * @param originalDirectory
     * @param newDirectory
     * @param tempDirectory
     * @return
     */
    private Object transferFolder(boolean moveContent, File originalDirectory,
        File newDirectory, File tempDirectory)
    {
        try {
            if (moveContent) {
                FileUtils.recursiveCopy(originalDirectory, tempDirectory);
            }

            // Remove the old folder from the repository.
            FolderRepository repository = getController().getFolderRepository();
            repository.removeFolder(folder, false);

            if (moveContent) {
                // Delete the old files.
                FileUtils.recursiveDelete(originalDirectory);
            }

            // Copy the files from the temp directory to the new local base
            if (!newDirectory.exists()) {
                if (!newDirectory.mkdirs()) {
                	log().error("Failed to create directory: " + newDirectory);
                }
            }

            if (moveContent) {
                FileUtils.recursiveCopy(tempDirectory, newDirectory);
            }

            // Create the new Folder in the repository.
            FolderInfo fi = new FolderInfo(folder);
            FolderSettings fs = new FolderSettings(newDirectory, folder
                .getSyncProfile(), false, folder.isUseRecycleBin(), false);
            folder = repository.createFolder(fi, fs);

            if (moveContent) {
                // Finally delete the temp files.
                FileUtils.recursiveDelete(tempDirectory);
            }

            // Update with new folder info.
            update();
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    /**
     * Should the content of the existing folder be moved to the new location?
     * 
     * @return true if should move.
     */
    private boolean shouldMoveContent() {
        int result = DialogFactory.genericDialog(getController().getUIController().getMainFrame().getUIComponent(),
                Translation.getTranslation("folderpanel.hometab.move_content.title"),
                Translation.getTranslation("folderpanel.hometab.move_content"),
                new String[]{
                        Translation.getTranslation("folderpanel.hometab.move_content.move"),
                        Translation.getTranslation("folderpanel.hometab.move_content.dont")},
                0, GenericDialogType.INFO); // Default is move content.
        return result == 0;
    }

    /**
     * Confirm that the user really does want to go ahead with the move.
     * 
     * @param newDirectory
     * @return true if the user wishes to move.
     */
    private boolean shouldMoveLocal(File newDirectory) {
        String title = Translation
            .getTranslation("folderpanel.hometab.confirm_local_folder_move.title");
        String message = Translation.getTranslation(
            "folderpanel.hometab.confirm_local_folder_move.text", folder
                .getLocalBase().getAbsolutePath(), newDirectory
                .getAbsolutePath());

        return DialogFactory.genericDialog(
                getController().getUIController().getMainFrame().getUIComponent(),
                title,
                message,
                new String[]{
                        Translation.getTranslation("general.continue"),
                        Translation.getTranslation("general.cancel")},
                0, GenericDialogType.INFO) == 0;
    }

    /**
     * Do some basic validation. Warn if moving to a folder that has files /
     * directories in it.
     * 
     * @param currentDirectory
     * @param newDirectory
     * @return
     */
    private boolean checkNewLocalFolder(File newDirectory) {

        // Warn if target directory is not empty.
        if (newDirectory != null && newDirectory.exists()
            && newDirectory.listFiles().length > 0) {
            int result = DialogFactory.genericDialog(
                    getController().getUIController().getMainFrame().getUIComponent(),
                    Translation.getTranslation("folderpanel.hometab.folder_not_empty.title"),
                    Translation.getTranslation("folderpanel.hometab.folder_not_empty",
                            newDirectory.getAbsolutePath()),
                    new String[] {
                            Translation.getTranslation("general.continue"),
                            Translation.getTranslation("general.cancel")},
                    1, GenericDialogType.WARN); // Default is cancel.
            if (result != 0) {
                // User does not want to move to new folder.
                return false;
            }
        }

        // All good.
        return true;
    }

    /** Helper class, Opens the local folder on action * */
    private class OpenLocalFolder extends BaseAction {

        OpenLocalFolder(Controller controller) {
            super("open_local_folder", controller);
        }

        /**
         * opens the folder currently in view in the operatings systems file
         * explorer
         */
        public void actionPerformed(ActionEvent e) {
            File localBase = folder.getLocalBase();
            try {
                FileUtils.executeFile(localBase);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void update() {

        localFolderField.setText(folder.getLocalBase().getAbsolutePath());

        FolderStatistic folderStatistic = folder.getStatistic();
        expectedFilesCountLabel.setText(MessageFormat.format("{0}",
            folderStatistic.getIncomingFilesCount()));
        totalNormalFilesCountLabel.setText(String.valueOf(folderStatistic
            .getLocalFilesCount()));
        totalSizeLabel.setText(Format.formatBytes(folderStatistic
            .getTotalSize()));
        fileCountLabel.setText(String.valueOf(folderStatistic
            .getFilesCount(getController().getMySelf())));
        sizeLabel.setText(Format.formatBytes(folderStatistic
            .getSize(getController().getMySelf())));

        double sync = folder.getStatistic().getHarmonizedSyncPercentage();
        String syncProfileText = Translation.getTranslation(folder
                .getSyncProfile().getTranslationId());
        if (previewMode) {

            // Folder uses NO_SYNC profile in preview mode,
            // but this is identical to PROJECT_WORK, so show NO_SYNC text.
            syncPercentageLabel.setText(Translation
                    .getTranslation("syncprofile.no_sync.name"));
            syncPercentageLabel.setIcon(SyncProfileUtil.getSyncIcon(0));
        } else {
            syncPercentageLabel.setText(SyncProfileUtil.renderSyncPercentage(sync)
                + ", " + syncProfileText);
            syncPercentageLabel.setIcon(SyncProfileUtil.getSyncIcon(sync));
        }

        if (folderStatistic.getDownloadCounter() == null || sync >= 100) {
            syncETALabel.setText("");
        } else {
        	syncETAEstimator.addValue(folderStatistic.getLocalSyncPercentage());
            syncETALabel.setText(new EstimatedTime(syncETAEstimator.estimatedMillis(100.0),
                true).toString());
        }
    }

    private class MyFolderListener implements FolderListener {

        public void folderChanged(FolderEvent folderEvent) {
            update();
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            update();
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
            update();
        }

        public void scanResultCommited(FolderEvent folderEvent) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    /**
     * Class to handle the folder move button action.
     */
    private class MyActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(localFolderButton)) {
                moveLocalFolder();
            }
        }
    }

    /**
     * Visualisation worker for folder move.
     */
    private class MyActivityVisualizationWorker extends
        ActivityVisualizationWorker
    {

        private final boolean moveContent;
        private final File originalDirectory;
        private final File newDirectory;
        private final File tempDirectory;

        MyActivityVisualizationWorker(boolean moveContent,
            File originalDirectory, File newDirectory) throws IOException
        {
            super(getUIController());
            this.moveContent = moveContent;
            this.originalDirectory = originalDirectory;
            this.newDirectory = newDirectory;
            tempDirectory = FileUtils.createTemporaryDirectory();
        }

        @Override
        public Object construct() {
            return transferFolder(moveContent, originalDirectory, newDirectory,
                tempDirectory);
        }

        @Override
        protected String getTitle() {
            return Translation
                .getTranslation("folderpanel.hometab.working.title");
        }

        @Override
        protected String getWorkingText() {
            return Translation
                .getTranslation("folderpanel.hometab.working.description");
        }

        @Override
        public void finished() {
            if (get() != null) {
                displayError((Exception) get(), tempDirectory);
            }
        }
    }
}
