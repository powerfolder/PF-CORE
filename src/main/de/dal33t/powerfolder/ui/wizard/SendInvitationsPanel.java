/* $Id: SendInvitationsPanel.java,v 1.12 2006/03/06 00:20:55 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jwf.WizardPanel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.net.NodeManager;
import de.dal33t.powerfolder.ui.render.PFListCellRenderer;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.MailUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.MemberComparator;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class SendInvitationsPanel extends PFWizardPanel {
    // The options of this screen
    private static final Object SAVE_TO_FILE_OPTION = new Object();
    private static final Object SEND_BY_MAIL_OPTION = new Object();
    private static final Object SEND_DIRECT_OPTION = new Object();

    private boolean initalized = false;

    private boolean showDyndnsSetup;
    private boolean firstFocusGainOfEmailField;
    private Invitation invitation;
    private JComponent invitationFileField;
    private JComponent sendByMailButton;
    private JComponent emailField;
    private JComponent saveToFileButton;
    private JComponent sendViaPowerFolderButton;
    private JComboBox nodeSelectionBox;
    private JTextArea invitationText;
    private JScrollPane invTextScroll;

    private ValueModel emailModel;
    private ValueModel invitationFileModel;
    private ValueModel nodeSelectionModel;
    private ValueModel decision;

    public SendInvitationsPanel(Controller controller, boolean showDyndnsSetup)
    {
        super(controller);
        this.showDyndnsSetup = showDyndnsSetup;
        this.firstFocusGainOfEmailField = true;
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
     * Handles the invitation to a node option.
     * 
     * @return true if send otherwise false
     */
    private boolean sendInvitationToNode() {
        if (invitation == null) {
            return false;
        }
        if (nodeSelectionModel.getValue() instanceof Member) {
            InvitationUtil.invitationToNode(getController(), invitation,
                (Member) nodeSelectionModel.getValue());
            // Do not evaulate return value. Because invitation is always sent
            // or enqueued for later sending.
            return true;
        }

        return false;
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return true;
    }

    @Override
    public boolean validateNext(List list)
    {
        boolean ok = false;
        invitation.invitationText = invitationText.getText();
        if (decision.getValue() == SEND_BY_MAIL_OPTION) {
            // Send by email
            ok = sendInvitationByMail();
        } else if (decision.getValue() == SAVE_TO_FILE_OPTION) {
            // Store now
            ok = saveInvitationToFile();
        } else if (decision.getValue() == SEND_DIRECT_OPTION) {
            // Send now
            ok = sendInvitationToNode();
        }
        return ok;
    }

    public WizardPanel next() {
        if (getController().getConnectionListener().getMyDynDns() == null
            && showDyndnsSetup)
        {
            return new SetupDnsPanel(getController());
        }
        // Show success panel
        return (WizardPanel) getWizardContext().getAttribute(
            PFWizard.SUCCESS_PANEL);
    }

    public boolean canFinish() {
        return false;
    }

    public void finish() {
    }

    // UI building ************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {
        // init
        initComponents();

        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout(
            "20dlu, pref, 15dlu, fill:120dlu, left:pref:grow",
            "5dlu, pref, 15dlu, pref, 3dlu, pref, 14dlu, pref, 4dlu, pref, 10dlu, "
                + "pref, 4dlu, pref, 10dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, pref:grow");

        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        int row = 2;
        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.sendinvitations.sendinvitation")), cc.xyw(
            4, row, 2));

        row += 2;
        // Add current wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, row, 1, 3,
            CellConstraints.DEFAULT, CellConstraints.TOP));

        builder.addLabel(Translation
            .getTranslation("wizard.sendinvitations.joinsync"), cc.xyw(4, row,
            2));

        row += 2;
        builder.addLabel(Translation
            .getTranslation("wizard.sendinvitations.neveruntrusted"), cc.xyw(4,
            row, 2));

        if (MailUtil.isSendEmailAvailable()) {
            row += 2;
            builder.add(sendByMailButton, cc.xyw(4, row, 2));
            row += 2;
            builder.add(emailField, cc.xy(4, row));
        }

        row += 2;
        builder.add(saveToFileButton, cc.xyw(4, row, 2));
        row += 2;
        builder.add(invitationFileField, cc.xy(4, row));

        row += 2;
        builder.add(sendViaPowerFolderButton, cc.xyw(4, row, 2));
        row += 2;
        builder.add(nodeSelectionBox, cc.xy(4, row));

        // row += 2;
        // builder.addLabel(Translation
        // .getTranslation("wizard.sendinvitations.additionaltext"),
        // cc.xyw(4, row, 2));

        // row += 2;
        // builder.add(invTextScroll, cc.xy(4, row));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        FolderInfo folder = (FolderInfo) getWizardContext().getAttribute(
            ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE);
        Reject.ifNull(folder, "Unable to send invitation, folder is null");

        // Clear folder attribute
        getWizardContext().setAttribute(
            ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE, null);

        invitation = new Invitation(folder, getController().getMySelf()
            .getInfo());
        invitation.suggestedProfile = folder.getFolder(getController())
            .getSyncProfile();

        // targetHolder = new ValueHolder();
        invitationFileModel = new ValueHolder();
        emailModel = new ValueHolder("email@host.de");
        decision = new ValueHolder(SEND_BY_MAIL_OPTION, true);

        sendByMailButton = BasicComponentFactory.createRadioButton(decision,
            SEND_BY_MAIL_OPTION, Translation
                .getTranslation("wizard.sendinvitations.sendbymail"));
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
            SAVE_TO_FILE_OPTION, Translation
                .getTranslation("wizard.sendinvitations.savetofile"));
        saveToFileButton.setOpaque(false);

        sendViaPowerFolderButton = BasicComponentFactory.createRadioButton(
            decision, SEND_DIRECT_OPTION, Translation
                .getTranslation("wizard.sendinvitations.overpf"));
        sendViaPowerFolderButton.setOpaque(false);

        nodeSelectionModel = new ValueHolder();
        nodeSelectionBox = SimpleComponentFactory
            .createComboBox(nodeSelectionModel);
        nodeSelectionBox.setRenderer(new PFListCellRenderer());
        refreshNodeSelectionBox();

        ActionListener action = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                invitationFileModel.setValue(invitation.folder.name
                    + ".invitation");
            }
        };

        invitationFileField = ComplexComponentFactory.createFileSelectionField(
            Translation.getTranslation("wizard.sendinvitations.title"),
            invitationFileModel, JFileChooser.FILES_ONLY, // Save invitation
            InvitationUtil.createInvitationsFilefilter(), action);
        // Ensure minimum dimension
        Dimension dims = invitationFileField.getPreferredSize();
        dims.width = Sizes.dialogUnitXAsPixel(147, invitationFileField);
        invitationFileField.setPreferredSize(dims);
        invitationFileField.setBackground(Color.WHITE);

        invitationText = new JTextArea();
        invTextScroll = new JScrollPane(invitationText);
        invTextScroll.setPreferredSize(new Dimension(50, 80));

        decision.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                emailField
                    .setEnabled(decision.getValue() == SEND_BY_MAIL_OPTION);
                invitationFileField
                    .setEnabled(decision.getValue() == SAVE_TO_FILE_OPTION);
                nodeSelectionBox
                    .setEnabled(decision.getValue() == SEND_DIRECT_OPTION);
                if (decision.getValue() == SEND_DIRECT_OPTION) {
                    refreshNodeSelectionBox();
                }
            }
        });
        emailField.setEnabled(decision.getValue() == SEND_BY_MAIL_OPTION);
        invitationFileField
            .setEnabled(decision.getValue() == SAVE_TO_FILE_OPTION);
        nodeSelectionBox.setEnabled(decision.getValue() == SEND_DIRECT_OPTION);
    }

    /**
     * Refreshes the list of nodes from core.
     */
    @SuppressWarnings("unchecked")
    private void refreshNodeSelectionBox()
    {
        nodeSelectionBox.removeAllItems();
        SortedSet<Member> nodes = new TreeSet<Member>(MemberComparator.NICK);
        NodeManager nm = getController().getNodeManager();
        nodes.addAll(nm.getConnectedNodes());
        nodes.addAll(Arrays.asList(nm.getFriends()));
        for (Member m : nm.getValidNodes()) {
            if (m.isOnLAN()) {
                nodes.add(m);
            }
        }
        // Collections.sort(nodes, MemberComparator.NICK);
        boolean noneOnline = true;
        for (Member member : nodes) {
            if (member.isFriend() || member.isOnLAN()) {
                nodeSelectionBox.addItem(member);
                noneOnline = false;
            }
        }
        if (noneOnline) {
            nodeSelectionBox.addItem(Translation
                .getTranslation("wizard.sendinvitations.offline"));
        }
    }

}