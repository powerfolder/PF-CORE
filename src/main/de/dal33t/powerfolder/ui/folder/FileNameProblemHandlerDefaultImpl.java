package de.dal33t.powerfolder.ui.folder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;

public class FileNameProblemHandlerDefaultImpl extends PFUIComponent implements
    FileNameProblemHandler
{

    public FileNameProblemHandlerDefaultImpl(Controller controller) {
        super(controller);
    }
    
    public void fileNameProblemsDetected(
        FileNameProblemEvent fileNameProblemEvent)
    {
        log().debug(fileNameProblemEvent.getFolder() + " " + fileNameProblemEvent.getScanResult().getProblemFiles());
        
    }
}
