package de.dal33t.powerfolder.disk.problem;

import java.nio.file.Path;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class FolderReadOnlyProblem extends ResolvableProblem {
    private Path path;

    public FolderReadOnlyProblem(Path path) {
        Reject.ifNull(path, "Path");
        this.path = path;
    }

    @Override
    public String getDescription() {
        return Translation.getTranslation("folder_problem.read_only_folder");
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
