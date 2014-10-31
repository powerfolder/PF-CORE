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
package de.dal33t.powerfolder.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Displays some network statistics and file statistics.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.8 $
 */
public class NetworkStatisticsPanel extends PFUIComponent {

    private JPanel panel;
    private JPanel networkStatsPanel;
    private JLabel connectedUsers;
    private JLabel onlineUsers;
    private JLabel knownUsers;
    private JLabel publicFolderCount;
    private JLabel localFolderCount;
    private JLabel publicFoldersSize;
    private JLabel localFoldersSize;
    private JLabel numberOfPublicFiles;
    private JLabel numberOfLocalFiles;
    private JLabel reconnectionQueueSize;
    private JButton updateButton;

    public NetworkStatisticsPanel(Controller controller) {
        super(controller);
    }

    /** returns this ui component, creates it if not available * */
    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout("fill:pref:grow",
                "fill:pref:grow, pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(networkStatsPanel, cc.xy(1, 1));
            // builder.addSeparator(null, cc.xy(1, 2));
            // builder.add(toolbar, cc.xy(1, 3));

            panel = builder.getPanel();
        }
        update();
        return panel;
    }

    public String getTitle() {
        return Translation.getTranslation("exp.network.statistics.title");
    }

    private void initComponents() {
        NodeManager nodeManager = getController().getNodeManager();
        // nodeManager.addNodeManagerListener(new MyNodeManagerListener());
        FolderRepository repo = getController().getFolderRepository();
        // repo.addFolderRepositoryListener(new MyFolderRepositoryListener());
        connectedUsers = new JLabel();
        onlineUsers = new JLabel();
        knownUsers = new JLabel();
        publicFolderCount = new JLabel();
        localFolderCount = new JLabel();
        publicFoldersSize = new JLabel();
        localFoldersSize = new JLabel();
        numberOfLocalFiles = new JLabel();
        numberOfPublicFiles = new JLabel();
        reconnectionQueueSize = new JLabel();
        updateButton = new JButton(Translation.getTranslation("general.update"));
        updateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                update();
            }
        });

        FormLayout layout = new FormLayout(
            "pref, 3dlu, pref",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 6dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.add(new JLabel(Translation
            .getTranslation("exp.network_statistics_panel.connected_computers")),
            cc.xy(1, row));
        builder.add(connectedUsers, cc.xy(3, row));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("exp.network_statistics_panel.online_computers")), cc
            .xy(1, row));
        builder.add(onlineUsers, cc.xy(3, row));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("exp.network_statistics_panel.known_computers")), cc
            .xy(1, row));
        builder.add(knownUsers, cc.xy(3, row));

        row += 2;
        builder
            .add(
                new JLabel(
                    Translation
                        .getTranslation("exp.network_statistics_panel.reconnection_queue_size")),
                cc.xy(1, row));
        builder.add(reconnectionQueueSize, cc.xy(3, row));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("exp.network_statistics_panel.local_folder_count")), cc
            .xy(1, row));
        builder.add(localFolderCount, cc.xy(3, row));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("exp.network_statistics_panel.number_of_local_files")),
            cc.xy(1, row));
        builder.add(numberOfLocalFiles, cc.xy(3, row));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("exp.network_statistics_panel.local_folders_size")), cc
            .xy(1, row));
        builder.add(localFoldersSize, cc.xy(3, row));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("exp.network_statistics_panel.public_folder_count")),
            cc.xy(1, row));
        builder.add(publicFolderCount, cc.xy(3, row));

        row += 2;
        builder
            .add(
                new JLabel(
                    Translation
                        .getTranslation("exp.network_statistics_panel.number_of_public_files")),
                cc.xy(1, row));
        builder.add(numberOfPublicFiles, cc.xy(3, row));

        row += 2;
        builder.add(new JLabel(Translation
            .getTranslation("exp.network_statistics_panel.public_folders_size")),
            cc.xy(1, row));
        builder.add(publicFoldersSize, cc.xy(3, row));

        row += 2;
        builder.add(updateButton, cc.xy(3, row));

        networkStatsPanel = builder.getPanel();
    }

    private void update() {
        int connected = getController().getNodeManager().countConnectedNodes();
        int sConnected = getController().getNodeManager()
            .countConnectedSupernodes();
        int online = getController().getNodeManager().countOnlineNodes();
        int sOnline = getController().getNodeManager().countOnlineSupernodes();
        int known = getController().getNodeManager().getNodesAsCollection()
            .size();
        int sKnown = getController().getNodeManager().countSupernodes();
        int nDontConnect = 0;
        int nDirectConnect = 0;
        for (Member node : getController().getNodeManager()
            .getNodesAsCollection())
        {
            if (node.isDontConnect()) {
                nDontConnect++;
            }
            if (node.isUnableToConnect()) {
                nDirectConnect++;
            }
        }

        connectedUsers.setText(connected + " (" + sConnected + ") X: "
            + nDirectConnect + " R: " + nDontConnect + ' ');
        onlineUsers.setText(online + " (" + sOnline + ')');
        knownUsers.setText(known + " (" + sKnown + ')');
        reconnectionQueueSize.setText(""
            + getController().getReconnectManager().countReconnectionQueue());

        FolderRepository repo = getController().getFolderRepository();
        // List list = repo.getUnjoinedFoldersList();
        // for (int i = 0; i < list.size(); i++) {
        // FolderInfo folderInfo = (FolderInfo) list.get(i);
        // totalUnjoinedBytes += folderInfo.bytesTotal;
        // totalUnjoinedFiles += folderInfo.filesCount;
        // }
        Collection<Folder> folders = repo.getFolders();

        long totalJoinedFiles = 0;
        long totalJoinedBytes = 0;
        for (Folder folder : folders) {
            totalJoinedFiles += folder.getKnownItemCount();
            totalJoinedBytes += folder.getStatistic().getSize(
                getController().getMySelf());
        }
        long totalUnjoinedBytes = 0;
        publicFoldersSize.setText(Format.formatBytes(totalUnjoinedBytes));
        localFoldersSize.setText(Format.formatBytes(totalJoinedBytes));
        long totalUnjoinedFiles = 0;
        numberOfPublicFiles.setText(Format.formatLong(totalUnjoinedFiles));
        numberOfLocalFiles.setText(Format.formatLong(totalJoinedFiles));

        // int publicFolders = list.size();
        int localFolders = repo.getFoldersCount();

        publicFolderCount.setText("n/a");
        localFolderCount.setText(localFolders + "");
    }

    private class MyNodeManagerListener extends NodeManagerAdapter {

        public void nodeAdded(NodeManagerEvent e) {
            update();
        }

        public void nodeConnected(NodeManagerEvent e) {
            update();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            update();
        }

        public void nodeRemoved(NodeManagerEvent e) {
            update();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

        public void folderCreated(FolderRepositoryEvent e) {
            update();
        }

        public void folderRemoved(FolderRepositoryEvent e) {
            update();
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            update();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            update();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
