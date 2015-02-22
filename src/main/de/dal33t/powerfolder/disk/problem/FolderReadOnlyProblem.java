package de.dal33t.powerfolder.disk.problem;

import java.nio.file.Path;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class FolderReadOnlyProblem extends ResolvableProblem {
    private final Path path;
    private final boolean revertedOnly;
    private final Folder folder;

    public FolderReadOnlyProblem(Folder folder, Path path) {
        this(folder, path, false);
    }

    public FolderReadOnlyProblem(Folder folder, Path path, boolean revertedOnly) {
        Reject.ifNull(path, "Path");
        this.path = path;
        this.revertedOnly = revertedOnly;
        this.folder = folder;
    }

    @Override
    public String getDescription() {
        if (revertedOnly) {
            return Translation.get(
                "folder_problem.read_only_folder_reverted", path.getFileName()
                    .toString());
        } else {
            return Translation.get(
                "folder_problem.read_only_folder", path.getFileName()
                    .toString());
        }
    }

    @Override
    public String getWikiLinkKey() {
        return null;
    }

    @Override
    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            @Override
            public void run() {
                PathUtils.openFileIfExists(path.getParent());

                folder.removeProblem(FolderReadOnlyProblem.this);
            }
        };
    }

    @Override
    public String getResolutionDescription() {
        return Translation.get("folder_problem.read_only_folder_reverted.resolution_description");
    }
}
