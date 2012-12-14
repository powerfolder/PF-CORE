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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.dal33t.powerfolder.security.FolderReadPermission;
import jwf.WizardPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.NodesSelectDialog2;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.AutoTextField;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.MemberComparator;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class SendInvitationsPanel extends PFWizardPanel {
    private static final Logger LOG = Logger
        .getLogger(SendInvitationsAdvancedPanel.class.getName());

    private JButtonMini addButton;
    private JButtonMini searchButton;
    private JButtonMini removeButton;
    private AutoTextField viaPowerFolderText;
    private JList inviteesList;
    private JScrollPane inviteesListScrollPane;
    private ActionLabel addMessageLink;
    private DefaultListModel inviteesListModel;
    private Invitation invitation;
    private ValueModel messageModel;
    private JPanel removeButtonPanel;
    private JComponent messageComp;
    private DefaultComboBoxModel permissionsComboModel;
    private JComboBox permissionsCombo;

    public SendInvitationsPanel(Controller controller) {
        super(controller);
    }

    /**
     * Handles the invitation to nodes option.
     * 
     * @return true if send otherwise false
     */
    private boolean sendInvitation() {
        if (invitation == null) {
            return false;
        }
        String permissionText = (String) permissionsComboModel.getSelectedItem();
        FolderPermission folderPermission = FolderPermission.readWrite(invitation.folder);
        if (permissionText != null) {
            FolderPermission readPermission = FolderPermission.read(invitation.folder);
            if (readPermission.getName().equals(permissionText)) {
                folderPermission = readPermission;
            }
            FolderPermission readWritePermission = FolderPermission.readWrite(invitation.folder);
            if (readWritePermission.getName().equals(permissionText)) {
                folderPermission = readWritePermission;
            }
            FolderPermission adminPermission = FolderPermission.admin(invitation.folder);
            if (adminPermission.getName().equals(permissionText)) {
                folderPermission = adminPermission;
            }
        }
        invitation.setPermission(folderPermission);
        boolean theResult = false;
        Set<Member> candidates = getCandidates();

        // Send invite from text or list.
        if (viaPowerFolderText.getText().length() > 0) {
            sendInvite(candidates, viaPowerFolderText.getText());
            theResult = true;
        }
        for (Object o : inviteesListModel.toArray()) {
            String invitee = (String) o;
            sendInvite(candidates, invitee);
            theResult = true;
        }

        return theResult;
    }

    /**
     * Send an invite to a friend. The invitee must be in the list of friends or
     * be a valid email.
     * 
     * @param candidates
     * @param invitee
     */
    private void sendInvite(Collection<Member> candidates, String invitee) {
        RuntimeException rte = null;
        // Invitation by email
        try {
            InvitationUtil.invitationByServer(getController(), invitation,
                invitee, false);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unable to send invitation to " + invitee
                + ". " + e, e);
            rte = e;
        }
        for (Member node : candidates) {
            AccountInfo aInfo = node.getAccountInfo();
            if (aInfo != null && aInfo.getDisplayName() != null
                && aInfo.getDisplayName().equalsIgnoreCase(invitee))
            {
                InvitationUtil.invitationToNode(getController(), invitation,
                    node);
            }
        }
        // Invitation by node name
        for (Member node : candidates) {
            if (invitee.equalsIgnoreCase(node.getNick())) {
                InvitationUtil.invitationToNode(getController(), invitation,
                    node);
            }
        }
        if (rte != null) {
            throw rte;
        }
    }

    public boolean hasNext() {
        return !inviteesListModel.isEmpty() || viaPowerFolderText.getText().length() > 0;
    }

    public WizardPanel next() {
        if (messageModel.getValue() != null) {
            invitation.setInvitationText((String) messageModel.getValue());
        }
        Runnable inviteTask = new Runnable() {
            public void run() {
                if (messageModel.getValue() != null) {
                    invitation.setInvitationText((String) messageModel.getValue());
                }
                if (!sendInvitation()) {
                    throw new RuntimeException(Translation.getTranslation("wizard.send_invitations.no_invitees"));
                }
            }

        };
        WizardPanel successPanel = (WizardPanel) getWizardContext()
            .getAttribute(PFWizard.SUCCESS_PANEL);
        return new SwingWorkerPanel(
            getController(),
            inviteTask,
            Translation
                .getTranslation("wizard.send_invitations.sending_invites"),
            Translation
                .getTranslation("wizard.send_invitations.sending_invites.text"),
            successPanel);
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
            "140dlu, pref:grow",
            "pref, 3dlu, pref, 3dlu, pref, max(9dlu;pref), 3dlu, pref, 10dlu, pref");
        // inv join text inv fdl hint1 hint2 auto list remove, privs
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();
        int row = 1;

        builder.addLabel(LoginUtil.getInviteUsernameLabel(getController()),
            cc.xyw(1, row, 2));
        row += 2;

        FormLayout layout2 = new FormLayout("pref:grow, 3dlu, pref, pref",
            "pref");
        PanelBuilder builder2 = new PanelBuilder(layout2);
        builder2.add(viaPowerFolderText, cc.xy(1, 1));
        builder2.add(addButton, cc.xy(3, 1));
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            builder2.add(searchButton, cc.xy(4, 1));
        }
        JPanel panel2 = builder2.getPanel();
        panel2.setOpaque(false);
        builder.add(panel2, cc.xy(1, row));
        row += 2;

        inviteesListScrollPane = new JScrollPane(inviteesList);
        inviteesListScrollPane.setPreferredSize(new Dimension(
            getPreferredSize().width, Sizes.dialogUnitYAsPixel(40,
                inviteesListScrollPane)));
        builder.add(inviteesListScrollPane, cc.xy(1, row));
        inviteesListScrollPane.setVisible(false);
        row += 1;

        FormLayout layout3 = new FormLayout("pref, pref:grow", "pref");
        PanelBuilder builder3 = new PanelBuilder(layout3);
        builder3.add(removeButton, cc.xy(1, 1));
        removeButtonPanel = builder3.getPanel();
        removeButtonPanel.setOpaque(false);
        builder.add(removeButtonPanel, cc.xy(1, row));
        removeButtonPanel.setVisible(false);
        row += 2;

        builder.add(addMessageLink.getUIComponent(), cc.xy(1, row));
        builder.add(messageComp, cc.xy(1, row));
        row += 2;

        FormLayout layout4 = new FormLayout("pref, 3dlu, pref:grow", "pref");
        PanelBuilder builder4 = new PanelBuilder(layout4);
        builder4.add(new JLabel(Translation.getTranslation("send_invitations.permissions_label")), cc.xy(1, 1));
        builder4.add(permissionsCombo, cc.xy(3, 1));
        builder.add(builder4.getPanel(), cc.xy(1, row));

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {
        messageModel = new ValueHolder();

        final FolderInfo folder = (FolderInfo) getWizardContext().getAttribute(
            FOLDERINFO_ATTRIBUTE);
        Reject.ifNull(folder, "Unable to send invitation, folder is null");

        // Clear folder attribute
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, null);

        invitation = folder.getFolder(getController()).createInvitation();

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

        List<String> candidateAddresses = getCandidatesAddresses();
        viaPowerFolderText.setDataList(candidateAddresses);

        addMessageLink = new ActionLabel(getController(),
            new MyAttachMessageAction());
        addMessageLink.setText(Translation
            .getTranslation("wizard.send_invitations.add_message.text"));
        addMessageLink.setToolTipText(Translation
            .getTranslation("wizard.send_invitations.add_message.tip"));
        addMessageLink.convertToBigLabel();

        JScrollPane messagePane = new JScrollPane(
            BasicComponentFactory.createTextArea(messageModel));
        FormLayout layout2 = new FormLayout("fill:140dlu",
            "pref, 3dlu, fill:40dlu");
        PanelBuilder builder2 = new PanelBuilder(layout2);
        CellConstraints cc = new CellConstraints();
        builder2.addLabel(
            Translation.getTranslation("dialog.personalized_message.hint"),
            cc.xy(1, 1));
        builder2.add(messagePane, cc.xy(1, 3));
        messageComp = builder2.getPanel();
        messageComp.setVisible(false);
        messageComp.setOpaque(false);

        permissionsComboModel = new DefaultComboBoxModel();
        permissionsCombo = new JComboBox(permissionsComboModel);
        permissionsComboModel.addElement(FolderPermission.readWrite(folder).getName());
        permissionsComboModel.addElement(FolderPermission.read(folder).getName());
        if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            permissionsComboModel.addElement(FolderPermission.admin(folder).getName());
        }

        enableAddButton();
        enableRemoveButton();

    }

    private List<String> getCandidatesAddresses() {
        List<String> candidateAddresses = new LinkedList<String>();
        for (Member friend : getController().getNodeManager().getFriends()) {
            AccountInfo aInfo = friend.getAccountInfo();
            if (aInfo != null && aInfo.getDisplayName() != null) {
                // FIXME Shows email unscrambled!
                candidateAddresses.add(0, aInfo.getDisplayName());
            }
            //candidateAddresses.add(friend.getNick());
        }
        for (Member node : getController().getNodeManager().getConnectedNodes())
        {
            if (!node.isOnLAN()) {
                continue;
            }
            AccountInfo aInfo = node.getAccountInfo();
            if (aInfo != null && aInfo.getDisplayName() != null) {
                // FIXME Shows email unscrambled!
                candidateAddresses.add(0, aInfo.getDisplayName());
            }
            //candidateAddresses.add(node.getNick());
        }
        return candidateAddresses;
    }

    private Set<Member> getCandidates() {
        Set<Member> candidate = new TreeSet<Member>(MemberComparator.NICK);
        Collections.addAll(candidate, getController().getNodeManager().getFriends());
        for (Member node : getController().getNodeManager().getConnectedNodes())
        {
            if (!node.isOnLAN()) {
                continue;
            }
            candidate.add(node);
        }
        return candidate;
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
            inviteesListScrollPane.setVisible(true);
            removeButtonPanel.setVisible(true);
            viaPowerFolderText.clear();
            updateButtons();
            enableAddButton();
            enableRemoveButton();
        }
    }

    // /////////////////
    // Inner classes //
    // /////////////////

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

            Collection<Member> selectedMembers = new ArrayList<Member>();
            NodesSelectDialog2 nsd2 = new NodesSelectDialog2(getController(),
                selectedMembers);
            nsd2.open();
            for (Member selectedMember : selectedMembers) {
                boolean got = false;
                for (Object o : inviteesListModel.toArray()) {
                    String invitee = (String) o;
                    if (selectedMember.getNick().equals(invitee)) {
                        got = true;
                        break;
                    }
                }
                if (!got) {
                    inviteesListModel.addElement(selectedMember.getNick());
                    inviteesListScrollPane.setVisible(true);
                    removeButtonPanel.setVisible(true);
                }
            }
            updateButtons();
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
                inviteesListScrollPane.setVisible(!inviteesListModel.isEmpty());
                removeButtonPanel.setVisible(!inviteesListModel.isEmpty());
                enableRemoveButton();
            }
        }
    }

    private class MyKeyListener extends KeyAdapter {

        public void keyReleased(KeyEvent e) {
            updateButtons();
            enableAddButton();
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            enableRemoveButton();
            updateButtons();
        }
    }

    private class MyAttachMessageAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            messageComp.setVisible(true);
            addMessageLink.getUIComponent().setVisible(false);
        }
    }
}