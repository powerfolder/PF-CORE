/* $Id: LoadInvitationPanel.java,v 1.11 2006/03/04 11:16:39 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.FolderException;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PREVIEW_FOLDER_ATTIRBUTE;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import jwf.WizardPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.List;

/**
 * Class that selects an invitation then does the folder setup for that invite.
 * 
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class LoadInvitationPanel extends PFWizardPanel {

    private boolean initalized;
    private JComponent locationField;
    private Invitation invitation;
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

    public LoadInvitationPanel(Controller controller) {
        super(controller);
    }

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    /**
     * Can procede if an invitation is selected.
     */

    public boolean hasNext() {
        return invitation != null;
    }

    public WizardPanel next() {

        // Set sync profile
        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
            syncProfileSelectorPanel.getSyncProfile());

        // Set folder info
        getWizardContext()
            .setAttribute(FOLDERINFO_ATTRIBUTE, invitation.folder);

        // Do not prompt for send invitation afterwards
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
            Boolean.FALSE);

        // Whether to load as preview
        getWizardContext().setAttribute(PREVIEW_FOLDER_ATTIRBUTE,
            previewOnlyCB.isSelected());

        // If preview, validateNext has created the folder, so all done.
        if (previewOnlyCB.isSelected()) {
            return (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
        } else {
            return new ChooseDiskLocationPanel(getController(),
                invitation.suggestedLocalBase.getAbsolutePath(),
                new FolderCreatePanel(getController()));
        }
    }

    public boolean canFinish() {
        return false;
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
        } catch (FolderException ex) {
            log().error("Unable to create new folder " + invitation.folder, ex);
            ex.show(getController());
            return false;
        }

        return true;
    }

    public void finish() {
    }

    private void buildUI() {
        initComponents();
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout(
            "20dlu, pref, 15dlu, right:pref, 5dlu, pref:grow, 20dlu",
            "5dlu, pref, 15dlu, pref, 5dlu, pref, 15dlu, pref, 5dlu, pref, "
                + "5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, "
                + "pref, 5dlu, pref:grow");

        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        // Main title
        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.loadinvitation.select")), cc.xywh(4, 2, 3,
            1));

        // Wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 9, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        // Please select invite...
        builder.addLabel(Translation
            .getTranslation("wizard.loadinvitation.selectfile"), cc.xy(6, 4));

        // Invite selector
        builder.add(locationField, cc.xy(6, 6));

        // Folder
        builder.add(folderHintLabel, cc.xy(4, 8));
        builder.add(folderNameLabel, cc.xy(6, 8));

        // From
        builder.add(invitorHintLabel, cc.xy(4, 10));
        builder.add(invitorLabel, cc.xy(6, 10));

        // Message
        builder.add(invitationMessageHintLabel, cc.xy(4, 12));
        builder.add(invitationMessageLabel, cc.xy(6, 12));

        // Est size
        builder.add(estimatedSizeHintLabel, cc.xy(4, 14));
        builder.add(estimatedSize, cc.xy(6, 14));

        // Sync
        builder.add(syncProfileHintLabel, cc.xy(4, 16));
        JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
        p.setOpaque(false);
        builder.add(p, cc.xy(6, 16));

        // Preview
        builder.add(previewOnlyCB, cc.xy(6, 18));

        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {

        ValueModel locationModel = new ValueHolder();

        // Invite behavior
        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                loadInvitation((String) evt.getNewValue());
                updateButtons();
            }
        });

        // Invite selector
        locationField = ComplexComponentFactory.createFileSelectionField(
            Translation.getTranslation("wizard.loadinvitation.choosefile"),
            locationModel, JFileChooser.FILES_AND_DIRECTORIES, InvitationUtil
                .createInvitationsFilefilter(), null, null, getController());
        Dimension dims = locationField.getPreferredSize();
        dims.width = Sizes.dialogUnitXAsPixel(147, locationField);
        locationField.setPreferredSize(dims);
        locationField.setBackground(Color.WHITE);

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
        syncProfileSelectorPanel = new SyncProfileSelectorPanel(getController());
        syncProfileSelectorPanel.setEnabled(false);

        // Preview
        previewOnlyCB = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("general.preview_folder"));
        previewOnlyCB.setOpaque(false);
        previewOnlyCB.setEnabled(false);

        // Do not let user select profile if preview.
        previewOnlyCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                syncProfileSelectorPanel
                    .setEnabled(!previewOnlyCB.isSelected());
            }
        });
    }

    private void loadInvitation(String file) {
        if (file == null) {
            return;
        }
        invitation = InvitationUtil.load(new File(file));
        log().info("Loaded invitation " + invitation);
        if (invitation != null) {
            folderHintLabel.setEnabled(true);
            folderNameLabel.setText(invitation.folder.name);

            invitorHintLabel.setEnabled(true);
            Member node = invitation.invitor.getNode(getController());
            invitorLabel.setText(node != null
                ? node.getNick()
                : invitation.invitor.nick);

            invitationMessageHintLabel.setEnabled(true);
            invitationMessageLabel.setText(invitation.invitationText == null
                ? ""
                : invitation.invitationText);

            estimatedSizeHintLabel.setEnabled(true);
            estimatedSize.setText(Format
                .formatBytes(invitation.folder.bytesTotal)
                + " ("
                + invitation.folder.filesCount
                + ' '
                + Translation.getTranslation("general.files") + ')');

            syncProfileHintLabel.setEnabled(true);
            syncProfileSelectorPanel.setEnabled(true);
            syncProfileSelectorPanel.setSyncProfile(
                invitation.suggestedProfile, false);

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