/* $Id: LoadInvitationPanel.java,v 1.11 2006/03/04 11:16:39 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.FolderException;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import jwf.WizardPanel;

import javax.swing.*;
import java.util.List;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Class to do folder creation for a specified invite.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class FolderInvitationPanel extends PFWizardPanel {

    private final Invitation invitation;

    private boolean initalized;
    private JLabel folderHintLabel;
    private JLabel folderNameLabel;
    private JLabel invitorHintLabel;
    private JLabel invitorLabel;
    private JLabel invitationMessageHintLabel;
    private JTextField invitationMessageLabel;
    private JLabel estimatedSizeHintLabel;
    private JLabel estimatedSize;
    private JLabel syncProfileHintLabel;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private JCheckBox previewOnlyCB;

    public FolderInvitationPanel(Controller controller, Invitation invitation) {
        super(controller);
        this.invitation = invitation;
    }

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    /**
     * Can procede if an invitation exists.
     */
    public boolean hasNext() {
        return invitation != null;
    }

    public boolean validateNext(List list) {
        return !previewOnlyCB.isSelected() || createPreviewFolder();
    }
    private boolean createPreviewFolder() {

        FolderSettings folderSettings = new FolderSettings(
                invitation.suggestedLocalBase, syncProfileSelectorPanel
                .getSyncProfile(), false, true, true);

        try {
            getController().getFolderRepository().createFolder(
                    invitation.folder, folderSettings);
        } catch (
            FolderException ex) {
            log().error("Unable to create new folder " + invitation.folder,
                    ex);
            ex.show(getController());
            return false;
        }

        return true;
    }

    public WizardPanel next() {

        // Set sync profile
        getWizardContext().setAttribute(
            SYNC_PROFILE_ATTRIBUTE,
            syncProfileSelectorPanel.getSyncProfile());

        // Set folder info
        getWizardContext().setAttribute(
            FOLDERINFO_ATTRIBUTE, invitation.folder);

        // Do not prompt for send invitation afterwards
        getWizardContext().setAttribute(
                SEND_INVIATION_AFTER_ATTRIBUTE, Boolean.FALSE);

        // Whether to open as preview
        getWizardContext().setAttribute(
                PREVIEW_FOLDER_ATTIRBUTE, previewOnlyCB.isSelected());

        // Setup choose disk location panel
        getWizardContext().setAttribute(
                PROMPT_TEXT_ATTRIBUTE,
                Translation.getTranslation("wizard.invite.selectlocaldirectory"));

        // Setup sucess panel of this wizard path
        TextPanelPanel successPanel = new TextPanelPanel(getController(),
            Translation.getTranslation("wizard.setupsuccess"), Translation
                .getTranslation("wizard.successjoin"));
        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
            successPanel);

        // If preview, validateNext has created the folder, so all done.
        if (previewOnlyCB.isSelected()) {
            return (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
        } else {
            return new ChooseDiskLocationPanel(getController(),
                    invitation.suggestedLocalBase.getAbsolutePath());
        }
    }

    public boolean canFinish() {
        return false;
    }

    public void finish() {
    }

    private void buildUI() {
        initComponents();
        loadInvitation();
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout(
                "20dlu, pref, 15dlu, right:pref, 5dlu, pref:grow, 20dlu",
                "15dlu, pref, 5dlu, pref, 15dlu, pref, 5dlu, pref, " +
                        "5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, " +
                        "pref, 5dlu, pref:grow");

        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        // Main title
        builder.add(createTitleLabel(Translation
                .getTranslation("wizard.folder_invitation.title")),
                cc.xywh(4, 2, 3, 1));

        // Wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 9, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        // Invite info
        Member node = invitation.invitor.getNode(getController());
        String invitorString = node != null ? node.getNick() :
                invitation.invitor.nick;
         StringBuilder sb = new StringBuilder(invitation.folder.name + " (");
            if (invitation.folder.secret) {
                sb.append("private");
            } else {
                sb.append("public");
            }
            sb.append(')');
        builder.addLabel(Translation
            .getTranslation("wizard.folder_invitation.intro",
                invitorString,sb.toString()),
                cc.xywh(4, 4, 3, 1));

        // Message

        int row = 6;
        String message = invitation.invitationText;
        if (message != null && message.trim().length() > 0) {
            builder.add(invitationMessageHintLabel, cc.xy(4, row));
            builder.add(invitationMessageLabel, cc.xy(6, row));
            row += 2;
        }

        // Est size
        builder.add(estimatedSizeHintLabel, cc.xy(4, row));
        builder.add(estimatedSize, cc.xy(6, row));
        row += 2;

        // Sync
        builder.add(syncProfileHintLabel, cc.xy(4, row));
        JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
        p.setOpaque(false);
        builder.add(p, cc.xy(6, row));
        row += 2;

        // Preview
        builder.add(previewOnlyCB, cc.xy(6, row));
        row += 2;

        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {

        getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.FILESHARING_PICTO);

        // Folder name label
        folderHintLabel = new JLabel(Translation
            .getTranslation("general.folder"));
        folderHintLabel.setEnabled(false);
        folderNameLabel = SimpleComponentFactory.createLabel();

        // Invitor label
        invitorHintLabel = new JLabel(Translation
            .getTranslation("general.invitor"));
        invitorHintLabel.setEnabled(false);
        invitorLabel = SimpleComponentFactory.createLabel();

        // Invitation messages
        invitationMessageHintLabel = new JLabel(Translation
            .getTranslation("general.message"));
        invitationMessageHintLabel.setEnabled(false);
        invitationMessageLabel = new JTextField();
        invitationMessageLabel.setEditable(false);

        // Estimated size
        estimatedSizeHintLabel = new JLabel(Translation
            .getTranslation("general.estimated_size"));
        estimatedSizeHintLabel.setEnabled(false);
        estimatedSize = SimpleComponentFactory.createLabel();

        // Sync profile
        syncProfileHintLabel = new JLabel(Translation
            .getTranslation("general.synchonisation"));
        syncProfileHintLabel.setEnabled(false);
        syncProfileSelectorPanel = new SyncProfileSelectorPanel(
            getController());
        syncProfileSelectorPanel.setEnabled(false);

        // Preview
        previewOnlyCB = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("general.preview_folder"));
        previewOnlyCB.setOpaque(false);
        previewOnlyCB.setEnabled(false);

        // Do not let user select profile if preview. 
        previewOnlyCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncProfileSelectorPanel.setEnabled(!previewOnlyCB.isSelected());
            }
        });
    }

    private void loadInvitation() {
        log().info("Loaded invitation " + invitation);
        if (invitation != null) {
            folderHintLabel.setEnabled(true);
            StringBuilder sb = new StringBuilder(invitation.folder.name + " (");
            if (invitation.folder.secret) {
                sb.append("private");
            } else {
                sb.append("public");
            }
            sb.append(')');
            folderNameLabel.setText(sb.toString());

            invitorHintLabel.setEnabled(true);
            Member node = invitation.invitor.getNode(getController());
            invitorLabel.setText(node != null
                ? node.getNick()
                : invitation.invitor.nick);

            invitationMessageHintLabel.setEnabled(true);
            invitationMessageLabel.setText(invitation.invitationText == null ?
            "" : invitation.invitationText);

            estimatedSizeHintLabel.setEnabled(true);
            estimatedSize.setText(Format.formatBytes(invitation.folder.bytesTotal)
                    + " (" + invitation.folder.filesCount + ' '
            + Translation.getTranslation("general.files") + ')');

            syncProfileHintLabel.setEnabled(true);
            syncProfileSelectorPanel.setEnabled(true);
            syncProfileSelectorPanel.setSyncProfile(invitation.suggestedProfile,
                    false);

            previewOnlyCB.setEnabled(true);
        } else {
            folderHintLabel.setEnabled(false);
            folderNameLabel.setText("");
            invitorHintLabel.setEnabled(false);
            invitorLabel.setText("");
            invitationMessageHintLabel.setEnabled(false);
            invitationMessageLabel.setText("");
            estimatedSizeHintLabel.setEnabled(false);
            estimatedSize.setText("");
            syncProfileHintLabel.setEnabled(false);
            syncProfileSelectorPanel.setEnabled(false);
            previewOnlyCB.setEnabled(false);
        }
    }
}