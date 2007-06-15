/* $Id: ChooseDiskLocationPanel.java,v 1.9 2005/11/20 04:26:09 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;

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
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;

/**
 * A generally used wizard panel for choosing a disk location for a folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class ChooseDiskLocationPanel extends PFWizardPanel {
    private boolean initalized = false;

    /** The attribute in wizard context, which will be displayed */
    public final static String PROMPT_TEXT_ATTRIBUTE = "disklocation.prompttext";
    /** The folder info object for the targeted folder */
    public final static String FOLDERINFO_ATTRIBUTE = "disklocation.folderinfo";
    /** The folder info object for the targeted folder */
    public final static String SYNC_PROFILE_ATTRIBUTE = "disklocation.syncprofile";
    /**
     * Determines, if the user should be prompted for sending invitation
     * afterwards
     */
    public final static String SEND_INVIATION_AFTERWARDS = "disklocation.sendinvitations";

    private JComponent locationField;
    private ValueModel locationModel;
    private Folder folder;
    private boolean folderCreated;
    private boolean sendInvitations;

    /**
     * Creates a new disk location wizard panel. Name of new folder is
     * automatically generated, folder will be secret
     * 
     * @param controller
     */
    public ChooseDiskLocationPanel(Controller controller) {
        super(controller);
    }

    // Application logic ******************************************************

    /**
     * Method called when pressed ok
     */
    private boolean createFolder() {
        if (StringUtils.isBlank((String) locationModel.getValue())) {
            // Abort
            return false;
        }

        File localBase = new File((String) locationModel.getValue());
        FolderInfo foInfo = (FolderInfo) getWizardContext().getAttribute(
            FOLDERINFO_ATTRIBUTE);
        SyncProfile syncProfile = (SyncProfile) getWizardContext()
            .getAttribute(SYNC_PROFILE_ATTRIBUTE);

        if (syncProfile == null) {
            throw new IllegalArgumentException(
                "Synchronisation profile not set !");
        }

        if (foInfo == null) {
            // Create new folder info
            String name = getController().getMySelf().getNick() + "-"
                + localBase.getName();

            String folderId = "[" + IdGenerator.makeId() + "]";
            boolean secrect = true;

            foInfo = new FolderInfo(name, folderId, secrect);
        }
        boolean useRecycleBin = ConfigurationEntry.USE_RECYCLE_BIN.
                        getValueBoolean(getController());

        Boolean sendInvs = (Boolean) getWizardContext().getAttribute(
            SEND_INVIATION_AFTERWARDS);
        sendInvitations = sendInvs == null || sendInvs.booleanValue();

        // Set attribute
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, foInfo);

        try {
            FolderSettings folderSettings =
                    new FolderSettings(localBase, syncProfile, false, useRecycleBin);
            folder = getController().getFolderRepository().createFolder(foInfo,
                    folderSettings);
            if (OSUtil.isWindowsSystem()) {
                // Add thumbs to ignore pattern on windows systems
                folder.getBlacklist().addPattern("*Thumbs.db");
            }
            log().info(
                "Folder '" + foInfo.name
                    + "' created successfully. local copy at "
                    + localBase.getAbsolutePath());
            return true;
        } catch (FolderException ex) {
            log().error("Unable to create new folder " + foInfo, ex);
            ex.show(getController());
            return false;
        }
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return locationModel.getValue() != null
            && !StringUtils.isBlank(locationModel.getValue().toString());
    }

    public boolean validateNext(List list) {
        folderCreated = createFolder();
        
        if (folderCreated
            && folder.getSyncProfile() == SyncProfile.PROJECT_WORK)
        {
            // Show sync folder panel after created a project folder
            new SyncFolderPanel(getController(), folder).open();
        }

        return folderCreated;
    }

    public WizardPanel next() {
        WizardPanel next;
        if (sendInvitations) {
            next = new SendInvitationsPanel(getController(), true);
        } else {
            next = (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
        }
        return next;
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

        FormLayout layout = new FormLayout("20dlu, pref, 15dlu, left:pref",
            "5dlu, pref, 15dlu, pref, 4dlu, pref, pref:grow");

        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.choosedisklocation.select")), cc.xy(4, 2)); // Select
        // directory
        // Add current wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 3, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        String infoText = (String) getWizardContext().getAttribute(
            PROMPT_TEXT_ATTRIBUTE);
        builder.addLabel(infoText, cc.xy(4, 4));

        builder.add(locationField, cc.xy(4, 6));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        locationModel = new ValueHolder();

        // Behavior
        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });

        FolderInfo folder = (FolderInfo) getWizardContext().getAttribute(
            FOLDERINFO_ATTRIBUTE);

        locationField = ComplexComponentFactory
            .createFolderBaseDirSelectionField(new ValueHolder(folder != null
                ? folder.name
                : ""), locationModel, getController());
        Dimension dims = locationField.getPreferredSize();
        dims.width = Sizes.dialogUnitXAsPixel(147, locationField);
        locationField.setPreferredSize(dims);
        locationField.setBackground(Color.WHITE);
    }
}