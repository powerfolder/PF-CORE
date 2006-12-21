/* $Id: FolderCreatePanel.java,v 1.23 2006/02/16 13:58:45 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.CreateShortcutAction;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.FolderCreateWorker;

/**
 * The creation panel for a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.23 $
 */
public class FolderCreatePanel extends AbstractFolderPanel {
    private boolean folderCreated;

    private JCheckBox storeInvitationBox;
    private JCheckBox cbCreateShortcut;

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
            JOptionPane.showMessageDialog(getUIComponent(), Translation
                .getTranslation("foldercreate.nameempty.text"), Translation
                .getTranslation("foldercreate.nameempty.title"),
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (localBase == null
            || StringUtils.isBlank(localBase.getAbsolutePath()))
        {
            JOptionPane.showMessageDialog(getUIComponent(), Translation
                .getTranslation("foldercreate.dirempty.text"), Translation
                .getTranslation("foldercreate.dirempty.title"),
                JOptionPane.ERROR_MESSAGE);
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

        setFolderInfo(new FolderInfo(name, folderId, secrect));

        // Actually create
        MyFolderCreateWorker createWorker = new MyFolderCreateWorker(
            getController(), getFolderInfo(), localBase,
            getSelectedSyncProfile(), storeInvitationBox.isSelected(),
            cbCreateShortcut.isSelected());
        // Close this dialog on success
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
            "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(storeInvitationBox, cc.xy(3, 1));
        builder.add(cbCreateShortcut, cc.xy(3, 3));
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
        cbCreateShortcut = new JCheckBox((String) getUIController()
            .getFolderCreateShortcutAction().getValue(Action.NAME));
        cbCreateShortcut.setEnabled(getUIController()
            .getFolderCreateShortcutAction().getValue(
                CreateShortcutAction.SUPPORTED) == Boolean.TRUE);
        // Default to "create shortcut"
        cbCreateShortcut.setSelected(cbCreateShortcut.isEnabled());
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
            boolean storeInv, boolean createShortcut)
        {
            super(theController, aFoInfo, aLocalBase, aProfile, storeInv,
                createShortcut);
        }

        @Override
        public void finished()
        {
            if (getFolderException() != null) {
                // Show error
                getFolderException().show(getController());
                getOkButton().setEnabled(true);
            } else {
                folderCreated = true;
                close();
            }
        }
    }
}