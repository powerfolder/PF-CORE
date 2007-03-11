package de.dal33t.powerfolder.ui;

import java.awt.Component;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ui.HasUIPanel;

/**
 * Displays some network statistics and file statistics. 
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * 
 * @version $Revision: 1.8 $
 */
public class NetworkStatisticsPanel extends PFUIComponent implements HasUIPanel {

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
        return Translation.getTranslation("title.network.statistics");
    }

    private void initComponents() {
        NodeManager nodeManager = getController().getNodeManager();
        nodeManager.addNodeManagerListener(new MyNodeManagerListener());
        FolderRepository repo = getController().getFolderRepository();
        repo.addFolderRepositoryListener(new MyFolderRepositoryListener());
        connectedUsers = new JLabel();
        onlineUsers = new JLabel();
        knownUsers = new JLabel();
        publicFolderCount = new JLabel();
        localFolderCount = new JLabel();
        publicFoldersSize = new JLabel();
        localFoldersSize = new JLabel();
        numberOfLocalFiles = new JLabel();
        numberOfPublicFiles = new JLabel();

        FormLayout layout = new FormLayout(
            "4dlu, pref, 4dlu, pref",
            "4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation
            .getTranslation("networkstatisticspanel.connected_users")), cc.xy(
            2, 2));
        builder.add(connectedUsers, cc.xy(4, 2));

        builder.add(new JLabel(Translation
            .getTranslation("networkstatisticspanel.online_users")), cc
            .xy(2, 4));
        builder.add(onlineUsers, cc.xy(4, 4));
        
        builder
            .add(new JLabel(Translation
                .getTranslation("networkstatisticspanel.known_users")), cc.xy(
                2, 6));
        builder.add(knownUsers, cc.xy(4, 6));
        
        builder.add(new JLabel(Translation
            .getTranslation("networkstatisticspanel.local_folder_count")), cc
            .xy(2, 8));
        builder.add(localFolderCount, cc.xy(4, 8));

        builder.add(new JLabel(Translation
            .getTranslation("networkstatisticspanel.number_of_local_files")),
            cc.xy(2, 10));
        builder.add(numberOfLocalFiles, cc.xy(4, 10));
        
        builder.add(new JLabel(Translation
            .getTranslation("networkstatisticspanel.local_folders_size")), cc
            .xy(2, 12));
        builder.add(localFoldersSize, cc.xy(4, 12));
        
        builder.add(new JLabel(Translation
            .getTranslation("networkstatisticspanel.public_folder_count")), cc
            .xy(2, 14));
        builder.add(publicFolderCount, cc.xy(4, 14));

        builder.add(new JLabel(Translation
            .getTranslation("networkstatisticspanel.number_of_public_files")),
            cc.xy(2, 16));
        builder.add(numberOfPublicFiles, cc.xy(4, 16));

        builder.add(new JLabel(Translation
            .getTranslation("networkstatisticspanel.public_folders_size")), cc
            .xy(2, 18));
        builder.add(publicFoldersSize, cc.xy(4, 18));

        networkStatsPanel = builder.getPanel();
    }

    private void update() {
        int connected = getController().getNodeManager().countConnectedNodes();
        int online = getController().getNodeManager().countOnlineNodes();
        int known = getController().getNodeManager().countNodes();

        connectedUsers.setText(connected + "");
        onlineUsers.setText(online + "");
        knownUsers.setText(known + "");

        FolderRepository repo = getController().getFolderRepository();
        //List list = repo.getUnjoinedFoldersList();
        long totalUnjoinedBytes = 0;
        long totalUnjoinedFiles = 0;
//        for (int i = 0; i < list.size(); i++) {
//            FolderInfo folderInfo = (FolderInfo) list.get(i);
//            totalUnjoinedBytes += folderInfo.bytesTotal;
//            totalUnjoinedFiles += folderInfo.filesCount;
//        }
        Folder[] folders = repo.getFolders();

        long totalJoinedFiles = 0;
        long totalJoinedBytes = 0;
        for (int i = 0; i < folders.length; i++) {
            totalJoinedFiles += folders[i].getFilesCount();
            totalJoinedBytes += folders[i].getStatistic().getSize(
                getController().getMySelf());
        }
        publicFoldersSize.setText(Format.formatBytes(totalUnjoinedBytes));
        localFoldersSize.setText(Format.formatBytes(totalJoinedBytes));
        numberOfPublicFiles.setText(Format.formatLong(totalUnjoinedFiles));
        numberOfLocalFiles.setText(Format.formatLong(totalJoinedFiles));

       // int publicFolders = list.size();
        int localFolders = repo.getFoldersCount();

        publicFolderCount.setText("n/a");
        localFolderCount.setText(localFolders + "");

    }

    private class MyNodeManagerListener implements NodeManagerListener {

        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
        }

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

        public void settingsChanged(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
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

        public void unjoinedFolderAdded(FolderRepositoryEvent e) {
            update();
        }

        public void unjoinedFolderRemoved(FolderRepositoryEvent e) {
            update();
        }
        
        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
