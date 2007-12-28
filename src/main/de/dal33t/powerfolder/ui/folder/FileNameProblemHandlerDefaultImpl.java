package de.dal33t.powerfolder.ui.folder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;

/**
 * shows a dialog to solve problems with filenames. may edit the scan result
 * before commit.
 */
public class FileNameProblemHandlerDefaultImpl extends PFUIComponent implements
    FileNameProblemHandler
{
    public FileNameProblemHandlerDefaultImpl(Controller controller) {
        super(controller);
    }

    public void fileNameProblemsDetected(
        FileNameProblemEvent fileNameProblemEvent)
    {
        if (logDebug) {
            log().debug(
                fileNameProblemEvent.getFolder() + " "
                    + fileNameProblemEvent.getProblems().size()
                    + " problematic files");
        }

        if (PreferencesEntry.FILE_NAME_CHECK.getValueBoolean(getController())) {
            FilenameProblemDialog dialog = new FilenameProblemDialog(
                getController(), fileNameProblemEvent);
            // Modal dialog causes wait here
            dialog.open();
            if (dialog.getOption() == FilenameProblemDialog.OK
                && !dialog.askAgain())
            {
                PreferencesEntry.FILE_NAME_CHECK.setValue(getController(),
                    false);
            }
        }
    }
}