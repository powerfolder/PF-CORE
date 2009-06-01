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
package de.dal33t.powerfolder.ui.wizard;

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import de.dal33t.powerfolder.ui.widget.AutoTextField;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.action.BaseAction;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class SendInvitationsPanel extends PFWizardPanel {

    private JTextArea invitationTextField;
    private JButtonMini addButton;
    private JButtonMini searchButton;
    private JButtonMini removeButton;
    private AutoTextField viaPowerFolderText;
    private JList inviteesList;
    private DefaultListModel inviteesListModel;

    private Invitation invitation;

    public SendInvitationsPanel(Controller controller) {
        super(controller);
    }

    /**
     * Handles the invitation to nodes option.
     *
     * @return true if send otherwise false
     */
    private boolean sendInvitationToNodes() {
        if (invitation == null) {
            return false;
        }
        boolean theResult = false;
        for (Object o : inviteesListModel.toArray()) {
            String invitee = (String) o;
//            InvitationUtil.invitationToNode(getController(), invitation,
//                    member);
            theResult = true;
        }
        return theResult;
    }

    public boolean hasNext() {
        return !inviteesListModel.isEmpty();
    }

    public boolean validateNext() {
        invitation.setInvitationText(invitationTextField.getText());
        return sendInvitationToNodes();
    }

    public WizardPanel next() {
        // Show success panel
        return (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
                "140dlu, pref:grow",
                "pref, 3dlu, pref, 6dlu, pref, 3dlu, pref, 6dlu, pref, pref, 3dlu, pref, 3dlu, pref, pref");
              // inv join    untrust     inv text    inv fdl     hint1 hint2       auto        list  remove
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.addLabel(Translation.getTranslation(
                "wizard.send_invitations.join"), cc.xyw(1, row, 2));

        row += 2;
        builder.addLabel(Translation.getTranslation(
                "wizard.send_invitations.never_untrusted"), cc.xyw(
                1, row, 2));

        row += 2;
        builder.addLabel(Translation.getTranslation(
                "wizard.send_invitations.invitation_text"), cc.xyw(
                1, row, 2));

        row += 2;
        JScrollPane invTextScroll = new JScrollPane(invitationTextField);
        invTextScroll.setPreferredSize(new Dimension(50, 60));
        builder.add(invTextScroll, cc.xy(1, row));

        row += 2;

        builder.addLabel(Translation
                .getTranslation("wizard.send_invitations.invitation_hint1"), cc.xyw(
                1, row, 2));

        row++;

        builder.addLabel(Translation
                .getTranslation("wizard.send_invitations.invitation_hint2"), cc.xyw(
                1, row, 2));

        row += 2;

        FormLayout layout2 = new FormLayout("107dlu, 3dlu, pref, pref", "pref");
        PanelBuilder builder2 = new PanelBuilder(layout2);
        builder2.add(viaPowerFolderText, cc.xy(1, 1));
        builder2.add(addButton, cc.xy(3, 1));
        builder2.add(searchButton, cc.xy(4, 1));
        JPanel panel2 = builder2.getPanel();
        panel2.setOpaque(false);
        builder.add(panel2, cc.xy(1, row));

        row += 2;

        JScrollPane scrollPane = new JScrollPane(inviteesList);
        UIUtil.removeBorder(scrollPane);
        scrollPane.setPreferredSize(new Dimension(getPreferredSize().width, 50));
        builder.add(new JScrollPane(scrollPane), cc.xy(1, row));

        row += 1;

        FormLayout layout3 = new FormLayout("pref, pref:grow", "pref");
        PanelBuilder builder3 = new PanelBuilder(layout3);
        builder3.add(removeButton, cc.xy(1, 1));
        JPanel panel3 = builder3.getPanel();
        panel3.setOpaque(false);
        builder.add(panel3, cc.xy(1, row));

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {
        FolderInfo folder = (FolderInfo) getWizardContext().getAttribute(
                FOLDERINFO_ATTRIBUTE);
        Reject.ifNull(folder, "Unable to send invitation, folder is null");

        // Clear folder attribute
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

        invitation = folder.getFolder(getController()).createInvitation();
        invitationTextField = new JTextArea();

        addButton = new JButtonMini(new MyAddAction(getController()));
        removeButton = new JButtonMini(new MyRemoveAction(getController()));
        searchButton = new JButtonMini(new MySearchAction(getController()));

        viaPowerFolderText = new AutoTextField();
        viaPowerFolderText.addKeyListener(new MyKeyListener());

        inviteesListModel = new DefaultListModel();
        inviteesList = new JList(inviteesListModel);
        inviteesList.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        inviteesList.getSelectionModel().addListSelectionListener(
                new MyListSelectionListener());

        List<String> friendNicks = new ArrayList<String>();
        for (Member friend : getController().getNodeManager().getFriends()) {
            friendNicks.add(friend.getNick());
        }
        viaPowerFolderText.setDataList(friendNicks);
        
        enableAddButton();
        enableRemoveButton();
    }

    protected JComponent getPictoComponent() {
        return new JLabel(getContextPicto());
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.send_invitations.title");
    }

    private void enableAddButton() {
        addButton.setEnabled(viaPowerFolderText.getText().length() > 0);
    }

    private void enableRemoveButton() {
        removeButton.setEnabled(!inviteesListModel.isEmpty()
                && inviteesList.getSelectedIndex() >= 0);
    }

    private void processInvitee() {
        String text = viaPowerFolderText.getText();
        if (text.length() > 0) {
            inviteesListModel.addElement(text);
            viaPowerFolderText.clear();
            updateButtons();
            enableAddButton();
            enableRemoveButton();
        }
    }

    ///////////////////
    // Inner classes //
    ///////////////////

    private class MyAddAction extends BaseAction {

        MyAddAction(Controller controller) {
            super("action_add_invitee", controller);
        }

        public void actionPerformed(ActionEvent e) {
            processInvitee();
        }
    }

    private class MySearchAction extends BaseAction {

        MySearchAction(Controller controller) {
            super("action_search_invitee", controller);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    private class MyRemoveAction extends BaseAction {

        MyRemoveAction(Controller controller) {
            super("action_remove_invitee", controller);
        }

        public void actionPerformed(ActionEvent e) {
            int index = inviteesList.getSelectedIndex();
            if (index >= 0) {
                inviteesListModel.remove(index);
                enableRemoveButton();
            }
        }
    }

    private class MyKeyListener implements KeyListener {

        public void keyTyped(KeyEvent e) {
        }

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
            enableAddButton();
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            enableRemoveButton();
            updateButtons();
        }
    }
}