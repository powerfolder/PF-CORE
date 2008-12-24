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
* $Id: MembersTab.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.members;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.folder.FolderInformationTab;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * UI component for the members information tab
 */
public class MembersTab extends PFUIComponent implements FolderInformationTab {

    private JPanel uiComponent;
    private MembersTableModel model;
    private JScrollPane scrollPane;
    private MyOpenChatAction openChatAction;
    private MembersTable membersTable;
    private Member selectedMember;

    private JButton addRemoveButton;

    /**
     * Constructor
     *
     * @param controller
     */
    public MembersTab(Controller controller) {
        super(controller);
        model = new MembersTableModel(getController());
        addRemoveButton = new JButton(getApplicationModel().getActionModel()
                .getAddFriendAction());
    }

    /**
     * Set the tab with details for a folder.
     *
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        model.setFolderInfo(folderInfo);
    }

    /**
     * Gets the ui component
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    public void initialize() {
        openChatAction = new MyOpenChatAction(getController());
        membersTable = new MembersTable(model);
        membersTable.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        membersTable.getSelectionModel().addListSelectionListener(
                new MySelectionListener());
        scrollPane = new JScrollPane(membersTable);

        // Whitestrip
        UIUtil.whiteStripTable(membersTable);
        UIUtil.removeBorder(scrollPane);
        UIUtil.setZeroHeight(scrollPane);

        enableOnSelection();
    }

    /**
     * Bulds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, fill:pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref , 3dlu, fill:0:grow, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(createToolBar(), cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(scrollPane, cc.xy(2, 6));
        uiComponent = builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {

        addRemoveButton.addActionListener(new MyAddRemoveActionListener());
        enableOnSelection();

        JButton reconnectButton = new JButton(getUIController()
                .getApplicationModel().getActionModel().getReconnectAction());
        reconnectButton.addActionListener(new MyReconnectActionListener());

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(openChatAction));
        bar.addRelatedGap();
        bar.addGridded(addRemoveButton);
        bar.addRelatedGap();
        bar.addGridded(reconnectButton);
        return bar.getPanel();
    }

    /**
     * Enable the invite action on the table selection.
     */
    private void enableOnSelection() {
        int selectedRow = membersTable.getSelectedRow();
        if (selectedRow >= 0) {
            selectedMember = (Member) model.getValueAt(
                    membersTable.getSelectedRow(), 0);
            openChatAction.setEnabled(true);

            addRemoveButton.setEnabled(true);
            if (selectedMember.isFriend()) {
                addRemoveButton.setAction(getApplicationModel().getActionModel()
                        .getRemoveFriendAction());
            } else {
                addRemoveButton.setAction(getApplicationModel().getActionModel()
                        .getAddFriendAction());
            }

        } else {
            selectedMember = null;
            openChatAction.setEnabled(false);
            addRemoveButton.setEnabled(false);
        }
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    private class MyOpenChatAction extends BaseAction {

        private MyOpenChatAction(Controller controller) {
            super("action_open_chat", controller);
        }

        public void actionPerformed(ActionEvent e) {
            getController().getUIController().openChat(selectedMember.getInfo());
        }
    }

    /**
     * Class to detect table selection changes.
     */
    private class MySelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            enableOnSelection();
        }
    }


    /**
     * Class to listen for add / remove friendship requests.
     */
    private class MyAddRemoveActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            ActionEvent ae = new ActionEvent(selectedMember.getInfo(),
                    e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers());
            if (selectedMember.isFriend()) {
                getApplicationModel().getActionModel().getRemoveFriendAction()
                        .actionPerformed(ae);
            } else {
                getApplicationModel().getActionModel().getAddFriendAction()
                        .actionPerformed(ae);
            }
        }
    }


    /**
     * Class to listen for reconnect requests.
     */
    private class MyReconnectActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            ActionEvent ae = new ActionEvent(selectedMember.getInfo(),
                    e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers());
            getApplicationModel().getActionModel().getReconnectAction()
                    .actionPerformed(ae);
        }
    }
}