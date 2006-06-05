/* $Id: FolderCreatePanel.java,v 1.23 2006/02/16 13:58:45 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.io.File;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.ActivityVisualisationWorker;
import de.dal33t.powerfolder.util.ui.BaseDialog;

/**
 * The creation panel for a folder
 * <p>
 * TODO TOT Do folder creation in SwingWorker s
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.23 $
 */
public class FolderCreatePanel extends AbstractFolderPanel {
    private boolean folderCreated;

    private JCheckBox storeInvitationBox;

    public FolderCreatePanel(Controller controller) {
        super(controller, null);
    }

    // Application logic ******************************************************

    /**
     * Returns the selected base dir
     * 
     * @return
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
        boolean secrect = ((Boolean) getSecrectModel().getValue())
            .booleanValue();
        FolderInfo foInfo = new FolderInfo(name, folderId, secrect);

        // Actually create
        FolderCreateWorker createWorker = new FolderCreateWorker(
            getController(), foInfo, localBase, getSelectedSyncProfile(),
            storeInvitationBox.isSelected());
        // Close this dialog on success
        createWorker.setDialogToClose(this);
        createWorker.start();
    }

    /**
     * Answers if the folder was created successfully
     * 
     * @return
     */
    public boolean folderCreated() {
        return folderCreated;
    }

    // UI Methods *************************************************************

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
    protected int appendCustomComponents(PanelBuilder builder, int row)
    {
        CellConstraints cc = new CellConstraints();

        row += 2;
        builder.add(storeInvitationBox, cc.xy(3, row));
        return row;
    }

    @Override
    protected String getCustomRows()
    {
        return "3dlu, pref, ";
    }

    @Override
    protected void initCustomComponents()
    {
        storeInvitationBox = new JCheckBox(Translation
            .getTranslation("foldercreate.dialog.saveinvitation"));
    }

    // Creation worker ********************************************************

    /**
     * Worker that helps to create a folder in the UI environment.
     * <p>
     * It prevents whitescreens/UI freezes when creating a folder, since the
     * actual creation is executed in a background thread.
     * <p>
     * Will display activity visualisation when the creation process is taking
     * longer.
     */
    private class FolderCreateWorker extends ActivityVisualisationWorker
    {
        private final Logger LOG = Logger
            .getLogger(FolderCreatePanel.class);

        private Controller controller;
        private FolderInfo foInfo;
        private File localBase;
        private SyncProfile syncProfile;
        private boolean storeInvitation;
        private BaseDialog dialog;

        public FolderCreateWorker(Controller theController, FolderInfo aFoInfo,
            File aLocalBase, SyncProfile aProfile, boolean storeInv)
        {
            super(theController.getUIController().getMainFrame()
                .getUIComponent());
            Reject.ifNull(aFoInfo, "FolderInfo is null");
            Reject.ifNull(aLocalBase, "Folder local basedir is null");
            Reject.ifNull(aProfile, "Syncprofile is null");

            controller = theController;
            foInfo = aFoInfo;
            localBase = aLocalBase;
            syncProfile = aProfile;
            storeInvitation = storeInv;
        }

        /**
         * Optional dialog. The dialog will be closed after the folder was
         * successfully created
         * 
         * @param aDialog
         *            the dialog to close after successfully folder creation
         */
        public void setDialogToClose(BaseDialog aDialog) {
            dialog = aDialog;
        }

        @Override
        protected String getTitle()
        {
            return Translation.getTranslation("foldercreate.progress.text",
                foInfo.name);
        }

        @Override
        protected String getWorkingText()
        {
            return Translation.getTranslation("foldercreate.progress.text",
                foInfo.name);
        }

        @Override
        public Object construct()
        {
            try {
                controller.getFolderRepository().createFolder(foInfo,
                    localBase, syncProfile, storeInvitation);
                return Boolean.TRUE;
            } catch (FolderException ex) {
                LOG.error("Unable to create new folder " + foInfo, ex);
                // Show error
                ex.show(controller);
                return Boolean.FALSE;
            }
        }

        @Override
        public void finished()
        {
            // TODO Find an abstract way for this
            if (Boolean.FALSE.equals(get())) {
                getOkButton().setEnabled(true);
            }
            // Close dialog
            if (dialog != null && Boolean.TRUE.equals(get())) {
                dialog.close();
            }
        }
    }
}