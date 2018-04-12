package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Translation;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class IllegalCharsFilenameProblem extends ResolvableProblem {
    private final String description;
    private final FileInfo fileInfo;
    private final String[] illegalChars;

    IllegalCharsFilenameProblem(FileInfo fileInfo, String[] illegalChars) {
        this.fileInfo = fileInfo;
        description = Translation.get("filename_problem.not_recommended_chars",
            fileInfo.getFilenameOnly());
        this.illegalChars = illegalChars;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public String getDescription() {
        return description;
    }

    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_ILLEGAL_CHARS;
    }

    public Folder getFolder(final Controller controller) {
        return fileInfo.getFolder(controller.getFolderRepository());
    }

    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                String newFilename = FilenameProblemHelper.removeChars(
                    fileInfo.getFilenameOnly(), illegalChars);
                FilenameProblemHelper.resolve(controller, fileInfo, newFilename,
                    IllegalCharsFilenameProblem.this);
            }
        };
    }

    public String getResolutionDescription() {
        return Translation.get("filename_problem.not_recommended_chars.soln_desc");
    }
}
