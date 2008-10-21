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
* $Id$
*/
package de.dal33t.powerfolder.ui.folder;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileUtil;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Display the synchronisation status of a member for the current folder. Shows
 * information about: - Member name - Folder name - Folder Size - Local Size -
 * Total Sync Percentage (%)
 * 
 * @author <a href="mailto:xsj@users.sourceforge.net"> Daniel Harabor </a>
 * @version 1.0 Last Modified: 21:37:20 8/06/2005
 */

public class MemberSyncStatusPanel extends PFUIComponent {

    private Folder folder;
    private Member member;
    private FolderStatisticListener statsListener;
    private JLabel folderNameLabel;
    private JLabel memberNameLabel;
    private JLabel localSizeLabel;
    private JLabel folderSizeLabel;
    private JLabel totalSyncLabel;
    private JPanel panel;

    public MemberSyncStatusPanel(Controller controller) {
        super(controller);
    }

    /**
     * @return Returns the folder.
     */
    public Folder getFolder() {
        return folder;
    }

    /*
     * private Member getMember() { return member; }
     */

    /**
     * Create the Member Sync information sub-panel
     * 
     * @return
     */
    private JPanel createMemberSyncInfoPanel() {

        JPanel infoPanel = new JPanel();
        FormLayout layout = new FormLayout("right:pref, 7dlu, pref",
            "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");

        PanelBuilder builder = new PanelBuilder(layout, infoPanel);
        CellConstraints cc = new CellConstraints();
        infoPanel.setBackground(Color.WHITE);

        builder.addTitle(Translation
            .getTranslation("folder.members.synchronisation_panel.title"), cc
            .xywh(1, 1, 3, 1));

        builder.addLabel(Translation.getTranslation("general.member"),
            cc.xy(1, 3)).setForeground(Color.BLACK);
        builder.add(memberNameLabel, cc.xy(3, 3));

        builder.addLabel(Translation.getTranslation("general.folder"),
            cc.xy(1, 5)).setForeground(Color.BLACK);
        builder.add(folderNameLabel, cc.xy(3, 5));

        builder
            .addLabel(
                Translation
                    .getTranslation("folder.members.synchronisation_panel.folder_size"),
                cc.xy(1, 7)).setForeground(Color.BLACK);
        builder.add(folderSizeLabel, cc.xy(3, 7));

        builder
            .addLabel(
                Translation
                    .getTranslation("folder.members.synchronisation_panel.local_size"),
                cc.xy(1, 9)).setForeground(Color.BLACK);
        builder.add(localSizeLabel, cc.xy(3, 9));

        builder.addLabel(Translation.getTranslation("folderinfo.total_sync"),
            cc.xy(1, 11)).setForeground(Color.BLACK);
        builder.add(totalSyncLabel, cc.xy(3, 11));

        Color color = UIManager
            .getColor(UIUtil.UIMANAGER_DARK_CONTROL_SHADOW_COLOR_PROPERTY);

        builder.setBorder(BorderFactory.createCompoundBorder(BorderFactory
            .createLineBorder(color), Borders.DLU7_BORDER));
        return builder.getPanel();

    }

