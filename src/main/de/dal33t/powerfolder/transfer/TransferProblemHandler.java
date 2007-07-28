package de.dal33t.powerfolder.transfer;

import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;

/**
 * Utility class to display information about file transfer problems.
 */
public class TransferProblemHandler extends PFComponent {

    public TransferProblemHandler(Controller controller) {
        super(controller);
    }

    /**
     * Display a warning message that the transfer failed.
     *
     * @param file
     *         file information
     * @param transferProblem
     *         the transfer problem
     * @param problemInformation
     *         optional information about the problem
     */
    public void showProblem(final FileInfo file,
                            final TransferProblem transferProblem,
                            final String problemInformation) {

        // Create worker to invoke later.
        Runnable worker = new Runnable() {
            public void run() {

                // Popup application
                getController().getUIController().getMainFrame()
                        .getUIComponent().setVisible(true);
                getController().getUIController().getMainFrame()
                        .getUIComponent().setExtendedState(Frame.NORMAL);

                // Show warning
                getController().getUIController().showWarningMessage(
                        Translation.getTranslation(
                                "transfer.problem.title"),
                        Translation.getTranslation(
                                transferProblem.getTranslationId(),
                                file.getFilenameOnly(),
                                problemInformation));
            }
        };

        // Invoke later
        SwingUtilities.invokeLater(worker);
    }
}
