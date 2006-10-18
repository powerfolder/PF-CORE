package de.dal33t.powerfolder.ui.folder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;
import de.dal33t.powerfolder.util.ui.UIUtil;

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
        log().debug(
            "****************** " + fileNameProblemEvent.getFolder() + " "
                + fileNameProblemEvent.getScanResult().getProblemFiles());
        
        final FilenameProblemDialog dialog = new FilenameProblemDialog(
            getController(), fileNameProblemEvent.getScanResult());
        Runnable runner = new Runnable() {            
            public void run() {
                dialog.open();
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }
}