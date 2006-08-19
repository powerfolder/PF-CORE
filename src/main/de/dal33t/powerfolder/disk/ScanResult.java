package de.dal33t.powerfolder.disk;

import java.util.*;

import de.dal33t.powerfolder.light.FileInfo;

public class ScanResult {
    private List<FileInfo> newFiles;
    private List<FileInfo> changedFiles;
    private List<FileInfo> deletedFiles;
    private List<FileInfo> movedFiles;
    private int totalFilesCount = 0;
    /** files with potential problems in filenames (like 2 long or illegal chars) */
    private Map<FileInfo, List<String>> problemFiles;

    public List<FileInfo> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<FileInfo> changedFiles) {
        this.changedFiles = new ArrayList<FileInfo>(changedFiles);
    }

    public List<FileInfo> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(List<FileInfo> deletedFiles) {
        this.deletedFiles = new ArrayList<FileInfo>(deletedFiles);
    }

    public List<FileInfo> getMovedFiles() {
        return movedFiles;
    }

    public void setMovedFiles(List<FileInfo> movedFiles) {
        this.movedFiles = new ArrayList<FileInfo>(movedFiles);
    }

    public List<FileInfo> getNewFiles() {
        return newFiles;
    }

    public void setNewFiles(List<FileInfo> newFiles) {
        this.newFiles = new ArrayList<FileInfo>(newFiles);
    }

    public Map<FileInfo, List<String>> getProblemFiles() {
        return problemFiles;
    }

    public void setProblemFiles(Map<FileInfo, List<String>> problemFiles) {
        this.problemFiles = new HashMap<FileInfo, List<String>>(problemFiles);
    }

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    public void setTotalFilesCount(int totalFilesCount) {
        this.totalFilesCount = totalFilesCount;
    }

}
