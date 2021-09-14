package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IllegalModificationDateProblem extends ResolvableProblem {
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

    @Override
    public Folder getFolder(Controller controller) {
        return fileInfo.getFolder(controller.getFolderRepository());
    }

    @Override
    public Runnable resolution(Controller controller) {
        return () -> {
            Path file = fileInfo.getDiskFile(controller.getFolderRepository());
            try {
                Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
                getFolder(controller).scanChangedFile(fileInfo);
            } catch (IOException e) {
                Logger.getLogger(IllegalModificationDateProblem.class.getName()).log(Level.WARNING, "Failed to set modification date of " + file + " to now.", e);
            }
        };
    }

    @Override
    public String getResolutionDescription() {
        return Translation.get("file_problem.illegal_modification_date.solution");
    }
}
