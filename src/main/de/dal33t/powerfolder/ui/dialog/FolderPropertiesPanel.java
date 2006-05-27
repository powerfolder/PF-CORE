/* $Id: FolderPropertiesPanel.java,v 1.3 2005/03/13 13:22:00 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.dialog;

import javax.swing.Icon;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;

/**
 * General Property for setting a folder properties
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class FolderPropertiesPanel extends AbstractFolderPanel {
    private Folder folder;

    /**
     * Create a new folder properties panel.
     * 
     * @param controller
     * @param fInfo
     */
    public FolderPropertiesPanel(Controller controller, Folder folder) {
        super(controller, (folder != null) ? folder.getInfo() : null);
        this.folder = folder;
        log().warn(this.folder);
    }

    // Methods fo FolderPreferencesPanel **************************************

    protected void okPressed() {
        close();
    }

    protected void cancelPressed() {
        // Just close dialog
        close();
    }

    protected String getMessage() {
        return "Properties of Folder " + getFolderInfo().name;
    }

    // Methods for BaseDialog *************************************************

    public String getTitle() {
        return "Folder properties";
    }

    protected Icon getIcon() {
        return Icons.FOLDER;
    }
}