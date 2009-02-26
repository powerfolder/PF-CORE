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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.ExpandableView;
import de.dal33t.powerfolder.ui.dialog.ConnectDialog;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;
import java.io.IOException;

/**
 * Class to render expandable view of a folder.
 */
public class ExpandableComputerView extends PFUIComponent implements ExpandableView {

    private final Member node;
    private JPanel uiComponent;
    private JPanel lowerOuterPanel;
    private AtomicBoolean expanded;
    private JButtonMini reconnectButton;
    private JButtonMini addRemoveButton;
    private JLabel pictoLabel;
    private JButtonMini chatButton;
    private JPanel upperPanel;
    private MyAddRemoveFriendAction addRemoveFriendAction;
    private MyReconnectAction reconnectAction;
    private JLabel lastSeenLabel;
    private MyNodeManagerListener nodeManagerListener;

    private ExpansionListener listenerSupport;

    private JPopupMenu contextMenu;

    /**
     * Constructor
     *
     * @param controller
     * @param member
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
        upperPanel.setToolTipText(
                Translation.getTranslation("exp_computer_view.collapse"));
        lowerOuterPanel.setVisible(true);
        listenerSupport.collapseAllButSource(new ExpansionEvent(this));
    }

    /**
     * Collapse this view if expanded.
     */
    public void collapse() {
        expanded.set(false);
        upperPanel.setToolTipText(
                Translation.getTranslation("exp_computer_view.expand"));
        lowerOuterPanel.setVisible(false);
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
                                             //  icon        name  space            chat
        FormLayout upperLayout = new FormLayout("pref, 3dlu, pref, pref:grow, 3dlu, pref",
            "pref");
        PanelBuilder upperBuilder = new PanelBuilder(upperLayout);
        CellConstraints cc = new CellConstraints();

        upperBuilder.add(pictoLabel, cc.xy(1, 1));
        upperBuilder.add(new JLabel(node.getNick()), cc.xy(3, 1));
        upperBuilder.add(chatButton, cc.xy(6, 1));

        upperPanel = upperBuilder.getPanel();
        upperPanel.setToolTipText(
                Translation.getTranslation("exp_computer_view.expand"));
        MouseAdapter ma = new MyMouseAdapter();
        upperPanel.addMouseListener(ma);
        upperPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pictoLabel.addMouseListener(ma);

        // Build lower detials with line border.
        FormLayout lowerLayout = new FormLayout("3dlu, pref, pref:grow, 3dlu, pref, pref, 3dlu",
            "pref, 3dlu, pref");
          // sep,        last
        PanelBuilder lowerBuilder = new PanelBuilder(lowerLayout);

        lowerBuilder.addSeparator(null, cc.xyw(1, 1, 7));

        lowerBuilder.add(lastSeenLabel, cc.xy(2, 3));
        lowerBuilder.add(addRemoveButton, cc.xy(5, 3));
        lowerBuilder.add(reconnectButton, cc.xy(6, 3));

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

        // Build ui with vertical space before the next one.
        FormLayout outerLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "pref, 3dlu");
        PanelBuilder outerBuilder = new PanelBuilder(outerLayout);
        outerBuilder.add(borderPanel, cc.xy(2, 1));

        uiComponent = outerBuilder.getPanel();

