package de.dal33t.powerfolder.event;

/** receives FileNameProblems events fired by Folder */
public interface FileNameProblemHandler {
    public void fileNameProblemsDetected(FileNameProblemEvent fileNameProblemEvent);
}
