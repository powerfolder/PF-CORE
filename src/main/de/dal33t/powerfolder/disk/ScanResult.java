package de.dal33t.powerfolder.disk;

import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.light.FileInfo;

public class ScanResult {
    private List<FileInfo> newFiles;
    private List<FileInfo> changedFiles;
    private List<FileInfo> deletedFiles;
    private List<FileInfo> movedFiles;
    /** files with potential problems in filenames (like 2 long or illegal chars) */
    private Map<FileInfo, FileNameProblem> problemFiles;

    public List<FileInfo> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<FileInfo> changedFiles) {
        this.changedFiles = changedFiles;
    }

    public List<FileInfo> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(List<FileInfo> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }

    public List<FileInfo> getMovedFiles() {
        return movedFiles;
    }

    public void setMovedFiles(List<FileInfo> movedFiles) {
        this.movedFiles = movedFiles;
    }

    public List<FileInfo> getNewFiles() {
        return newFiles;
    }

    public void setNewFiles(List<FileInfo> newFiles) {
        this.newFiles = newFiles;
    }

    public Map<FileInfo, FileNameProblem> getProblemFiles() {
        return problemFiles;
    }

    public void setProblemFiles(Map<FileInfo, FileNameProblem> problemFiles) {
        this.problemFiles = problemFiles;
    }

}
