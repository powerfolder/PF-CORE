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
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
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
import java.util.Date;

/**
 * Class to render expandable view of a folder.
 */
public class ExpandableComputerView extends PFUIComponent {

    private final Member member;
    private JButtonMini expandCollapseButton;
    private JPanel uiComponent;
    private JPanel lowerOuterPanel;
    private AtomicBoolean expanded;
    private JButtonMini reconnectButton;
    private JButtonMini addRemoveButton;
    private JLabel pictoLabel;

    private JLabel lastSeenLabel;
    private MyNodeManagerListener nodeManagerListener;

    /**
     * Constructor
     *
     * @param controller
     * @param member
     */
    public ExpandableComputerView(Controller controller, Member member) {
        super(controller);
        this.member = member;
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
                                             //  icon        name  space            recon       ex/co
        FormLayout upperLayout = new FormLayout("pref, 3dlu, pref, pref:grow, 3dlu, pref, 3dlu, pref",
            "pref");
        PanelBuilder upperBuilder = new PanelBuilder(upperLayout);
        CellConstraints cc = new CellConstraints();

        upperBuilder.add(pictoLabel, cc.xy(1, 1));
        upperBuilder.add(new JLabel(member.getNick()), cc.xy(3, 1));
        upperBuilder.add(addRemoveButton, cc.xy(6, 1));
        upperBuilder.add(expandCollapseButton, cc.xy(8, 1));

        JPanel upperPanel = upperBuilder.getPanel();

        // Build lower detials with line border.
        FormLayout lowerLayout = new FormLayout("3dlu, pref, pref:grow, 3dlu, pref, 3dlu",
            "pref, 3dlu, pref");
          // sep,        last
        PanelBuilder lowerBuilder = new PanelBuilder(lowerLayout);

        lowerBuilder.addSeparator(null, cc.xywh(2, 1, 4, 1));

        lowerBuilder.add(lastSeenLabel, cc.xy(2, 3));
        lowerBuilder.add(reconnectButton, cc.xy(5, 3));

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

        expandCollapseButton = new JButtonMini(Icons.EXPAND,
                Translation.getTranslation("exp_computer_view.expand"));
        expandCollapseButton.addActionListener(new MyCollapseActionListener());
        lastSeenLabel = new JLabel();
        reconnectButton = new JButtonMini(getApplicationModel()
                .getActionModel().getReconnectAction());
        reconnectButton.addActionListener(new MyReconnectActionListener());
        addRemoveButton = new JButtonMini(getApplicationModel().getActionModel()
                .getAddFriendAction());
        addRemoveButton.addActionListener(new MyAddRemoveActionListener());
        pictoLabel = new JLabel();
        updateDetails();
        configureAddRemoveButton();
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
    public Member getMember() {
        return member;
    }

    /**
     * Updates the displayed details if for this member.
     *
     * @param e
     */
    private void updateDetailsIfRequired(NodeManagerEvent e) {
        Member node = e.getNode();
        if (node == null) {
            return;
        }
        if (node.equals(member)) {
            updateDetails();
            configureAddRemoveButton();
        }
    }

    private void configureAddRemoveButton() {
        if (member.isFriend()) {
            addRemoveButton.configureFromAction(getApplicationModel().getActionModel().getRemoveFriendAction());
        } else {
            addRemoveButton.configureFromAction(getApplicationModel().getActionModel().getAddFriendAction());
        }
    }

    /**
     * Updates the displayed details of the member.
     */
    private void updateDetails() {
        Date time = member.getLastConnectTime();
        String lastConnectedTime;
        if (time == null) {
            lastConnectedTime = "";
        } else {
            lastConnectedTime = Format.formatDate(time);
        }
        lastSeenLabel.setText(Translation.getTranslation(
                "exp_computer_view.last_seen_text", lastConnectedTime));

        if (member.isConnected()) {
            if (member.isFriend()) {
                pictoLabel.setIcon(Icons.NODE_FRIEND_CONNECTED);
                pictoLabel.setToolTipText(Translation.getTranslation(
                        "exp_computer_view.node_friend_connected_text"));
            } else {
                pictoLabel.setIcon(Icons.NODE_NON_FRIEND_CONNECTED);
                pictoLabel.setToolTipText(Translation.getTranslation(
                        "exp_computer_view.node_non_friend_connected_text"));
            }
        } else {
            if (member.isFriend()) {
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

    /**
     * Class to respond to expand / collapse events.
     */
    private class MyCollapseActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            boolean exp = expanded.get();
            if (exp) {
                expanded.set(false);
                expandCollapseButton.setIcon(Icons.EXPAND);
                expandCollapseButton.setToolTipText(
                        Translation.getTranslation("exp_computer_view.expand"));
                lowerOuterPanel.setVisible(false);
            } else {
                expanded.set(true);
                expandCollapseButton.setIcon(Icons.COLLAPSE);
                expandCollapseButton.setToolTipText(
                        Translation.getTranslation("exp_computer_view.collapse"));
                lowerOuterPanel.setVisible(true);
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

    /**
     * Class to listen for reconnect requests.
     */
    private class MyReconnectActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            ActionEvent ae = new ActionEvent(getMember().getInfo(),
                    e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers());
            getApplicationModel().getActionModel().getReconnectAction()
                    .actionPerformed(ae);
        }
    }

    /**
     * Class to listen for add / remove friendship requests.
     */
    private class MyAddRemoveActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            ActionEvent ae = new ActionEvent(getMember().getInfo(),
                    e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers());
            if (member.isFriend()) {
                getApplicationModel().getActionModel().getRemoveFriendAction()
                        .actionPerformed(ae);
            } else {
                getApplicationModel().getActionModel().getAddFriendAction()
                        .actionPerformed(ae);
            }            
        }
    }
}