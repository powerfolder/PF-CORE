/* $Id: FolderCreatePanel.java,v 1.23 2006/02/16 13:58:45 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.CreateShortcutAction;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.FolderCreateWorker;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.webservice.WebServiceException;

/**
 * The creation panel for a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.23 $
 */
public class FolderCreatePanel extends AbstractFolderPanel {
    private boolean folderCreated;

    private JCheckBox storeInvitationBox;
    private JCheckBox createShortcutBox;
    private JCheckBox sendInvitationBox;
    private JCheckBox backupByOnlineStorageBox;

    public FolderCreatePanel(Controller controller) {
        super(controller, null);
    }

    public FolderCreatePanel(Controller controller, String presetFolder) {
        this(controller);
        // Initialize valid ValueModels
        getUIComponent();
        File presetDir = new File(presetFolder);
        getNameModel().setValue(presetDir.getName());
        getBaseDirModel().setValue(presetFolder);
    }

    // Application logic ******************************************************

    /**
     * @return the selected base dir
     */
    private File getSelectedBaseDir() {
        if (getBaseDirModel().getValue() == null) {
            return null;
        }
        String baseDir = (String) getBaseDirModel().getValue();
        if (StringUtils.isBlank(baseDir)) {
            return null;
        }
        return new File(baseDir);
    }

    /**
     * Method called when pressed ok
     */
    private void startCreateFolder() {
        String name = (String) getNameModel().getValue();
        File localBase = getSelectedBaseDir();

        if (StringUtils.isBlank(name)) {
            DialogFactory.showErrorDialog(getUIComponent(),
                    Translation.getTranslation("foldercreate.nameempty.title"),
                    Translation.getTranslation("foldercreate.nameempty.text"));
            return;
        }

        if (localBase == null
            || StringUtils.isBlank(localBase.getAbsolutePath()))
        {
            DialogFactory.showErrorDialog(getUIComponent(),
                    Translation.getTranslation("foldercreate.dirempty.title"),
                    Translation.getTranslation("foldercreate.dirempty.text"));
            return;
        }

        if (localBase == null) {
            // Abort
            return;
        }

        // Disable ok button
        getOkButton().setEnabled(false);

        String folderId = "[" + IdGenerator.makeId() + "]";
        boolean secrect = true;

        // Default to the general propery for recycle bin use.
        boolean useRecycleBin = ConfigurationEntry.USE_RECYCLE_BIN
            .getValueBoolean(getController());

        setFolderInfo(new FolderInfo(name, folderId, secrect));

        // Actually create
        MyFolderCreateWorker createWorker = new MyFolderCreateWorker(
            getController(), getFolderInfo(), localBase,
            getSelectedSyncProfile(), storeInvitationBox.isSelected(),
            createShortcutBox.isSelected(), useRecycleBin);
        // Close this dialog on success
        setVisible(false);
        createWorker.start();
    }

    /**
     * @return true if the folder was created successfully
     */
    public boolean folderCreated() {
        return folderCreated;
    }

    // UI Methods *************************************************************

    @Override
    protected JComponent getCustomComponents(String columnSpecs)
    {
        FormLayout layout = new FormLayout(columnSpecs + ", 4dlu, pref",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(storeInvitationBox, cc.xy(3, 1));
        builder.add(createShortcutBox, cc.xy(3, 3));
        builder.add(sendInvitationBox, cc.xy(3, 5));
        builder.add(backupByOnlineStorageBox, cc.xy(3, 7));
        return builder.getPanel();
    }

    // Methods for BaseDialog *************************************************

    public String getTitle() {
        return Translation.getTranslation("foldercreate.dialog.title");
    }

    protected Icon getIcon() {
        return Icons.NEW_FOLDER;
    }

    // Methods fo FolderPreferencesPanel **************************************

    protected String getMessage() {
        return Translation.getTranslation("foldercreate.dialog.settings");
    }

    protected void okPressed() {
        // Start creating
        startCreateFolder();
    }

    protected void cancelPressed() {
        // Just close dialog
        close();
    }

    @Override
    protected void initCustomComponents()
    {
        storeInvitationBox = new JCheckBox(Translation
            .getTranslation("foldercreate.dialog.saveinvitation"));
        storeInvitationBox.setSelected(true);
        createShortcutBox = new JCheckBox((String) getUIController()
            .getFolderCreateShortcutAction().getValue(Action.NAME));
        createShortcutBox.setEnabled(getUIController()
            .getFolderCreateShortcutAction().getValue(
                CreateShortcutAction.SUPPORTED) == Boolean.TRUE);
        // Default to "create shortcut"
        createShortcutBox.setSelected(createShortcutBox.isEnabled());

        sendInvitationBox = new JCheckBox(Translation
            .getTranslation("foldercreate.dialog.sendinvitation"));
        sendInvitationBox.setSelected(true);

        backupByOnlineStorageBox = new JCheckBox(Translation
            .getTranslation("foldercreate.dialog.backupbyonlinestorage"));
        backupByOnlineStorageBox.setSelected(false);
        backupByOnlineStorageBox.getModel().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (backupByOnlineStorageBox.isSelected()) {
                    getUIController().getWebServiceClientModel()
                        .checkAndSetupAccount(false);
                }
            }
        });
    }

    // Creation worker ********************************************************

    /**
     * Worker to create the folder in the background and shows activity
     * visualization. It is highly recommended to read the javadocs from
     * <code>FolderCreateWorker</code>.
     * 
     * @see FolderCreateWorker
     */
    private class MyFolderCreateWorker extends FolderCreateWorker {

        public MyFolderCreateWorker(Controller theController,
            FolderInfo aFoInfo, File aLocalBase, SyncProfile aProfile,
            boolean storeInv, boolean createShortcut, boolean useRecycleBin)
        {
            super(theController, aFoInfo, aLocalBase, aProfile, storeInv,
                createShortcut, useRecycleBin);
        }

        @Override
        public Object construct()
        {
            Object o = super.construct();

            if (getFolderException() == null
                && backupByOnlineStorageBox.isSelected()
                && getController().getWebServiceClient().isLastLoginOK())
            {
                try {
                    getController().getWebServiceClient().setupFolder(
                        getFolder());
                } catch (WebServiceException e) {
                    getUIController()
                        .showWarningMessage(
                            Translation
                                .getTranslation("foldercreate.dialog.backuperror.title"),
                            Translation
                                .getTranslation("foldercreate.dialog.backuperror.text"));
                    log().error("Unable to backup folder to online storage", e);
                }
            }
            return o;
        }

        @Override
        public void finished()
        {
            if (getFolderException() != null) {
                // Show error
                getFolderException().show(getController());
                getOkButton().setEnabled(true);
                setVisible(true);
            } else {
                folderCreated = true;
                close();
                if (sendInvitationBox.isSelected()) {
                    PFWizard.openSendInvitationWizard(getController(),
                        getFolderInfo());
                }
            }
        }
    }
}