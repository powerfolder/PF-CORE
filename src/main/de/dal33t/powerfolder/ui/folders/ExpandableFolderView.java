/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: ExpandableFolderView.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.folders;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.ActionModel;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class to render expandable view of a folder.
 */
public class ExpandableFolderView extends PFUIComponent {

    private final Folder folder;
    private JButtonMini expandCollapseButton;
    private JButtonMini syncFolderButton;
    private JPanel uiComponent;
    private JPanel lowerOuterPanel;
    private AtomicBoolean expanded;

    private JLabel filesLabel;
    private JButtonMini openSettingsInformationButton;
    private JButtonMini openFilesInformationButton;
    private JButtonMini openComputersInformationButton;
    private JLabel syncPercentLabel;
    private JLabel totalSizeLabel;
    private JLabel membersLabel;
    private JLabel filesAvailableLabel;

    private MyFolderListener myFolderListener;
    private MyFolderMembershipListener myFolderMembershipListener;

    /**
     * Constructor
     *
     * @param controller
     * @param folder
     */
    public ExpandableFolderView(Controller controller, Folder folder) {
        super(controller);
        this.folder = folder;
    }

    /**
     * Gets the ui component, building if required.
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    /**
     * Builds the ui component.
     */
    private void buildUI() {

        initComponent();

        // Build ui
                                            //  icon        name   space            # files     sync        ex/co
        FormLayout upperLayout = new FormLayout("pref, 3dlu, pref, pref:grow, 3dlu, pref, 3dlu, pref, 3dlu, pref",
            "pref");
        PanelBuilder upperBuilder = new PanelBuilder(upperLayout);
        CellConstraints cc = new CellConstraints();

        JLabel jLabel;
        if (folder.isPreviewOnly()) {
            jLabel = new JLabel(Icons.PF_PREVIEW);
            jLabel.setToolTipText(Translation.getTranslation(
                    "exp_folder_view.folder_preview_text"));
        } else {
            jLabel = new JLabel(Icons.PF_LOCAL);
            jLabel.setToolTipText(Translation.getTranslation(
                    "exp_folder_view.folder_local_text"));
        }
        upperBuilder.add(jLabel, cc.xy(1, 1));
        upperBuilder.add(new JLabel(folder.getName()), cc.xy(3, 1));
        upperBuilder.add(filesAvailableLabel, cc.xy(6, 1));
        upperBuilder.add(syncFolderButton, cc.xy(8, 1));
        upperBuilder.add(expandCollapseButton, cc.xy(10, 1));

        JPanel upperPanel = upperBuilder.getPanel();

        // Build lower detials with line border.
        FormLayout lowerLayout = new FormLayout("3dlu, pref, pref:grow, 3dlu, pref, 3dlu",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");
        PanelBuilder lowerBuilder = new PanelBuilder(lowerLayout);

        String transferMode = Translation.getTranslation("exp_folder_view.transfer_mode",
                folder.getSyncProfile().getProfileName());
        lowerBuilder.addSeparator(null, cc.xywh(2, 1, 4, 1));
        
        lowerBuilder.add(new JLabel(transferMode), cc.xy(2, 3));
        lowerBuilder.add(openSettingsInformationButton, cc.xy(5, 3));

        lowerBuilder.addSeparator(null, cc.xywh(2, 5, 4, 1));

        lowerBuilder.add(syncPercentLabel, cc.xy(2, 7));
        lowerBuilder.add(openFilesInformationButton, cc.xy(5, 7));

        lowerBuilder.add(filesLabel, cc.xy(2, 9));

        lowerBuilder.add(totalSizeLabel, cc.xy(2, 11));

        lowerBuilder.addSeparator(null, cc.xywh(2, 13, 4, 1));

        lowerBuilder.add(membersLabel, cc.xy(2, 15));
        lowerBuilder.add(openComputersInformationButton, cc.xy(5, 15));

        JPanel lowerPanel = lowerBuilder.getPanel();

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

    /**
     * Initializes the components.
     */
    private void initComponent() {
        expanded = new AtomicBoolean();

        ActionModel actionModel = getApplicationModel().getActionModel();
        
        openSettingsInformationButton = new JButtonMini(actionModel
                .getOpenSettingsInformationAction());
        openSettingsInformationButton.addActionListener(
                new MyOpenSettingsInformationActionListener());

        openFilesInformationButton = new JButtonMini(actionModel
                .getOpenFilesInformationAction());
        openFilesInformationButton.addActionListener(
                new MyOpenFilesInformationActionListener());

        openComputersInformationButton = new JButtonMini(actionModel
                .getOpenMembersInformationAction());
        openComputersInformationButton.addActionListener(
                new MyOpenMembersInformationActionListener());

        expandCollapseButton = new JButtonMini(Icons.EXPAND,
                Translation.getTranslation("exp_folder_view.expand"));
        expandCollapseButton.addActionListener(new MyExpColActionListener());
        syncFolderButton = new JButtonMini(Icons.SYNC,
                Translation.getTranslation("exp_folder_view.synchronize_folder"));
        filesLabel = new JLabel();
        syncPercentLabel = new JLabel();
        totalSizeLabel = new JLabel();
        membersLabel = new JLabel();
        filesAvailableLabel = new JLabel();

        updateNumberOfFiles();
        updateStatsDetails();
        updateFolderMembershipDetails();

        registerListeners();
    }

    /**
     * Class finalization. Required to unregister listeners.
     *
     * @throws Throwable
     */
    protected void finalize() throws Throwable {
        unregisterListeners();
        super.finalize();
    }

    /**
     * Register listeners of the folder.
     */
    private void registerListeners() {
        myFolderListener = new MyFolderListener();
        folder.addFolderListener(myFolderListener);
        myFolderMembershipListener = new MyFolderMembershipListener();
        folder.addMembershipListener(myFolderMembershipListener);
    }

    /**
     * Unregister listeners of the folder.
     */
    private void unregisterListeners() {
        folder.removeFolderListener(myFolderListener);
        folder.removeMembershipListener(myFolderMembershipListener);
    }

    /**
     * Gets the name of the associated folder.
     * @return
     */
    public FolderInfo getFolderInfo() {
        return folder.getInfo();
    }

    /**
     * Updates the statistics details of the folder.
     */
    private void updateStatsDetails() {
        FolderStatistic statistic = folder.getStatistic();
        double sync = statistic.getHarmonizedSyncPercentage();
        if (sync < 0) {
            sync = 0;
        }
        if (sync > 100) {
            sync = 100;
        }
        String syncText = Translation.getTranslation(
                "exp_folder_view.synchronized", sync);
        syncPercentLabel.setText(syncText);

        long totalSize = statistic.getTotalSize();
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
        int count = statistic.getIncomingFilesCount();
        if (count == 0) {
            filesAvailableLabel.setText("");
        } else {
            filesAvailableLabel.setText(Translation.getTranslation(
                    "exp_folder_view.files_available", count));
        }
    }

    /**
     * Updates the number of files details of the folder.
     */
    private void updateNumberOfFiles() {
        String filesText = Translation.getTranslation("exp_folder_view.files",
                folder.getKnownFilesCount());
        filesLabel.setText(filesText);
    }

    /**
     * Updates the folder member details.
     */
    private void updateFolderMembershipDetails() {
        int count = folder.getMembersCount();
        membersLabel.setText(Translation.getTranslation(
                "exp_folder_view.members", count));
    }

    /**
     * Class to respond to folder events.
     */
    private class MyFolderListener implements FolderListener {

        public void statisticsCalculated(FolderEvent folderEvent) {
            updateStatsDetails();
        }

        public void fileChanged(FolderEvent folderEvent) {
        }

        public void filesDeleted(FolderEvent folderEvent) {
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
        }

        public void scanResultCommited(FolderEvent folderEvent) {
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Class to respond to folder membership events.
     */
    private class MyFolderMembershipListener implements FolderMembershipListener {

        public void memberJoined(FolderMembershipEvent folderEvent) {
            updateFolderMembershipDetails();
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            updateFolderMembershipDetails();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Class to respond to expand / collapse events.
     */
    private class MyExpColActionListener implements ActionListener {
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

    private class MyOpenSettingsInformationActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ActionEvent ae = new ActionEvent(ExpandableFolderView.this,
                    (int) e.getWhen(), e.getActionCommand(), e.getModifiers());
            getApplicationModel().getActionModel()
                    .getOpenSettingsInformationAction().actionPerformed(ae);
        }
    }

    private class MyOpenFilesInformationActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ActionEvent ae = new ActionEvent(ExpandableFolderView.this,
                    (int) e.getWhen(), e.getActionCommand(), e.getModifiers());
            getApplicationModel().getActionModel()
                    .getOpenFilesInformationAction().actionPerformed(ae);
        }
    }

    private class MyOpenMembersInformationActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ActionEvent ae = new ActionEvent(ExpandableFolderView.this,
                    (int) e.getWhen(), e.getActionCommand(), e.getModifiers());
            getApplicationModel().getActionModel()
                    .getOpenMembersInformationAction().actionPerformed(ae);
        }
    }
}
