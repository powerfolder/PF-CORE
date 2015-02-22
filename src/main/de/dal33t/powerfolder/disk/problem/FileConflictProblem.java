package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class FileConflictProblem extends ResolvableProblem {

    private FileInfo fInfo;

    public FileConflictProblem(FileInfo fInfo) {
        Reject.ifNull(fInfo, "FileInfo");
        this.fInfo = fInfo;
    }

    public FileInfo getFileInfo() {
        return fInfo;
    }

    @Override
    public String getDescription() {
        return Translation.get("file_conflict_problem.description",
            fInfo.getRelativeName());
    }

    @Override
    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_FILE_CONFLICT;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (fInfo == null ? 0 : fInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FileConflictProblem other = (FileConflictProblem) obj;
        if (fInfo == null) {
            if (other.fInfo != null)
                return false;
        } else if (!fInfo.equals(other.fInfo))
            return false;
        return true;
    }

    @Override
    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                Folder folder = controller.getFolderRepository().getFolder(fInfo.getFolderInfo());
                PFWizard.openSingleFileRestoreWizard(controller, folder, fInfo, fInfo);
            }
        };
    }

    @Override
    public String getResolutionDescription() {
        return Translation.get("file_conflict_problem.soln_desc");
    }
}
