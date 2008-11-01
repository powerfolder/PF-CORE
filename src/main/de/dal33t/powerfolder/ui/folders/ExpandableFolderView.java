package de.dal33t.powerfolder.ui.folders;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Format;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.text.DecimalFormat;

public class ExpandableFolderView extends PFUIComponent {

    private final Folder folder;
    private JButtonMini expandCollapseButton;
    private JButtonMini syncFolderButton;
    private JPanel uiComponent;
    private JPanel lowerOuterPanel;
    private AtomicBoolean expanded;

    private JLabel filesLabel;
    private JLabel syncPercentLabel;
    private JLabel totalSizeLabel;
    private JLabel membersLabel;

    private MyFolderListener myFolderListener;
    private MyFolderMembershipListener myFolderMembershipListener;
    public ExpandableFolderView(Controller controller, Folder folder) {
        super(controller);
        this.folder = folder;
    }

    private void buildUI() {

        initComponent();

        // Build ui
        FormLayout upperLayout = new FormLayout("pref, 3dlu, pref, pref:grow, 3dlu, pref, 3dlu, pref",
            "pref");
        PanelBuilder upperBuilder = new PanelBuilder(upperLayout);
        CellConstraints cc = new CellConstraints();

        upperBuilder.add(new JLabel(Icons.DIRECTORY), cc.xy(1, 1));
        upperBuilder.add(new JLabel(folder.getName()), cc.xy(3, 1));
        upperBuilder.add(syncFolderButton, cc.xy(6, 1));
        upperBuilder.add(expandCollapseButton, cc.xy(8, 1));

        JPanel upperPanel = upperBuilder.getPanel();

        // Build lower detials with line border.
        FormLayout lowerLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");
        PanelBuilder lowerBuilder = new PanelBuilder(lowerLayout);

        String transferMode = Translation.getTranslation("exp_folder_view.transfer_mode",
                folder.getSyncProfile().getProfileName());
        lowerBuilder.addSeparator(null, cc.xy(2, 2));
        
        lowerBuilder.add(new JLabel(transferMode), cc.xy(2, 4));

        lowerBuilder.addSeparator(null, cc.xy(2, 6));

        lowerBuilder.add(syncPercentLabel, cc.xy(2, 8));
        
        lowerBuilder.add(filesLabel, cc.xy(2, 10));

        lowerBuilder.add(totalSizeLabel, cc.xy(2, 12));

        lowerBuilder.addSeparator(null, cc.xy(2, 14));

        lowerBuilder.add(membersLabel, cc.xy(2, 16));

        JPanel lowerPanel = lowerBuilder.getPanel();
       // lowerPanel.setBorder(BorderFactory.createEtchedBorder());

        // Build spacer then lower outer with lower panel
        FormLayout lowerOuterLayout = new FormLayout("pref:grow",
            "3dlu, pref");
        PanelBuilder lowerOuterBuilder = new PanelBuilder(lowerOuterLayout);
        lowerOuterPanel = lowerOuterBuilder.getPanel();
        lowerOuterPanel.setVisible(false);
        lowerOuterBuilder.add(lowerPanel, cc.xy(1, 2));

        // Build border around upper and lower
        FormLayout borderLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "3dlu, pref, pref, 3dlu");
        PanelBuilder borderBuilder = new PanelBuilder(borderLayout);
        borderBuilder.add(upperPanel, cc.xy(2, 2));
        borderBuilder.add(lowerOuterBuilder.getPanel(), cc.xy(2, 3));
        JPanel borderPanel = borderBuilder.getPanel();
        borderPanel.setBorder(BorderFactory.createEtchedBorder());

