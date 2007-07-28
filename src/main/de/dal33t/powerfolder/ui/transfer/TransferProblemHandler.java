package de.dal33t.powerfolder.ui.transfer;

import java.awt.Frame;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.TransferProblem;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * Utility class to display information about file transfer problems.
 */
public class TransferProblemHandler extends TransferAdapter {
    private Controller controller;

    public TransferProblemHandler(Controller controller) {
        Reject.ifNull(controller, "Controller is null");
        this.controller = controller;
    }

    // Interface methods ******************************************************

    public void downloadBroken(TransferManagerEvent event) {
        showProblem(event.getFile(), event.getTransferProblem(), event
            .getProblemInformation());
    }

    public void uploadBroken(TransferManagerEvent event) {
        showProblem(event.getFile(), event.getTransferProblem(), event
            .getProblemInformation());
    }

    public boolean fireInEventDispathThread() {
        return true;
    }

    // Internal methods *******************************************************

    /**
     * Display a warning message that the transfer failed.
     * 
     * @param file
     *            file information
     * @param transferProblem
     *            the transfer problem
     * @param problemInformation
     *            optional information about the problem
     */
    private void showProblem(final FileInfo file,
        final TransferProblem transferProblem, final String problemInformation)
    {
        if (!PreferencesEntry.WARN_ON_DOWNLOAD_TRANSFER_PROBLEMS
            .getValueBoolean(controller))
        {
            return;
        }
        if (!shouldShowProblem(transferProblem)) {
            return;
        }
        // Popup application
        controller.getUIController().getMainFrame().getUIComponent()
            .setVisible(true);
        controller.getUIController().getMainFrame().getUIComponent()
            .setExtendedState(Frame.NORMAL);

        // Show warning
        controller.getUIController().showWarningMessage(
            Translation.getTranslation("transfer.problem.title"),
            Translation.getTranslation(transferProblem.getTranslationId(), file
                .getFilenameOnly(), problemInformation));
    }

    private boolean shouldShowProblem(TransferProblem problem) {
        return TransferProblem.FILE_NOT_FOUND_EXCEPTION.equals(problem)
            || TransferProblem.IO_EXCEPTION.equals(problem)
            || TransferProblem.TEMP_FILE_DELETE.equals(problem)
            || TransferProblem.TEMP_FILE_OPEN.equals(problem)
            || TransferProblem.TEMP_FILE_WRITE.equals(problem)
            || TransferProblem.MD5_ERROR.equals(problem);
    }
}
