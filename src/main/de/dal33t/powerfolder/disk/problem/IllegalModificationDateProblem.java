package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;

public class IllegalModificationDateProblem extends Problem {
    private final FileInfo fileInfo;

    public IllegalModificationDateProblem(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    @Override
    public String getDescription() {
        return Translation.get("file_problem.illegal_modification_date",
                fileInfo.getFilenameOnly());
    }

    @Override
    public String getWikiLinkKey() {
        return null;
    }
}
