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

import static de.dal33t.powerfolder.ui.wizard.SendInvitationsPanel.OPTIONS.SAVE_TO_FILE;
import static de.dal33t.powerfolder.ui.wizard.SendInvitationsPanel.OPTIONS.SEND_BY_MAIL;
import static de.dal33t.powerfolder.ui.wizard.SendInvitationsPanel.OPTIONS.SEND_DIRECT;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import jwf.WizardPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.dialog.NodesSelectDialog;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.FileSelectorFactory;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class SendInvitationsPanel extends PFWizardPanel {

    // The options of this screen
    enum OPTIONS {
        SAVE_TO_FILE, SEND_BY_MAIL, SEND_DIRECT
    }

    private boolean firstFocusGainOfEmailField;
    private Invitation invitation;
    private JComponent invitationFileField;
    private JComponent sendByMailButton;
    private JComponent emailField;
    private JComponent ccBox;
    private JComponent saveToFileButton;
    private JRadioButton sendViaPowerFolderButton;
    private JTextField invitationTextField;

    private ValueModel emailModel;
    private ValueModel invitationFileModel;
    private ValueModel decision;
    private ValueModel viaPowerFolderModel;
    private ValueModel ccValue;
    private JTextField viaPowerFolderText;
    private JButton viaPowerFolderConfigButton;

    private final Collection<Member> viaPowerFolderMembers = new ArrayList<Member>();

    public SendInvitationsPanel(Controller controller)
    {
        super(controller);
        firstFocusGainOfEmailField = true;
    }

    // Application logic

    /**
     * Handles the invitation to disk option.
     * 
     * @return true if saved otherwise false
     */
    private boolean saveInvitationToFile() {
        if (StringUtils.isBlank((String) invitationFileModel.getValue())) {
            return false;
        }
        if (invitation == null) {
            return false;
        }
        String filename = (String) invitationFileModel.getValue();
        if (!filename.endsWith(".invitation")) {
            filename += ".invitation";
        }
        File file = new File(filename);
        if (file.exists()) {
            // TODO: Add confirm dialog
        }
        return InvitationUtil.invitationToDisk(getController(), invitation,
            file);
    }

    /**
     * Handles the invitation to mail option.
     * 
     * @return true if mailed otherwise false
     */
    private boolean sendInvitationByMail() {
        if (invitation == null) {
            return false;
        }
        if (getController().getOSClient().isConnected()) {
            InvitationUtil.invitationByServer(getController(), invitation,
                (String) emailModel.getValue(), (Boolean) ccValue.getValue());
            // TODO Could fail, but that's a "latent" event.
            return true;
        } else {
            // TODO Think about total removal of this crappy thing.
            return InvitationUtil.invitationToMail(getController(), invitation,
                (String) emailModel.getValue());
        }
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
        for (Member member : viaPowerFolderMembers) {
            InvitationUtil
                .invitationToNode(getController(), invitation, member);
            // Do not evaulate return value. Because invitation is
            // always sent or enqueued for later sending.
            theResult = true;
        }
        return theResult;
    }

    // From WizardPanel *******************************************************

    public boolean hasNext() {
        return true;
    }

    public boolean validateNext(List<String> errors) {
        invitation.setInvitationText(invitationTextField.getText());
        boolean ok = false;
        if (decision.getValue() == SEND_BY_MAIL) {
            // Send by email
            ok = sendInvitationByMail();
        } else if (decision.getValue() == SAVE_TO_FILE) {
            // Store now
            ok = saveInvitationToFile();
        } else if (decision.getValue() == SEND_DIRECT) {
            // Send now
            ok = sendInvitationToNodes();
        }
        return ok;
    }

    public WizardPanel next() {
        // Show success panel
        return (WizardPanel) getWizardContext().getAttribute(
            PFWizard.SUCCESS_PANEL);
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
            "pref, 3dlu, 140dlu, pref:grow",
            "pref, 3dlu, pref, 6dlu, pref, $rg, pref, 6dlu, pref, $rg, "
                + "pref, 3dlu, pref, 6dlu, pref, 3dlu, pref, 6dlu, pref, 3dlu, "
                + "pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.addLabel(Translation
            .getTranslation("wizard.send_invitations.join"), cc.xyw(1, row, 4));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("wizard.send_invitations.never_untrusted"), cc.xyw(
            1, row, 4));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("wizard.send_invitations.invitation_text"), cc.xyw(
            1, row, 4));

        row += 2;
        builder.add(invitationTextField, cc.xy(1, row));

        row += 2;
        builder.add(sendByMailButton, cc.xyw(1, row, 3));
        row += 2;
        builder.add(emailField, cc.xy(1, row));
        row += 2;
        builder.add(ccBox, cc.xy(1, row));

        row += 2;
        builder.add(saveToFileButton, cc.xyw(1, row, 3));
        row += 2;
        builder.add(invitationFileField, cc.xy(1, row));

        row += 2;
        builder.add(sendViaPowerFolderButton, cc.xyw(1, row, 3));
        row += 2;

        FormLayout layout2 = new FormLayout("122dlu, 3dlu, pref", "pref");
        PanelBuilder builder2 = new PanelBuilder(layout2);
        builder2.add(viaPowerFolderText, cc.xy(1, 1));
        builder2.add(viaPowerFolderConfigButton, cc.xy(3, 1));
        JPanel panel2 = builder2.getPanel();
        panel2.setOpaque(false);
        builder.add(panel2, cc.xy(1, row));

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

        invitationFileModel = new ValueHolder();
        emailModel = new ValueHolder(Translation
            .getTranslation("send_invitation.example_email_address"));
        ccValue = new ValueHolder(false);
        decision = new ValueHolder(SEND_BY_MAIL, true);

        sendByMailButton = BasicComponentFactory.createRadioButton(decision,
            SEND_BY_MAIL, Translation
                .getTranslation("wizard.send_invitations.send_by_mail"));
        sendByMailButton.setOpaque(false);

        emailField = BasicComponentFactory.createTextField(emailModel);
        emailField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (firstFocusGainOfEmailField) {
                    emailModel.setValue("");
                    firstFocusGainOfEmailField = false;
                }
            }

            public void focusLost(FocusEvent e) {
            }
        });

        ccBox = BasicComponentFactory.createCheckBox(ccValue, Translation
            .getTranslation("wizard.send_invitations.send_by_mail.cc_me"));
        ccBox.setOpaque(false);

        saveToFileButton = BasicComponentFactory.createRadioButton(decision,
            SAVE_TO_FILE, Translation
                .getTranslation("wizard.send_invitations.save_to_file"));
        saveToFileButton.setOpaque(false);

        sendViaPowerFolderButton = BasicComponentFactory.createRadioButton(
            decision, SEND_DIRECT, Translation
                .getTranslation("wizard.send_invitations.over_powerfolder"));
        sendViaPowerFolderButton.setOpaque(false);
        sendViaPowerFolderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (viaPowerFolderModel.getValue() == null
                    || viaPowerFolderModel.getValue().equals(
                        Translation.getTranslation("dialog.node_select.no_computers")))
                {
                    openNodesSelectDialog();
                }
            }
        });

        invitationFileField = FileSelectorFactory.createFileSelectionField(
            Translation.getTranslation("wizard.send_invitations.title"),
            invitationFileModel, JFileChooser.FILES_ONLY, // Save invitation
            InvitationUtil.createInvitationsFilefilter(), false);
        invitationFileField.setOpaque(false);
        
        invitationTextField = new JTextField(Translation.getTranslation(
            "wizard.send_invitations.invitation_text_sample", folder.name));
        JScrollPane invTextScroll = new JScrollPane(invitationTextField);
        invTextScroll.setPreferredSize(new Dimension(50, 80));

        viaPowerFolderModel = new ValueHolder();
        viaPowerFolderModel.setValue(Translation
            .getTranslation("dialog.node_select.no_computers"));
        viaPowerFolderText = BasicComponentFactory.createTextField(
            viaPowerFolderModel, false);
        viaPowerFolderText.setEnabled(false);
        viaPowerFolderConfigButton = new JButtonMini(Icons.NODE_FRIEND_CONNECTED,
                Translation.getTranslation("send_invitation.select_computer.text"));
        viaPowerFolderConfigButton.setEnabled(decision.getValue()
                == SAVE_TO_FILE);
        viaPowerFolderConfigButton.addActionListener(new MyActionListener());

        emailField.setEnabled(decision.getValue() == SEND_BY_MAIL);
        invitationFileField.setEnabled(decision.getValue() == SAVE_TO_FILE);

        decision.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                emailField.setEnabled(decision.getValue() == SEND_BY_MAIL);
                ccBox.setEnabled(decision.getValue() == SEND_BY_MAIL);
                invitationFileField
                    .setEnabled(decision.getValue() == SAVE_TO_FILE);
                viaPowerFolderConfigButton
                    .setEnabled(decision.getValue() == SEND_DIRECT);
            }
        });
    }

    protected JComponent getPictoComponent() {
        return new JLabel(getContextPicto());
    }

    protected String getTitle() {
        return Translation
            .getTranslation("wizard.send_invitations.send_invitation");
    }

    private void openNodesSelectDialog() {
        NodesSelectDialog dialog = new NodesSelectDialog(getController(),
            viaPowerFolderModel, viaPowerFolderMembers, false);
        dialog.open();
    }

    /**
     * Listen for activation of the via powerfolder button.
     */
    private class MyActionListener implements ActionListener {

        /**
         * Open a UserSelectDialog
         * 
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            openNodesSelectDialog();
        }
    }
}