        uiComponent.setTransferHandler(new MyTransferHandler());
    }

    /**
     * Initializes the components.
     */
    private void initComponent() {
        expanded = new AtomicBoolean();

        lastSeenLabel = new JLabel();
        reconnectAction = new MyReconnectAction(getController());
        reconnectButton = new JButtonMini(reconnectAction, true);
        addRemoveFriendAction = new MyAddRemoveFriendAction(getController());
        addRemoveButton = new JButtonMini(addRemoveFriendAction, true);
        chatButton = new JButtonMini(new MyOpenChatAction(getController()), true);
        pictoLabel = new JLabel();
        updateDetails();
        configureAddRemoveButton();
        registerListeners();
    }

    /**
     * Call this to unregister listeners if computer is being removed.
     */
    public void removeListeners() {
        unregisterListeners();
    }

    /**
     * Register listeners of the folder.
     */
    private void registerListeners() {
        nodeManagerListener = new MyNodeManagerListener();
        getController().getNodeManager().addNodeManagerListener(
                nodeManagerListener);
    }

    /**
     * Unregister listeners of the folder.
     */
    private void unregisterListeners() {
        getController().getNodeManager().removeNodeManagerListener(
                nodeManagerListener);
    }

    /**
     * Gets the name of the associated folder.
     * @return
     */
    public Member getNode() {
        return node;
    }

    /**
     * Updates the displayed details if for this member.
     *
     * @param e
     */
    private void updateDetailsIfRequired(NodeManagerEvent e) {
        Member eventNode = e.getNode();
        if (eventNode == null) {
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
        Date time = node.getLastConnectTime();
        String lastConnectedTime;
        if (time == null) {
            lastConnectedTime = "";
        } else {
            lastConnectedTime = Format.formatDate(time);
        }
        lastSeenLabel.setText(Translation.getTranslation(
                "exp_computer_view.last_seen_text", lastConnectedTime));

        if (node.isConnected()) {
            if (node.isFriend()) {
                pictoLabel.setIcon(Icons.NODE_FRIEND_CONNECTED);
                pictoLabel.setToolTipText(Translation.getTranslation(
                        "exp_computer_view.node_friend_connected_text"));
            } else {
                pictoLabel.setIcon(Icons.NODE_NON_FRIEND_CONNECTED);
                pictoLabel.setToolTipText(Translation.getTranslation(
                        "exp_computer_view.node_non_friend_connected_text"));
            }
        } else {
            if (node.isFriend()) {
                pictoLabel.setIcon(Icons.NODE_FRIEND_DISCONNECTED);
                pictoLabel.setToolTipText(Translation.getTranslation(
                        "exp_computer_view.node_friend_disconnected_text"));
            } else {
                pictoLabel.setIcon(Icons.NODE_NON_FRIEND_DISCONNECTED);
                pictoLabel.setToolTipText(Translation.getTranslation(
                        "exp_computer_view.node_non_friend_disconnected_text"));
            }
        }

    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ExpandableComputerView that = (ExpandableComputerView) obj;

        if (!node.equals(that.node)) {
            return false;
        }

        return true;
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
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public JPopupMenu createPopupMenu() {
        if (contextMenu == null) {
            contextMenu = new JPopupMenu();
            contextMenu.add(addRemoveFriendAction);
            contextMenu.add(reconnectAction);
        }
        return contextMenu;
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    /**
     * Class to respond to expand / collapse events.
     */
    private class MyMouseAdapter extends MouseAdapter {

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
            if (!expanded.get()) {
                createPopupMenu().show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
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
    private class MyNodeManagerListener implements NodeManagerListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void friendAdded(NodeManagerEvent e) {
            updateDetailsIfRequired(e);
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateDetailsIfRequired(e);
        }

        public void nodeAdded(NodeManagerEvent e) {
            updateDetailsIfRequired(e);
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateDetailsIfRequired(e);
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateDetailsIfRequired(e);
        }

        public void nodeRemoved(NodeManagerEvent e) {
            updateDetailsIfRequired(e);
        }

        public void settingsChanged(NodeManagerEvent e) {
            updateDetailsIfRequired(e);
        }

        public void startStop(NodeManagerEvent e) {
            updateDetailsIfRequired(e);
        }
    }

    private class MyOpenChatAction extends BaseAction {

        private MyOpenChatAction(Controller controller) {
            super("action_open_chat", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openChat(getNode().getInfo());
        }
    }

    private class MyReconnectAction extends BaseAction {

        MyReconnectAction(Controller controller) {
            super("action_reconnect", controller);
        }

        public void actionPerformed(ActionEvent e) {

            // Build new connect dialog
            final ConnectDialog connectDialog = new ConnectDialog(getController());

            Runnable connector = new Runnable() {
                public void run() {

                    // Open connect dialog if ui is open
                    connectDialog.open(node.getNick());

                    // Close connection first
                    node.shutdown();

                    // Now execute the connect
                    try {
                        if (!node.reconnect()) {
                            throw new ConnectionException(Translation.getTranslation(
                                    "dialog.unable_to_connect_to_member",
                                node.getNick()));
                        }
                    } catch (ConnectionException ex) {
                        connectDialog.close();
                        if (!connectDialog.isCanceled() && !node.isConnected()) {
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
            super("action_add_friend", controller);
        }

        public void setAdd(boolean add) {
            this.add = add;
            if (add) {
                configureFromActionId("action_add_friend");
            } else {
                configureFromActionId("action_remove_friend");
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (add) {
                boolean askForFriendshipMessage = PreferencesEntry.
                        ASK_FOR_FRIENDSHIP_MESSAGE.getValueBoolean(getController());
                if (askForFriendshipMessage) {

                    // Prompt for personal message.
                    String[] options = {
                            Translation.getTranslation("general.ok"),
                            Translation.getTranslation("general.cancel")};

                    FormLayout layout = new FormLayout("pref", "pref, 3dlu, pref, pref");
                    PanelBuilder builder = new PanelBuilder(layout);
                    CellConstraints cc = new CellConstraints();
                    String nick = node.getNick();
                    String text = Translation.getTranslation(
                            "friend.search.personal.message.text2", nick);
                    builder.add(new JLabel(text), cc.xy(1, 1));
                    JTextArea textArea = new JTextArea();
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(400, 200));
                    builder.add(scrollPane, cc.xy(1, 3));
                    JPanel innerPanel = builder.getPanel();

                    NeverAskAgainResponse response = DialogFactory.genericDialog(
                            getController(),
                            Translation.getTranslation("friend.search.personal.message.title"),
                            innerPanel, options, 0, GenericDialogType.INFO,
                            Translation.getTranslation("general.neverAskAgain"));
                    if (response.getButtonIndex() == 0) { // == OK
                        String personalMessage = textArea.getText();
                        node.setFriend(true, personalMessage);
                    }
                    if (response.isNeverAskAgain()) {
                        // don't ask me again
                        PreferencesEntry.ASK_FOR_FRIENDSHIP_MESSAGE.setValue(
                                getController(), false);
                    }
                } else {
                    // Send with no personal messages
                    node.setFriend(true, null);
                }
            } else {
                node.setFriend(false, null);
            }
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

            final File file = getFileList(support);
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
         * Get the directory to import.
         * The transfer is a list of files; need to check the list has one
         * directory, else return null.
         *
         * @param support
         * @return
         */
        private File getFileList(TransferSupport support) {
            Transferable t = support.getTransferable();
            try {
                java.util.List list = (java.util.List) t.getTransferData(
                        DataFlavor.javaFileListFlavor);
                if (list.size() == 1) {
                    for (Object o : list) {
                        if (o instanceof File) {
                            File file = (File) o;
                            if (!file.isDirectory()) {
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
}