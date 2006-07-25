package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JFileChooser;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;

/**
 * Load an Invitation from disk
 * 
 * @version $Revision: 1.4 $
 */
public class FolderInvitationAction extends BaseAction {

    public FolderInvitationAction(Controller controller) {
        super("join_by_invitation", controller);
    }

    /**
     * Opens a dialog to load invitation from disk
     */
    public void actionPerformed(ActionEvent e) {
        // Select file from disk
        JFileChooser fc = DialogFactory.createFileChooser();
        fc.setDialogTitle(Translation.getTranslation("loadinvitation.title"));
        fc.setFileFilter(InvitationUtil.createInvitationsFilefilter());
        int result = fc.showOpenDialog(getController().getUIController()
            .getMainFrame().getUIComponent());
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        // Store invitation to disk
        File file = fc.getSelectedFile();
        if (file == null) {
            return;
        }

        log().debug("Loading invitation from " + file);

        // Load invitation from disk
        Invitation invitation = InvitationUtil.load(file);
        if (invitation == null) {
            unableToReadInvitation(file);
        }

        log().verbose(
            "Disk invitation to " + invitation.folder + " from "
                + invitation.invitor);

        if (getController().getFolderRepository().hasJoinedFolder(
            invitation.folder))
        {
            getController().getUIController().showWarningMessage(
                "Already on folder",
                "Unable to join folder '" + invitation.folder.name
                    + "', already joined");
        } else {
            // Invitation received
            getController().getFolderRepository().invitationReceived(
                invitation, false, false);
        }
    }

    /**
     * Shows an error dialog
     * 
     * @param file
     */
    private void unableToReadInvitation(File file) {
        getController().getUIController().showErrorMessage(
            "Unable to read invitation",
            "Error while reading invitation file\n'" + file.getAbsolutePath()
                + "'.", null);
    }
}
