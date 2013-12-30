package de.dal33t.powerfolder.disk.problem;

import java.nio.file.Path;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class FolderReadOnlyProblem extends ResolvableProblem {
    private Path path;
    private boolean revertedOnly;

    public FolderReadOnlyProblem(Path path) {
        this(path, false);
    }

    public FolderReadOnlyProblem(Path path, boolean revertedOnly) {
        Reject.ifNull(path, "Path");
        this.path = path;
        this.revertedOnly = revertedOnly;
    }

    @Override
    public String getDescription() {
        if (revertedOnly) {
            return Translation.getTranslation(
                "folder_problem.read_only_folder_reverted", path.getFileName()
                    .toString());
        } else {
            return Translation.getTranslation(
                "folder_problem.read_only_folder", path.getFileName()
                    .toString());
        }
    }

    @Override
    public String getWikiLinkKey() {
        return WikiLinks.SECURITY_PERMISSION;
    }

    @Override
    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            @Override
            public void run() {
                PathUtils.openFile(path.getParent());
            }
        };
    }

    @Override
    public String getResolutionDescription() {
        return null;
    }
}
