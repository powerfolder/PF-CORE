package de.dal33t.powerfolder.ui.folder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import javax.swing.*;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.FolderLeaveAction;
import de.dal33t.powerfolder.ui.action.SyncFolderAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.EstimatedTime;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;

/**
 * Shows information about the (Joined) Folder and gives the user some actions
 * he can do on the folder.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class HomeTab extends PFUIComponent implements FolderTab {

    private static final Logger LOG = Logger.getLogger(HomeTab.class);

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
    private JButton syncFolderButton;
    private BaseAction openLocalFolder;

    private JComponent localFolderLabel;
    private JLabel deletedFilesCountLabel;
    private JLabel expectedFilesCountLabel;
    private JLabel totalFilesCountLabel;
    private JLabel totalNormalFilesCountLabel;
    private JLabel totalSizeLabel;
    private JLabel fileCountLabel;
    private JLabel sizeLabel;
    private JLabel syncPercentageLabel;
    private JLabel syncETALabel;
    private ValueModel localFolderValueModel = new ValueHolder();
    private String temporaryFolderBaseDir;


    public HomeTab(Controller controller) {
        super(controller);
        folderModel = new SelectionModel();
        myFolderListener = new MyFolderListener();
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
        Action syncFolderAction = new SyncFolderAction(getController(),
            folderModel);
        syncFolderButton = new JButton(syncFolderAction);
        openLocalFolder = new OpenLocalFolder(getController());
        JLabel locFolderLabel = new JLabel(Translation
            .getTranslation("folderpanel.hometab.local_folder_location"));
        createLocalFolder();
        deletedFilesCountLabel = new JLabel();
        expectedFilesCountLabel = new JLabel();
        totalFilesCountLabel = new JLabel();
        totalNormalFilesCountLabel = new JLabel();
        totalSizeLabel = new JLabel();
        fileCountLabel = new JLabel();
        sizeLabel = new JLabel();
        syncPercentageLabel = new JLabel();
        syncETALabel = new JLabel();

        toolbar = createToolBar();

        FormLayout layout;
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            // Widen local folder label (Complex component).
            layout = new FormLayout(
                "4dlu, pref, 4dlu, 120dlu",
                "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref");
        } else {
            // Show the whole of the local folder label.
            layout = new FormLayout(
                "4dlu, pref, 4dlu, pref",
                "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref");
        }
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("4dlu, 0, 0, 0"));
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.addLabel(Translation
            .getTranslation("folderpanel.hometab.synchronisation_percentage"),
            cc.xy(2, row));
        builder.add(syncPercentageLabel, cc.xy(4, row));

        row += 2;
        builder.add(locFolderLabel, cc.xy(2, row));
        builder.add(localFolderLabel, cc.xy(4, row));

        row += 2;
        builder.addLabel(Translation.getTranslation("folderpanel.hometab."
            + "number_of_local_files_in_folder"), cc.xy(2, row));
        builder.add(totalNormalFilesCountLabel, cc.xy(4, row));

        row += 2;
        builder.addLabel(Translation.getTranslation("folderpanel.hometab."
            + "number_of_deleted_files_in_folder"), cc.xy(2, row));
        builder.add(deletedFilesCountLabel, cc.xy(4, row));

        row += 2;
        builder.addLabel(Translation.getTranslation("folderpanel.hometab."
            + "number_of_available_files_at_other_members"), cc.xy(2, row));
        builder.add(expectedFilesCountLabel, cc.xy(4, row));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("folderpanel.hometab.total_number_of_files"), cc
            .xy(2, row));
        builder.add(totalFilesCountLabel, cc.xy(4, row));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("folderpanel.hometab.local_size"), cc.xy(2, row));
        builder.add(sizeLabel, cc.xy(4, row));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("folderpanel.hometab.total_size"), cc.xy(2, row));
        builder.add(totalSizeLabel, cc.xy(4, row));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("folderpanel.hometab.synchronisation_eta"), cc.xy(
            2, row));
        builder.add(syncETALabel, cc.xy(4, row));

        folderDetailsPanel = builder.getPanel();
    }

    /**
     * For local folder,
     * create a text filed / button pair for Win/mac,
     * or just a text field for others.
     */
    private void createLocalFolder() {
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {

            // Set up selection field and button.
            ActionListener preChangeListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    temporaryFolderBaseDir = (String) localFolderValueModel.getValue();
                }
            };
            String title = Translation
                .getTranslation("folderpanel.hometab.choose_local_folder.title");
            localFolderLabel = ComplexComponentFactory.createDirectorySelectionField(title,
                    localFolderValueModel, preChangeListener, new MyPostChangeListener());
        } else {
            // Set up simple label.
            localFolderLabel = new JLabel();
        }
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();

        bar.addGridded(syncFolderButton);
        bar.addRelatedGap();
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            bar.addUnrelatedGap();
            bar.addGridded(new JButton(openLocalFolder));
        }
        bar.addUnrelatedGap();
        bar.addGridded(sendInvitationButton);

        bar.addRelatedGap();
        bar.addGridded(leaveFolderButton);

        // TODO: Disable webservice button in RC2
        // bar.addRelatedGap();
        // bar.addGridded(webServiceButton);

        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }

    /** Helper class, Opens the local folder on action * */
    private class OpenLocalFolder extends BaseAction {

        public OpenLocalFolder(Controller controller) {
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

        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            localFolderValueModel.setValue(folder.getLocalBase().getAbsolutePath());
        } else {
            ((JLabel) localFolderLabel).setText(folder.getLocalBase().getAbsolutePath());
        }

        FolderStatistic folderStatistic = folder.getStatistic();
        deletedFilesCountLabel.setText(String.valueOf(folderStatistic
                .getTotalDeletedFilesCount()));
        expectedFilesCountLabel.setText(MessageFormat.format("{0}", folderStatistic
                .getTotalExpectedFilesCount()));
        totalFilesCountLabel.setText(String.valueOf(folderStatistic.getTotalFilesCount()));
        totalNormalFilesCountLabel.setText(String.valueOf(folderStatistic
                .getTotalNormalFilesCount()));
        totalSizeLabel.setText(Format.formatBytes(folderStatistic
            .getTotalSize()));
        fileCountLabel.setText(String.valueOf(folderStatistic.getFilesCount(getController()
                .getMySelf())));
        sizeLabel.setText(Format.formatBytes(folderStatistic
            .getSize(getController().getMySelf())));

        double syncPercentage;
        if (folder.getSyncProfile() == SyncProfile.SYNCHRONIZE_PCS) {
            syncPercentage = folderStatistic.getTotalSyncPercentage();
        } else {
            syncPercentage = folderStatistic.getSyncPercentage(getController()
                .getMySelf());
        }
        syncPercentageLabel.setText(Format.NUMBER_FORMATS
            .format(syncPercentage)
            + '%');
        syncPercentageLabel.setIcon(Icons.getSyncIcon(syncPercentage));

        if (folderStatistic.getDownloadCounter() == null
            || syncPercentage >= 100)
        {
            syncETALabel.setText("");
        } else {
            syncETALabel.setText(new EstimatedTime(folderStatistic
                .getDownloadCounter().calculateEstimatedMillisToCompletion(),
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
     * Action listener implementation to move files to bnew folder
     * if user has selected a different location.
     */
    private class MyPostChangeListener implements ActionListener {

        /**
         * Response from local folder move event.
         *
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            String newFolderBaseDir = (String) localFolderValueModel.getValue();

            // Ensure by default the displayed folder is as it was.
            localFolderValueModel.setValue(temporaryFolderBaseDir);

            // newFolderBaseDir is null if user cancelled.
            if (newFolderBaseDir != null && !newFolderBaseDir.equals(temporaryFolderBaseDir)) {

                File f = new File(newFolderBaseDir);
                int result = FileUtils.canMoveFiles(folder.getLocalBase(), f);
                if (result == 1) {
                    String title = Translation
                            .getTranslation("folderpanel.hometab.confirm_local_folder_move.title");
                    String message = Translation
                            .getTranslation("folderpanel.hometab.folder_not_empty.text",
                                    temporaryFolderBaseDir, newFolderBaseDir);

                    JOptionPane.showMessageDialog(
                            getController().getUIController().getMainFrame().getUIComponent(),
                            message, title, JOptionPane.ERROR_MESSAGE);
                    return;
                } else if (result == 2) {
                    String title = Translation
                            .getTranslation("folderpanel.hometab.confirm_local_folder_move.title");
                    String message = Translation
                            .getTranslation("folderpanel.hometab.sub_folder_move.text",
                                    temporaryFolderBaseDir, newFolderBaseDir);

                    JOptionPane.showMessageDialog(
                            getController().getUIController().getMainFrame().getUIComponent(),
                            message, title, JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Confirm move.
                if (showConfirmationDialog(newFolderBaseDir) == JOptionPane.YES_OPTION) {
                    MyFolderMoveWorker mfmw = new MyFolderMoveWorker(folder, f);
                    mfmw.start();
                    update();
                    // Update display to new folder.
                    localFolderValueModel.setValue(newFolderBaseDir);
                }
            }
        }

        /**
         * Confirm that the user really wants to make the move.
         *
         * @param newFolderValue
         * @return 
         */
        private int showConfirmationDialog(String newFolderValue) {
            String title = Translation
                    .getTranslation("folderpanel.hometab.confirm_local_folder_move.title");
            String message = Translation
                    .getTranslation("folderpanel.hometab.confirm_local_folder_move.text",
                            temporaryFolderBaseDir, newFolderValue);

            return JOptionPane.showConfirmDialog(
                    getController().getUIController().getMainFrame().getUIComponent(),
                    message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        }
    }

    /**
     * Visualisation worker class in case the folder move takes a long time.
     */
    private class MyFolderMoveWorker extends ActivityVisualizationWorker {

        private Folder oldFolder;
        private File newLocalBase;
        private FolderException exception;

        public MyFolderMoveWorker(Folder oldFolder, File newLocalBase) {
            super(getController().getUIController());
            this.oldFolder = oldFolder;
            this.newLocalBase = newLocalBase;
        }

        protected String getTitle() {
            return Translation.getTranslation("foldermove.progress.title");
        }

        protected String getWorkingText() {
            return Translation.getTranslation("foldermove.progress.text",
                oldFolder.getName());
        }

        public Object construct() {
            try {
                FolderRepository repository = getController().getFolderRepository();

                // Remove original folder from the folder repository.
                repository.removeFolder(folder);

                // Create new folder
                FolderSettings folderSettings =
                        new FolderSettings(newLocalBase,
                                oldFolder.getSyncProfile(),
                                false, oldFolder.isUseRecycleBin());
                repository.createFolder(oldFolder.getInfo(), folderSettings);

                // Move contents
                File oldLocalBase = oldFolder.getLocalBase();
                FileUtils.moveFiles(oldLocalBase, newLocalBase);

                return null;
            } catch (FolderException e) {
                exception = e;
            } catch (IOException e) {
                exception = new FolderException(e);
            }
            return null;
        }

        /**
         * Display error if something went wrong.
         */
        public void finished() {
            if (exception != null) {
                if (!getController().isConsoleMode()) {
                    exception.show(getController());
                }
                LOG.error("Move folder error", exception);
            }
        }

    }
}
