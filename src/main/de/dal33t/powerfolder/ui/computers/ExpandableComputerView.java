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
package de.dal33t.powerfolder.ui.computers;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionQuality;
import de.dal33t.powerfolder.security.SecurityManagerEvent;
import de.dal33t.powerfolder.security.SecurityManagerListener;
import de.dal33t.powerfolder.ui.ExpandableView;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.ConnectDialog;
import de.dal33t.powerfolder.ui.event.ExpansionEvent;
import de.dal33t.powerfolder.ui.event.ExpansionListener;
import de.dal33t.powerfolder.ui.util.CursorUtils;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Class to render expandable view of a folder.
 */
public class ExpandableComputerView extends PFUIComponent implements
    ExpandableView
{

    private final Member node;
    private JPanel uiComponent;
    private JPanel lowerOuterPanel;
    private AtomicBoolean expanded;
    private JLabel infoLabel;
    private JButtonMini reconnectButton;
    private JButtonMini addRemoveButton;
    private JButtonMini pictoLabel;
    private JPanel upperPanel;
    private MyAddRemoveFriendAction addRemoveFriendAction;
    private MyReconnectAction reconnectAction;
    private JLabel lastSeenLabel;
    private JLabel usernameLabel;
    private JLabel versionLabel;
    private MyNodeManagerListener nodeManagerListener;
    private MySecurityManagerListener secManagerListener;

    private ExpansionListener listenerSupport;

    private JPopupMenu contextMenu;
    private JPanel borderPanel;
    private final AtomicBoolean focus = new AtomicBoolean();

    /**
     * Constructor
     *
     * @param controller
     * @param node
     */
    public ExpandableComputerView(Controller controller, Member node) {
        super(controller);
        listenerSupport = ListenerSupportFactory
            .createListenerSupport(ExpansionListener.class);
        this.node = node;
    }

    /**
     * Expand this view if collapsed.
     */
    public void expand() {
        expanded.set(true);
        upperPanel.setToolTipText(Translation
            .getTranslation("exp.exp_computer_view.collapse"));
        lowerOuterPanel.setVisible(true);
        updateBorderPanel();
        listenerSupport.resetAllButSource(new ExpansionEvent(this));
    }

    /**
     * Collapse this view if expanded.
     */
    public void collapse() {
        expanded.set(false);
        upperPanel.setToolTipText(Translation
            .getTranslation("exp.exp_computer_view.expand"));
        lowerOuterPanel.setVisible(false);
        updateBorderPanel();
    }

    public void setFocus(boolean focus) {
        this.focus.set(focus);
        updateBorderPanel();
    }

    public boolean hasFocus() {
        return focus.get();
    }

    private void updateBorderPanel() {
        if (focus.get()) {
            borderPanel.setBorder(BorderFactory.createEtchedBorder());
        } else {
            borderPanel.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    /**
     * Gets the ui component, building if required.
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    public boolean isExpanded() {
        return expanded.get();
    }

    /**
     * Builds the ui component.
     */
    private void buildUI() {

        initComponent();

        // Build ui
        FormLayout upperLayout = new FormLayout(
            "pref, 3dlu, pref, pref:grow, 3dlu, pref", "pref");
        PanelBuilder upperBuilder = new PanelBuilder(upperLayout);
        CellConstraints cc = new CellConstraints();

        upperBuilder.add(pictoLabel, cc.xy(1, 1));
        upperBuilder.add(infoLabel, cc.xy(3, 1));

        upperPanel = upperBuilder.getPanel();
        upperPanel.setOpaque(false);
        upperPanel.setToolTipText(Translation
            .getTranslation("exp.exp_computer_view.expand"));
        MouseAdapter ma = new MyMouseAdapter();
        upperPanel.addMouseListener(ma);
        CursorUtils.setHandCursor(upperPanel);
        pictoLabel.addActionListener(new PrimaryButtonActionListener());

        // Build lower details with line border.
        // last, qual rmve recon
        FormLayout lowerLayout = new FormLayout(
            "pref, pref:grow, 3dlu, pref, 2dlu, pref, 2dlu, pref",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
        // sep, last
        PanelBuilder lowerBuilder = new PanelBuilder(lowerLayout);
        lowerBuilder.setBorder(Borders.createEmptyBorder("0, 3dlu, 0, 3dlu"));

        lowerBuilder.addSeparator(null, cc.xyw(1, 1, 8));

        lowerBuilder.add(usernameLabel, cc.xy(1, 3));
        lowerBuilder.add(addRemoveButton, cc.xywh(6, 3, 1, 3));
        lowerBuilder.add(reconnectButton, cc.xywh(8, 3, 1, 3));
        lowerBuilder.add(lastSeenLabel, cc.xy(1, 5));
        if (getController().isVerbose()) {
            lowerBuilder.appendRow("3dlu");
            lowerBuilder.appendRow("pref");
            lowerBuilder.add(versionLabel, cc.xy(1, 7));
        }

        JPanel lowerPanel = lowerBuilder.getPanel();
        lowerPanel.setOpaque(false);

        // Build spacer then lower outer with lower panel
        FormLayout lowerOuterLayout = new FormLayout("pref:grow", "3dlu, pref");
        PanelBuilder lowerOuterBuilder = new PanelBuilder(lowerOuterLayout);
        lowerOuterPanel = lowerOuterBuilder.getPanel();
        lowerOuterPanel.setOpaque(false);
        lowerOuterPanel.setVisible(false);
        lowerOuterBuilder.add(lowerPanel, cc.xy(1, 2));

        // Build border around upper and lower
        FormLayout borderLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "3dlu, pref, pref, 3dlu");
        PanelBuilder borderBuilder = new PanelBuilder(borderLayout);
        borderBuilder.add(upperPanel, cc.xy(2, 2));
        JPanel panel = lowerOuterBuilder.getPanel();
        panel.setOpaque(false);
        borderBuilder.add(panel, cc.xy(2, 3));
        borderPanel = borderBuilder.getPanel();
        borderPanel.setOpaque(false);

        // Build ui with vertical space before the next one.
        FormLayout outerLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "pref, 3dlu");
        PanelBuilder outerBuilder = new PanelBuilder(outerLayout);
        outerBuilder.add(borderPanel, cc.xy(2, 1));

        uiComponent = outerBuilder.getPanel();
        uiComponent.setOpaque(false);

        uiComponent.setTransferHandler(new MyTransferHandler());
    }

    /**
     * Initializes the components.
     */
    private void initComponent() {
        expanded = new AtomicBoolean();
        infoLabel = new JLabel(node.getNick());
        lastSeenLabel = new JLabel();
        AccountInfo aInfo = node.getAccountInfo();
        usernameLabel = new JLabel(aInfo != null
            ? aInfo.getScrabledDisplayName()
            : null);
        versionLabel = new JLabel(Translation.getTranslation(
            "exp.exp_computer_view.version", ""));
        reconnectAction = new MyReconnectAction(getController());
        reconnectButton = new JButtonMini(reconnectAction);
        addRemoveFriendAction = new MyAddRemoveFriendAction(getController());
        addRemoveButton = new JButtonMini(addRemoveFriendAction);
        pictoLabel = new JButtonMini(Icons.getIconById(Icons.BLANK), "");
        updateDetails();
        configureAddRemoveButton();
        registerListeners();
    }

    /**
     * Call this to unregister listeners if computer is being removed.
     */
    public void removeCoreListeners() {
        getController().getNodeManager().removeNodeManagerListener(
            nodeManagerListener);
        getController().getSecurityManager().removeListener(secManagerListener);
    }

    /**
     * Register listeners of the folder.
     */
    private void registerListeners() {
        nodeManagerListener = new MyNodeManagerListener();
        getController().getNodeManager().addNodeManagerListener(
            nodeManagerListener);
        secManagerListener = new MySecurityManagerListener();
        getController().getSecurityManager().addListener(secManagerListener);
    }

    /**
     * Gets the name of the associated folder.
     *
     * @return
     */
    public Member getNode() {
        return node;
    }

    /**
     * Updates the displayed details if for this member.
     *
     * @param eventNode
     */
    private void updateDetailsIfRequired(Member eventNode) {
        if (node == null) {
            return;
        }
        if (node.equals(eventNode)) {
            updateDetails();
            configureAddRemoveButton();
        }
    }

    /**
     * Configure the add / remove button on node change.
     */
    private void configureAddRemoveButton() {

        if (node.isFriend()) {
            addRemoveFriendAction.setAdd(false);
        } else {
            addRemoveFriendAction.setAdd(true);
        }
        addRemoveButton.configureFromAction(addRemoveFriendAction);
    }

    /**
     * Updates the displayed details of the member.
     */
    private void updateDetails() {

        if (node.isCompletelyConnected()) {
            lastSeenLabel.setText(Translation
                .getTranslation("exp.exp_computer_view.connected_text"));
        } else if (node.isConnecting()) {
            lastSeenLabel.setText(Translation
                .getTranslation("exp.exp_computer_view.connecting_text"));
        } else {
            Date time = node.getLastConnectTime();
            String lastConnectedTime;
            if (time == null) {
                lastConnectedTime = "";
            } else {
                lastConnectedTime = Format.formatDateShort(time);
            }
            lastSeenLabel.setText(Translation.getTranslation(
                "exp.exp_computer_view.last_seen_text", lastConnectedTime));
        }

        AccountInfo aInfo = node.getAccountInfo();
        if (aInfo != null && !node.isServer()) {
            usernameLabel.setText(Translation.getTranslation(
                "exp.exp_computer_view.account", aInfo.getScrabledDisplayName()));
        } else if (node.isServer()) {
            usernameLabel.setText("");
        } else {
            usernameLabel.setText(Translation
                .getTranslation("exp.exp_computer_view.no_login"));
        }

        if (getController().isVerbose()) {
            Identity id = node.getIdentity();
            if (id != null) {
                versionLabel.setText(Translation.getTranslation(
                    "exp.exp_computer_view.version", id.getProgramVersion()));
            }
        }

        String iconName;
        String text;
        if (node.isCompletelyConnected()) {
            ConnectionHandler peer = node.getPeer();
            iconName = Icons.NODE_CONNECTED;
            text = Translation
                .getTranslation("exp.exp_computer_view.node_friend_connected_text");
            if (node.isOnLAN()) {
                iconName = Icons.NODE_LAN;
                text = Translation.getTranslation("exp.connection_lan.text");
            } else if (peer != null) {
                ConnectionQuality quality = peer.getConnectionQuality();
                if (quality != null) {
                    switch (quality) {
                        case GOOD :
                            iconName = Icons.NODE_CONNECTED;
                            text = Translation
                                .getTranslation("exp.connection_quality_good.text");
                            break;
                        case MEDIUM :
                            iconName = Icons.NODE_MEDIUM;
                            text = Translation
                                .getTranslation("exp.connection_quality_medium.text");
                            break;
                        case POOR :
                            iconName = Icons.NODE_POOR;
                            text = Translation
                                .getTranslation("exp.connection_quality_poor.text");
                            break;
                    }
                }
            }
        } else if (node.isConnecting()) {
            iconName = Icons.NODE_CONNECTING;
            text = Translation
                .getTranslation("exp.exp_computer_view.node_connecting_text");
        } else {
            iconName = Icons.NODE_DISCONNECTED;
            text = Translation
                .getTranslation("exp.exp_computer_view.node_friend_disconnected_text");
        }
        pictoLabel.setIcon(Icons.getIconById(iconName));
        pictoLabel.setToolTipText(text);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ExpandableComputerView that = (ExpandableComputerView) obj;

        return node.equals(that.node);

    }

    public int hashCode() {
        return node.hashCode();
    }

    /**
     * Add an expansion listener.
     *
     * @param listener
     */
    public void addExpansionListener(ExpansionListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    /**
     * Remove an expansion listener.
     *
     * @param listener
     */
    public void removeExpansionListener(ExpansionListener listener) {
        ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

    public JPopupMenu createPopupMenu() {
        if (contextMenu == null) {
            contextMenu = new JPopupMenu();
            contextMenu.add(addRemoveFriendAction).setIcon(null);
            contextMenu.add(reconnectAction).setIcon(null);
        }
        return contextMenu;
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

    /**
     * Class to respond to expand / collapse events.
     */
    private class MyMouseAdapter extends MouseAdapter {

        private volatile boolean mouseOver;

        // Auto expand if user hovers for two seconds.
        public void mouseEntered(MouseEvent e) {
            if (PreferencesEntry.AUTO_EXPAND.getValueBoolean(getController())) {
                mouseOver = true;
                if (!expanded.get()) {
                    getController().schedule(new TimerTask() {
                        public void run() {
                            if (mouseOver) {
                                if (!expanded.get()) {
                                    expand();
                                }
                            }
                        }
                    }, 2000);
                }
            }
        }

        public void mouseExited(MouseEvent e) {
            mouseOver = false;
        }

        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            createPopupMenu().show(evt.getComponent(), evt.getX(), evt.getY());
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                setFocus(true);
                if (expanded.get()) {
                    collapse();
                } else {
                    expand();
                }
            }
        }
    }

    /**
     * Listener of node events.
     */
    private class MyNodeManagerListener extends NodeManagerAdapter {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void friendAdded(NodeManagerEvent e) {
            updateDetailsIfRequired(e.getNode());
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateDetailsIfRequired(e.getNode());
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnecting(NodeManagerEvent e) {
            updateDetailsIfRequired(e.getNode());
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateDetailsIfRequired(e.getNode());
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateDetailsIfRequired(e.getNode());
        }

        public void nodeOnline(NodeManagerEvent e) {
            updateDetailsIfRequired(e.getNode());
        }

        public void nodeOffline(NodeManagerEvent e) {
            updateDetailsIfRequired(e.getNode());
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateDetailsIfRequired(e.getNode());
        }

        public void startStop(NodeManagerEvent e) {
            updateDetailsIfRequired(e.getNode());
        }
    }

    private class MySecurityManagerListener implements SecurityManagerListener {

        public void nodeAccountStateChanged(SecurityManagerEvent event) {
            updateDetailsIfRequired(event.getNode());
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyReconnectAction extends BaseAction {

        MyReconnectAction(Controller controller) {
            super("exp.action_reconnect", controller);
        }

        public void actionPerformed(ActionEvent e) {

            // Build new connect dialog
            final ConnectDialog connectDialog = new ConnectDialog(
                getController(), UIUtil.getParentWindow(e));

            Runnable connector = new Runnable() {
                public void run() {

                    // Open connect dialog if ui is open
                    connectDialog.open(node.getNick());

                    // Close connection first
                    node.shutdown();

                    // Now execute the connect
                    try {
                        if (node.reconnect().isFailure()) {
                            throw new ConnectionException(Translation
                                .getTranslation(
                                    "dialog.unable_to_connect_to_member", node
                                        .getNick()));
                        }
                    } catch (ConnectionException ex) {
                        connectDialog.close();
                        if (!connectDialog.isCanceled() && !node.isConnected())
                        {
                            // Show if user didn't cancel
                            ex.show(getController());
                        }
                    }

                    // Close dialog
                    connectDialog.close();
                }
            };

            // Start connect in anonymous thread
            new Thread(connector, "Reconnector to " + node.getNick()).start();
        }
    }

    private class MyAddRemoveFriendAction extends BaseAction {

        private boolean add = true;

        private MyAddRemoveFriendAction(Controller controller) {
            super("exp.action_add_friend", controller);
        }

        public void setAdd(boolean add) {
            this.add = add;
            if (add) {
                configureFromActionId("exp.action_add_friend");
            } else {
                configureFromActionId("exp.action_remove_friend");
            }
        }

        public void actionPerformed(ActionEvent e) {
            node.setFriend(add, null);
        }
    }

    /**
     * Handler for single file drop.
     */
    private class MyTransferHandler extends TransferHandler {

        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        public boolean importData(TransferSupport support) {

            if (!support.isDrop()) {
                return false;
            }

            final Path file = getFileList(support);
            if (file == null) {
                return false;
            }

            // Run later, so do not tie up OS drag and drop process.
            Runnable runner = new Runnable() {
                public void run() {
                    getUIController().transferSingleFile(file, node);
                }
            };
            SwingUtilities.invokeLater(runner);

            return true;
        }

        /**
         * Get the directory to import. The transfer is a list of files; need to
         * check the list has one directory, else return null.
         *
         * @param support
         * @return
         */
        private Path getFileList(TransferSupport support) {
            Transferable t = support.getTransferable();
            try {
                List list = (List) t
                    .getTransferData(DataFlavor.javaFileListFlavor);
                if (list.size() == 1) {
                    for (Object o : list) {
                        if (o instanceof Path) {
                            Path file = (Path) o;
                            if (Files.isDirectory(file)) {
                                return file;
                            }
                        }
                    }
                }
            } catch (UnsupportedFlavorException e) {
                logSevere(e);
            } catch (IOException e) {
                logSevere(e);
            }
            return null;
        }
    }

    private class PrimaryButtonActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (expanded.get()) {
                collapse();
            } else {
                expand();
            }
        }
    }
}