    /**
     * Create the Additional Information sub-panel.
     * 
     * @return
     */
    /*
     * private JPanel createAdditionalInfoPanel() { JPanel infoPanel = new
     * JPanel(); FormLayout layout = new FormLayout("right:pref, 7dlu, pref",
     * "p, 3dlu, p, 3dlu, p, 3dlu, p"); PanelBuilder builder = new
     * PanelBuilder(infoPanel, layout); CellConstraints cc = new
     * CellConstraints(); infoPanel.setBackground(Color.WHITE);
     * builder.addTitle("Additional Information"); builder.addLabel("Transfer
     * status", cc.xy(1, 3)).setForeground( Color.BLACK);
     * builder.add(transferDetailsLabel, cc.xy(3, 3)); builder.addLabel("Also
     * joined", cc.xy(1, 5)).setForeground(Color.BLACK);
     * builder.add(folderListArea, cc.xy(3, 5)); builder.addLabel("Network
     * Info", cc.xy(1, 7)) .setForeground(Color.BLACK);
     * builder.add(networkInfoArea, cc.xy(3, 7));
     * builder.setBorder(PowerBorderFactory.createBorder()); return
     * builder.getPanel(); }
     */
    /***************************************************************************
     * returns this ui component, creates it if not available
     * 
     * @return
     */
    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            panel = createMemberSyncInfoPanel();
        }
        return panel;
    }

    /**
     * initialises the display elements of this panel.
     */
    private void initComponents() {
        panel = new JPanel();

        statsListener = new FolderStatisticListener();

        memberNameLabel = SimpleComponentFactory.createLabel();
        memberNameLabel.setForeground(Color.BLACK);
        ensureDims(memberNameLabel);

        folderNameLabel = SimpleComponentFactory.createLabel();
        folderNameLabel.setForeground(Color.BLACK);
        ensureDims(memberNameLabel);

        localSizeLabel = SimpleComponentFactory.createLabel();
        localSizeLabel.setForeground(Color.BLACK);
        ensureDims(localSizeLabel);

        folderSizeLabel = SimpleComponentFactory.createLabel();
        folderSizeLabel.setForeground(Color.BLACK);
        ensureDims(localSizeLabel);

        totalSyncLabel = SimpleComponentFactory.createLabel();
        totalSyncLabel.setForeground(Color.BLACK);
        ensureDims(totalSyncLabel);
    }

    /**
     * The folder which the currently selected member is part of.
     * 
     * @param folder
     */
    public void setFolder(Folder folder) {

        if (folder != null)
            folder.removeFolderListener(statsListener);

        this.folder = folder;
        folder.addFolderListener(statsListener);

        folderNameLabel.setIcon(Icons.FOLDER);

        updatePanel();
    }

    /**
     * The currently selected member about which the sync status is displayed.
     * 
     * @param member
     */
    public void setMember(Member member) {
        this.member = member;

        memberNameLabel.setIcon(Icons.getIconFor(member));
        updatePanel();
    }

    /**
     * Update the panel statistics when a new member has been selected.
     */
    private void updatePanel() {
        if (member != null && folder != null) {
            if (folder.hasMember(member)) {

                FolderStatistic stats = folder.getStatistic();
                int filesRcvd;
                // if (member.isMySelf()) {
                // filesRcvd = stats.getLocalFilesCount();
                // } else {
                filesRcvd = stats.getFilesCountInSync(member);
                // }
                int filesTotal = stats.getTotalFilesCount();
                long bytesRcvd = stats.getSizeInSync(member);
                long bytesTotal = stats.getTotalSize();

                double sync = stats.getSyncPercentage(member);

                memberNameLabel.setText(member.getNick());
                folderNameLabel.setText(folder.getName());

                // bytesTotal = folder.getStatistic().getTotalSize();
                // filesCount = folder.getStatistic().getTotalFilesCount();

                localSizeLabel.setText(filesRcvd + " "
                    + Translation.getTranslation("general.files") + " ("
                    + Format.formatBytes(bytesRcvd) + ")");

                folderSizeLabel.setText(filesTotal + " "
                    + Translation.getTranslation("general.files") + " ("
                    + Format.formatBytes(bytesTotal) + ")");

                totalSyncLabel.setText(SyncProfileUtil
                    .renderSyncPercentage(sync));
                totalSyncLabel.setIcon(SyncProfileUtil.getSyncIcon(sync));
            }
        }
    }

    /**
     * Ensures the preferred height and widht of a component
     * 
     * @param comp
     */
    private void ensureDims(JComponent comp) {
        Dimension dims = comp.getPreferredSize();
        dims.height = Math.max(dims.height, Icons.FOLDER.getIconHeight());
        dims.width = 10;
        comp.setPreferredSize(dims);

    }

    /**
     * FolderStatisticListener Description: Listens for changes to
     * FolderStatistic objects that indicate the state of the current folder has
     * changed in some way.
     * 
     * @author <a href="mailto:xsj@users.sourceforge.net"> Daniel Harabor </a>
     * @version 1.0 Last Modified: 02:20:22 13/06/2005
     */
    private class FolderStatisticListener extends FolderAdapter {

        public void statisticsCalculated(FolderEvent folderEvent) {
            updatePanel();
            if (isFiner()) {
                logFiner(
                    "Updated SyncStatusPanel due to change in: " + folderEvent);
            }
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}