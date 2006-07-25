/* $Id: SendInvitationsPanel.java,v 1.12 2006/03/06 00:20:55 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

import javax.swing.*;

import jwf.WizardPanel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class SendInvitationsPanel extends PFWizardPanel {
    private boolean initalized = false;

    private Invitation invitation;
    private JComponent invitationFileField;
    private ValueModel invitationFileModel;

    public SendInvitationsPanel(Controller controller) {
        super(controller);
    }

    // Application logic

    /**
     * Handles the invitation to disk option.
     * <P>
     * COPIED FROM InviteAction.java.
     * <p>
     * TODO: Consolidate code
     * 
     * @return true if saved otherwise false
     */
    private boolean storeInvitation() {
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
        log().warn("Writing invitation to " + file);
        try {
            ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)));
            out.writeObject(invitation);
            out.writeObject(getController().getMySelf().getInfo());
            out.close();

            return true;
        } catch (IOException e) {
            getController().getUIController().showErrorMessage(
                "Unable to write invitation",
                "Error while writing invitation, please try again.", e);
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

    public WizardPanel next() {
        // Store now
        storeInvitation();

        if (getController().getConnectionListener().getMyDynDns() == null) {
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

        //        setBorder(new TitledBorder(Translation
        //            .getTranslation("wizard.sendinvitations.title"))); //Save
        // invitation
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout("20dlu, pref, 15dlu, left:pref",
            "5dlu, pref, 15dlu, pref, pref, pref, pref, 4dlu, pref, pref:grow");

        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.sendinvitations.savefile")), cc.xy(4, 2)); // Save
        // invitation
        // file

        // Add current wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 3, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        builder.addLabel(Translation
            .getTranslation("wizard.sendinvitations.joinsync"), cc.xy(4, 4)); //Invitation
        // files
        // are
        // required
        // to
        // join
        // a
        // synchronization
        builder
            .addLabel(Translation
                .getTranslation("wizard.sendinvitations.passwdasfile"), cc.xy(
                4, 5)); //It is similar to a password, but stored as file
        builder.addLabel(Translation
            .getTranslation("wizard.sendinvitations.neveruntrusted"), cc.xy(4,
            6));//Never give out invitation files to untrusted people
        builder.addLabel(Translation
            .getTranslation("wizard.sendinvitations.create"), cc.xy(4, 7));

        //        builder.addLabel(
        //            "Either you write an invitation to a file, or send it online now",
        //            cc.xywh(3, 5, 2, 1));

        //builder.add(selectFileButton, cc.xy(3, 6));
        //
        //        builder.add(targetBox, cc.xy(3, 7));
        builder.add(invitationFileField, cc.xy(4, 9));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        FolderInfo folder = (FolderInfo) getWizardContext().getAttribute(
            ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE);

        // Clear folder attribute
        getWizardContext().setAttribute(
            ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE, null);

        invitation = new Invitation(folder, getController().getMySelf()
            .getInfo());

        //        targetHolder = new ValueHolder();
        invitationFileModel = new ValueHolder();

        //
        //        targetBox = SimpleComponentFactory.createComboBox(targetHolder);
        //        targetBox.setRenderer(new PFListCellRenderer(getController()));
        //
        //        // Add options
        //        targetBox.addItem("To disk... (select file below)");
        //        Member[] nodes = getController().getNodeManager().getNodes();
        //        // Sort
        //        Arrays.sort(nodes, MemberComparator.IN_GUI);
        //
        //        for (int i = 0; i < nodes.length; i++) {
        //            if (nodes[i].isConnected() && !nodes[i].isMySelf())
        //                // only show as invitation option, if not already in folder
        //                targetBox.addItem(nodes[i]);
        //        }
        //

        ActionListener action = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                invitationFileModel.setValue(invitation.folder.name
                    + ".invitation");
            }
        };

        invitationFileField = ComplexComponentFactory.createFileSelectionField(
            Translation.getTranslation("wizard.sendinvitations.title"),
            invitationFileModel, JFileChooser.FILES_ONLY, //Save invitation
            InvitationUtil.createInvitationsFilefilter(), action);
        // Ensure minimum dimension
        Dimension dims = invitationFileField.getPreferredSize();
        dims.width = Sizes.dialogUnitXAsPixel(147, invitationFileField);
        invitationFileField.setPreferredSize(dims);
        invitationFileField.setBackground(Color.WHITE);

        //
        //        // Add behavior, disk location field only visible when "To disk..."
        //        // option selected
        //        targetHolder.addValueChangeListener(new PropertyChangeListener() {
        //            public void propertyChange(PropertyChangeEvent evt) {
        //                invitationFileField
        //                    .setVisible(!(evt.getNewValue() instanceof Member));
        //            }
        //        });
    }

}