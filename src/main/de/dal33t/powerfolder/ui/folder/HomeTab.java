package de.dal33t.powerfolder.ui.folder;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.LeaveAction;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.OSUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.EstimatedTime;
import de.dal33t.powerfolder.util.ui.SelectionModel;

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

    private QuickInfoPanel quickInfo;
    private JPanel panel;
    private JPanel folderDetailsPanel;
    private JPanel toolbar;
    private JButton leaveFolderButton;
    private JButton sendInvitationButton;
    private JButton syncFolderButton;
    private BaseAction openLocalFolder;

    private JLabel localFolderLabel;
    private JLabel deletedFilesCountLabel;
    private JLabel expectedFilesCountLabel;
    private JLabel totalFilesCountLabel;
    private JLabel totalNormalFilesCountLabel;
    private JLabel totalSizeLabel;
    private JLabel fileCountLabel;
    private JLabel sizeLabel;
    private JLabel syncPercentageLabel;
    private JLabel syncETALabel;

    public HomeTab(Controller controller) {
        super(controller);
        folderModel = new SelectionModel();
    }

    /** set the folder to display */
    public void setFolder(Folder folder) {
        this.folder = folder;
        // TODO Remvoe listener from old folder.
        folder.addFolderListener(new MyFolderListener());
        folderModel.setSelection(folder);

        update();
    }

    public String getTitle() {
        return Translation.getTranslation("folderpanel.hometab.title");
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();

            FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, pref, fill:pref:grow, pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(quickInfo.getUIComponent(), cc.xy(1, 1));
            builder.addSeparator(null, cc.xy(1, 2));
            builder.add(folderDetailsPanel, cc.xy(1, 3));
            builder.addSeparator(null, cc.xy(1, 4));
            builder.add(toolbar, cc.xy(1, 5));

            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {

        quickInfo = new FolderQuickInfoPanel(getController());
        leaveFolderButton = new JButton(new LeaveAction(getController(),
            folderModel));
        sendInvitationButton = new JButton(getUIController()
            .getInviteUserAction());
        syncFolderButton = new JButton(getUIController().getSyncFolderAction());
        openLocalFolder = new OpenLocalFolder(getController());
        JLabel locFolderLabel = new JLabel(Translation
            .getTranslation("folderpanel.hometab.local_folder_location"));
        localFolderLabel = new JLabel();
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

        FormLayout layout = new FormLayout(
            "4dlu, pref, 4dlu, pref",
            "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref");
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
        builder.add(createLocalFolderLabelLink(), cc.xy(4, row));

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

    private JLabel createLocalFolderLabelLink() {
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            localFolderLabel.setCursor(Cursor
                .getPredefinedCursor(Cursor.HAND_CURSOR));
            localFolderLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    openLocalFolder.actionPerformed(null);
                }
            });
        }
        return localFolderLabel;
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
            localFolderLabel
                .setText("<HTML><BODY><SPAN style=\"color: #000000; text-decoration: underline;\">"
                    + folder.getLocalBase().getAbsolutePath()
                    + "</SPAN></BODY></HTML>");
        } else {
            localFolderLabel.setText(folder.getLocalBase().getAbsolutePath());
        }

        FolderStatistic folderStatistic = folder.getStatistic();
        deletedFilesCountLabel.setText(folderStatistic
            .getTotalDeletedFilesCount()
            + "");
        expectedFilesCountLabel.setText(folderStatistic
            .getTotalExpectedFilesCount()
            + "");
        totalFilesCountLabel.setText(folderStatistic.getTotalFilesCount() + "");
        totalNormalFilesCountLabel.setText(folderStatistic
            .getTotalNormalFilesCount()
            + "");
        totalSizeLabel.setText(Format.formatBytes(folderStatistic
            .getTotalSize()));
        fileCountLabel.setText(folderStatistic.getFilesCount(getController()
            .getMySelf())
            + "");
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
            + "%");
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

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