        // Build ui with vertical space before the next one
        FormLayout outerLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "pref, 3dlu");
        PanelBuilder outerBuilder = new PanelBuilder(outerLayout);
        outerBuilder.add(borderPanel, cc.xy(2, 1));

        uiComponent = outerBuilder.getPanel();

    }

    private void initComponent() {
        expanded = new AtomicBoolean();

        expandCollapseButton = new JButtonMini(Icons.EXPAND,
                Translation.getTranslation("exp_folder_view.expand"));
        expandCollapseButton.addActionListener(new MyActionListener());
        syncFolderButton = new JButtonMini(Icons.DOWNLOAD_ACTIVE,
                Translation.getTranslation("exp_folder_view.synchronize_folder"));
        filesLabel = new JLabel();
        syncPercentLabel = new JLabel();
        totalSizeLabel = new JLabel();
        membersLabel = new JLabel();
        updateNumberOfFiles();
        updateStatsDetails();
        registerListeners();
        updateFolderMembershipDetails();
    }

    /**
     * This should be called if the folder is removed from the repository,
     * so that the listener gets removed.
     */
    public void detatch() {
        folder.removeFolderListener(myFolderListener);
        folder.removeMembershipListener(myFolderMembershipListener);
    }

    private void registerListeners() {
        myFolderListener = new MyFolderListener();
        folder.addFolderListener(myFolderListener);
        myFolderMembershipListener = new MyFolderMembershipListener();
        folder.addMembershipListener(myFolderMembershipListener);
    }

    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    public String getFolderName() {
        return folder.getName();
    }

    private void updateStatsDetails() {
        double sync = folder.getStatistic().getHarmonizedSyncPercentage();
        if (sync < 0) {
            sync = 0;
        }
        if (sync > 100) {
            sync = 100;
        }
        String syncText = Translation.getTranslation(
                "exp_folder_view.synchronized", sync);
        syncPercentLabel.setText(syncText);

        long totalSize = folder.getStatistic().getTotalSize();
        String descriptionKey = "exp_folder_view.total_bytes";
        long divisor = 1;
        if (totalSize >= 1024) {
            divisor *= 1024;
            descriptionKey = "exp_folder_view.total_kilobytes";
        }
        if (totalSize / divisor >= 1024) {
            divisor *= 1024;
            descriptionKey = "exp_folder_view.total_megabytes";
        }
        if (totalSize / divisor >= 1024) {
            divisor *= 1024;
            descriptionKey = "exp_folder_view.total_gigabytes";
        }
        double num;
        if (divisor == 1) {
            num = totalSize;
        } else {
            num = (double) totalSize / (double) divisor;
        }

        DecimalFormat numberFormat = Format.getNumberFormat();
        String formattedNum = numberFormat.format(num);

        totalSizeLabel.setText(Translation.getTranslation(
                descriptionKey, formattedNum));
    }

    private void updateNumberOfFiles() {
        String filesText = Translation.getTranslation("exp_folder_view.files",
                folder.getKnownFilesCount());
        filesLabel.setText(filesText);
    }

    private void updateFolderMembershipDetails() {
        int count = folder.getMembersCount();
        membersLabel.setText(Translation.getTranslation(
                "exp_folder_view.members", count));
    }

    private class MyFolderListener extends FolderAdapter {

        public void statisticsCalculated(FolderEvent folderEvent) {
            updateStatsDetails();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    private class MyFolderMembershipListener implements FolderMembershipListener {

        public void memberJoined(FolderMembershipEvent folderEvent) {
            updateFolderMembershipDetails();
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            updateFolderMembershipDetails();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            boolean exp = expanded.get();
            if (exp) {
                expanded.set(false);
                expandCollapseButton.setIcon(Icons.EXPAND);
                expandCollapseButton.setToolTipText(
                        Translation.getTranslation("exp_folder_view.expand"));
                lowerOuterPanel.setVisible(false);
            } else {
                expanded.set(true);
                expandCollapseButton.setIcon(Icons.COLLAPSE);
                expandCollapseButton.setToolTipText(
                        Translation.getTranslation("exp_folder_view.collapse"));
                lowerOuterPanel.setVisible(true);
            }
        }
    }
}
