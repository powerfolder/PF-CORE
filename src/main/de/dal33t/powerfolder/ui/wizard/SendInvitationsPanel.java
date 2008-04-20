/* $Id: SendInvitationsPanel.java,v 1.12 2006/03/06 00:20:55 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.dialog.NodesSelectDialog;
import static de.dal33t.powerfolder.ui.wizard.SendInvitationsPanel.OPTIONS.*;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.MailUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;
import jwf.WizardPanel;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
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
    private JComponent saveToFileButton;
    private JRadioButton sendViaPowerFolderButton;
    private JTextField invitationTextField;

    private ValueModel emailModel;
    private ValueModel invitationFileModel;
    private ValueModel decision;
    private ValueModel viaPowerFolderModel;
    private JTextField viaPowerFolderText;
    private JButton viaPowerFolderConfigButton;

    private final Collection<Member> viaPowerFolderMembers = new ArrayList<Member>();

    public SendInvitationsPanel(Controller controller, boolean showDyndnsSetup)
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
        return InvitationUtil.invitationToMail(getController(), invitation,
            (String) emailModel.getValue());
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

    @Override
    public boolean validateNext(List list) {
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
            "pref, 5dlu, pref",
            "pref, 5dlu, pref, 10dlu, pref, 5dlu, pref, 10dlu, pref, 5dlu, "
                + "pref, 10dlu, pref, 5dlu, pref, 10dlu, pref, 5dlu, pref, 5dlu, " +
                    "pref, 5dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.addLabel(Translation
            .getTranslation("wizard.send_invitations.join"), cc.xyw(1, row, 2));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("wizard.send_invitations.never_untrusted"), cc.xyw(
            1, row, 2));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("wizard.send_invitations.invitation_text"), cc.xyw(
            1, row, 2));

        row += 2;
        builder.add(invitationTextField, cc.xy(1, row));

        if (MailUtil.isSendEmailAvailable()) {
            row += 2;
            builder.add(sendByMailButton, cc.xyw(1, row, 2));
            row += 2;
            builder.add(emailField, cc.xy(1, row));
        }

        row += 2;
        builder.add(saveToFileButton, cc.xyw(1, row, 2));
        row += 2;
        builder.add(invitationFileField, cc.xy(1, row));

        row += 2;
        builder.add(sendViaPowerFolderButton, cc.xyw(1, row, 2));
        row += 2;

        FormLayout layout2 = new FormLayout("pref:grow, 4dlu, 15dlu", "pref");
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
                        Translation.getTranslation("send_invitation.no_users")))
                {
                    openNodesSelectDialog();
                }
            }
        });

        ActionListener action = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                invitationFileModel.setValue(invitation.folder.name
                    + ".invitation");
            }
        };

        invitationFileField = ComplexComponentFactory.createFileSelectionField(
            Translation.getTranslation("wizard.send_invitations.title"),
            invitationFileModel,
            JFileChooser.FILES_ONLY, // Save invitation
            InvitationUtil.createInvitationsFilefilter(), action, null,
            getController());
        // Ensure minimum dimension
        Dimension dims = invitationFileField.getPreferredSize();
        dims.width = Sizes.dialogUnitXAsPixel(147, invitationFileField);
        invitationFileField.setPreferredSize(dims);
        invitationFileField.setBackground(Color.WHITE);

        invitationTextField = new JTextField(Translation.getTranslation(
            "wizard.send_invitations.invitation_text_sample", folder.name));
        JScrollPane invTextScroll = new JScrollPane(invitationTextField);
        invTextScroll.setPreferredSize(new Dimension(50, 80));

        viaPowerFolderModel = new ValueHolder();
        viaPowerFolderModel.setValue(Translation
            .getTranslation("send_invitation.no_users"));
        viaPowerFolderText = BasicComponentFactory.createTextField(
            viaPowerFolderModel, false);
        viaPowerFolderText.setEnabled(false);
        viaPowerFolderConfigButton = new JButton(Icons.NODE_FRIEND_CONNECTED);
        viaPowerFolderConfigButton.setToolTipText(Translation
            .getTranslation("send_invitation.select_user.text"));
        viaPowerFolderConfigButton
            .setEnabled(decision.getValue() == SAVE_TO_FILE);
        viaPowerFolderConfigButton.addActionListener(new MyActionListener());

        emailField.setEnabled(decision.getValue() == SEND_BY_MAIL);
        invitationFileField.setEnabled(decision.getValue() == SAVE_TO_FILE);

        decision.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                emailField.setEnabled(decision.getValue() == SEND_BY_MAIL);
                invitationFileField
                    .setEnabled(decision.getValue() == SAVE_TO_FILE);
                viaPowerFolderConfigButton
                    .setEnabled(decision.getValue() == SEND_DIRECT);
            }
        });

        setPicto((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON));
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.send_invitations.send_invitation");
    }

    private void openNodesSelectDialog() {
        NodesSelectDialog dialog = new NodesSelectDialog(getController(),
            viaPowerFolderModel, viaPowerFolderMembers);
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