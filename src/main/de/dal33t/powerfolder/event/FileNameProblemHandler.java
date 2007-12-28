package de.dal33t.powerfolder.event;

/** receives FileNameProblems events fired by Folder */
public interface FileNameProblemHandler {
    /**
     * Called when a problem with names of files get detected.
     * 
     * @param fileNameProblemEvent
     */
    public void fileNameProblemsDetected(
        FileNameProblemEvent fileNameProblemEvent);
}
