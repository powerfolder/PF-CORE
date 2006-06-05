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
import de.dal33t.powerfolder.util.Translation;

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
    
    private JCheckBox storeInvitation;

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
    private boolean createFolder() {
        String name = (String) getNameModel().getValue();
        File localBase = getSelectedBaseDir();

        if (StringUtils.isBlank(name)) {
            JOptionPane.showMessageDialog(getUIComponent(), Translation
                .getTranslation("foldercreate.nameempty.text"), Translation
                .getTranslation("foldercreate.nameempty.title"),
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (localBase == null
            || StringUtils.isBlank(localBase.getAbsolutePath()))
        {
            JOptionPane.showMessageDialog(getUIComponent(), Translation
                .getTranslation("foldercreate.dirempty.text"), Translation
                .getTranslation("foldercreate.dirempty.title"),
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String folderId = "[" + IdGenerator.makeId() + "]";
        boolean secrect = ((Boolean) getSecrectModel().getValue())
            .booleanValue();
        FolderInfo foInfo = new FolderInfo(name, folderId, secrect);

        if (localBase == null) {
            // Abort
            return false;
        }

        try {
            // Set sync profile
            SyncProfile profile = getSelectedSyncProfile();

            getController().getFolderRepository().createFolder(foInfo,
                localBase, profile, storeInvitation.isSelected());
            
            return true;
        } catch (FolderException ex) {
            log().error("Unable to create new folder " + foInfo, ex);
            // Show error
            ex.show(getController());
            return false;
        }
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
        return Translation.getTranslation("foldercreate.dialog.title"); //$NON-NLS-1$
    }

    protected Icon getIcon() {
        return Icons.NEW_FOLDER;
    }

    // Methods fo FolderPreferencesPanel **************************************

    protected String getMessage() {
        return Translation.getTranslation("foldercreate.dialog.settings");
    }

    protected void okPressed() {
        folderCreated = createFolder();
        // Close dialog
        if (folderCreated) {
            close();
        }
    }

    protected void cancelPressed() {
        // Just close dialog
        close();
    }

    @Override
    protected int appendCustomComponents(PanelBuilder builder, int row) {
        CellConstraints cc = new CellConstraints();
        
        row += 2;
        builder.add(storeInvitation, cc.xy(3, row));
        return row;
    }

    @Override
    protected String getCustomRows() {
        return "3dlu, pref, "; 
    }

    @Override
    protected void initCustomComponents() {
        storeInvitation = new JCheckBox(Translation
            .getTranslation("foldercreate.dialog.saveinvitation"));
    }
}