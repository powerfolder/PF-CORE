package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class FileConflictProblem extends Problem {

    private FileInfo fInfo;

    public FileConflictProblem(FileInfo fInfo) {
        super();
        Reject.ifNull(fInfo, "FileInfo");
        this.fInfo = fInfo;
    }

    public FileInfo getFileInfo() {
        return fInfo;
    }

    @Override
    public String getDescription() {
        return Translation.getTranslation("file_conflict_problem.description",
            fInfo.getRelativeName());
    }

    @Override
    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_FILE_CONFLICT;
    }

}
