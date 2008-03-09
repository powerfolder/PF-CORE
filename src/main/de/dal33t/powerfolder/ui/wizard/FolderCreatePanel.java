/* $Id: LoadInvitationPanel.java,v 1.11 2006/03/04 11:16:39 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SyncProfileSelectorPanel;
import jwf.WizardPanel;

import javax.swing.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
 * Class to do folder creation for an optional specified folderInfo.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class FolderCreatePanel extends PFWizardPanel {

    private final String initialFolderName;

    private boolean initalized;
    private JTextField folderNameTextField;
    private SyncProfileSelectorPanel syncProfileSelectorPanel;
    private JCheckBox sendInviteAfterCB;

    /**
     * Constuctor
     *
     * @param controller
     * @param folderInfo optional FolderIno. Any details are used to prepopulate
     * fields.
     */
    public FolderCreatePanel(Controller controller, String folderName) {
        super(controller);
        initialFolderName = folderName;
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
        return folderNameTextField.getText().trim().length() > 0;
    }

    public WizardPanel next() {

        // Set FolderInfo
        FolderInfo folderInfo = new FolderInfo(
                folderNameTextField.getText().trim(),
                '[' + IdGenerator.makeId() + ']', true);
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, folderInfo);

        // Set sync profile
        getWizardContext().setAttribute(
            SYNC_PROFILE_ATTRIBUTE,
            syncProfileSelectorPanel.getSyncProfile());

        // Do not prompt for send invitation afterwards
        getWizardContext().setAttribute(
                SEND_INVIATION_AFTER_ATTRIBUTE, sendInviteAfterCB.isSelected());

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


        return new ChooseDiskLocationPanel(getController(),
                null);
    }

    public boolean canFinish() {
        return false;
    }

    public void finish() {
    }

    private void buildUI() {
        initComponents();
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout(
                "20dlu, pref, 15dlu, right:pref, 5dlu, pref:grow, 20dlu",
                "5dlu, pref, 15dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref:grow");

        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        // Main title
        builder.add(createTitleLabel(Translation
                .getTranslation("wizard.create_folder.title")),
                cc.xyw(4, 2, 3));

        // Wizard picto
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 6, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        int row = 4;

        // Folder Name
        builder.add(new JLabel(Translation.getTranslation("fileinfo.name")),
                cc.xy(4, row));
        builder.add(folderNameTextField, cc.xy(6, row));
        row += 2;

        // Sync
        builder.add(new JLabel(Translation
            .getTranslation("general.synchonisation")), cc.xy(4, row));
        JPanel p = (JPanel) syncProfileSelectorPanel.getUIComponent();
        p.setOpaque(false);
        builder.add(p, cc.xy(6, row));
        row += 2;

        // Send Invite
        builder.add(sendInviteAfterCB, cc.xy(6, row));
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
        folderNameTextField = SimpleComponentFactory.createTextField(true);
        folderNameTextField.setText(initialFolderName);
        folderNameTextField.addKeyListener(new MyKeyListener());

        // Sync profile
        syncProfileSelectorPanel = new SyncProfileSelectorPanel(
            getController(), SyncProfile.SYNCHRONIZE_PCS);

        // Send Invite
        sendInviteAfterCB = SimpleComponentFactory
                .createCheckBox(Translation
                        .getTranslation("wizard.create_folder.sendinvitation"));
        sendInviteAfterCB.setOpaque(false);
    }

    private class MyKeyListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
            // Not implemented
        }

        public void keyReleased(KeyEvent e) {
            // Not implemented
        }

        public void keyTyped(KeyEvent e) {
            updateButtons();
        }
    }
}