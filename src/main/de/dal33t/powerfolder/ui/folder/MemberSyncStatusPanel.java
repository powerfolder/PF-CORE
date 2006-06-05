package de.dal33t.powerfolder.ui.folder;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FolderList;
import de.dal33t.powerfolder.message.TransferStatus;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Display the synchronisation status of a member for the current folder. Shows
 * information about: - Member name - Folder name - Folder Size - Local Size -
 * Total Sync Percentage (%) - Additional Information (transfer status, network
 * info, other joined folders)
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

    private JTextArea folderListArea;

    private JTextArea networkInfoArea;

    private JPanel panel;

    private JLabel transferDetailsLabel;

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
				.getTranslation("folder.members.synchronisation_panel.title"),
				cc.xywh(1, 1, 3, 1));

		builder.addLabel(Translation.getTranslation("general.member"),
				cc.xy(1, 3)).setForeground(Color.BLACK);
		builder.add(memberNameLabel, cc.xy(3, 3));

        builder.addLabel(Translation.getTranslation("general.folder"), cc.xy(1, 5)).setForeground(Color.BLACK);
        builder.add(folderNameLabel, cc.xy(3, 5));

        builder.addLabel(Translation.getTranslation("folder.members.synchronisation_panel.folder_size"), cc.xy(1, 7)).setForeground(Color.BLACK);
        builder.add(folderSizeLabel, cc.xy(3, 7));

        builder.addLabel(Translation.getTranslation("folder.members.synchronisation_panel.local_size"), cc.xy(1, 9)).setForeground(Color.BLACK);
        builder.add(localSizeLabel, cc.xy(3, 9));

        builder.addLabel(Translation.getTranslation("folderinfo.totalsync"),
            cc.xy(1, 11)).setForeground(Color.BLACK);
        builder.add(totalSyncLabel, cc.xy(3, 11));
        
        Color color = UIManager
            .getColor(Util.UIMANAGER_DARK_CONTROL_SHADOW_COLOR_PROPERTY);

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

        transferDetailsLabel = SimpleComponentFactory.createLabel();
        transferDetailsLabel.setForeground(Color.BLACK);
        ensureDims(transferDetailsLabel);

        folderListArea = new JTextArea();
        folderListArea.setEditable(false);
        folderListArea.setForeground(Color.BLACK);
        ensureDims(folderListArea);

        networkInfoArea = new JTextArea();
        networkInfoArea.setEditable(false);
        networkInfoArea.setForeground(Color.BLACK);
        ensureDims(networkInfoArea);

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

        folderNameLabel.setIcon(Icons.getIconFor(getController(), folder
            .getInfo()));

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
                if (member.isMySelf()) {
                    filesRcvd = stats.getTotalNormalFilesCount();
                } else {
                    filesRcvd = stats.getFilesCount(member);
                }
                int filesTotal = stats.getTotalFilesCount();
                long bytesRcvd = stats.getSize(member);
                long bytesTotal = stats.getTotalSize();

                double totalSync = stats.getSyncPercentage(member);

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

                totalSyncLabel.setText(Format.NUMBER_FORMATS.format(totalSync)
                    + " %");
                totalSyncLabel.setIcon(Icons.getSyncIcon(totalSync));

                transferDetailsLabel.setText(appendTransferStatus());

                folderListArea.setText(getMemberFolderList());

                networkInfoArea.setText(getMemberNetworkInfo());

            }

        }
    }

    /**
     * Transfer status details
     * 
     * @return
     */
    private String appendTransferStatus() {

        String text;

        if (!member.isMySelf()) {

            TransferStatus lastStatus = member.getLastTransferStatus();
            text = "\n\n";
            if (lastStatus != null) {
                text += lastStatus.toString();
            } else {
                text += "Transfers status: - not available";
                if (member.isConnected()) {
                    text += " yet";
                }
                text += " -";
                return text;
            }
        }
        return null;
    }

    /**
     * Returns other folders that the current member has also joined.
     * 
     * @return
     */
    private String getMemberFolderList() {
        String text = "";

        FolderList fList = member.getLastFolderList();
        // text = member.toString();

        text += "\n\nMember of following folders:";
        if (fList != null) {
            for (int i = 0; i < fList.folders.length; i++) {
                FolderInfo fInfo = fList.folders[i];
                if (getController().isVerbose()
                    || !fInfo.secret
                    || getController().getFolderRepository().hasJoinedFolder(
                        fInfo))
                {
                    // only if public or secret folder we know
                    text += "\n  " + fInfo;
                    if (getController().isVerbose()) {
                        text += ", id: " + fInfo.id;
                    }
                }
            }
            if (getController().isVerbose() && fList.secretFolders != null
                && fList.secretFolders.length > 0)
            {
                text += "\n   --secret--";
                for (int i = 0; i < fList.secretFolders.length; i++) {
                    FolderInfo fInfo = fList.secretFolders[i];
                    text += "\n  " + fInfo;
                    if (getController().isVerbose()) {
                        text += ", id: " + fInfo.id;
                    }
                }
            }
        }

        return text;
    }

    /**
     * Returns information about the network connection (LAN/Internet) of the
     * currently selected member and their IP address.
     * 
     * @return
     */
    private String getMemberNetworkInfo() {
        String text = "";

        if (getController().isVerbose()) {
            text += "\nNodeId: " + member.getId();
            text += "\nLast connect: " + member.getLastConnectTime();
        }
        text += "\n\nNetwork: " + (member.isOnLAN() ? "LAN" : "i-net");
        text += ", reconnect at " + member.getReconnectAddress();

        return text;
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
    private class FolderStatisticListener implements FolderListener {
        public void remoteContentsChanged(FolderEvent folderEvent) {
        }

        public void folderChanged(FolderEvent folderEvent) {
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            updatePanel();
            log().verbose(
                "Updated SyncStatusPanel due to change in: " + folderEvent);
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
        }
    }
}