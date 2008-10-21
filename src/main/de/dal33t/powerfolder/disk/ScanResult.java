/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.light.FileInfo;

import java.util.*;

/**
 * This class lists all the differences between the database of files in a
 * folder (knownFiles) and the ones available on disk.
 */
public class ScanResult {
    public enum ResultState {
        SCANNED, USER_ABORT, HARDWARE_FAILURE, BUSY
    }

    private List<FileInfo> newFiles;

    private List<FileInfo> changedFiles;

    private Collection<FileInfo> deletedFiles;
    /** from, to */
    private Map<FileInfo, FileInfo> movedFiles;

    private List<FileInfo> restoredFiles;

    /** files with potential problems in filenames (like 2 long or illegal chars) */
    private Map<FileInfo, List<FilenameProblem>> problemFiles;

    private ResultState resultState;

    private int totalFilesCount = 0;

    public boolean isChangeDetected() {
        return changedFiles.size() > 0 || deletedFiles.size() > 0
            || newFiles.size() > 0 || movedFiles.size() > 0
            || restoredFiles.size() > 0;
    }

    public List<FileInfo> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<FileInfo> changedFiles) {
        this.changedFiles = new ArrayList<FileInfo>(changedFiles);
    }

    public Collection<FileInfo> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(Collection<FileInfo> deletedFiles) {
        this.deletedFiles = new ArrayList<FileInfo>(deletedFiles);
    }

    /** from, to */
    public Map<FileInfo, FileInfo> getMovedFiles() {
        return movedFiles;
    }

    public void setMovedFiles(Map<FileInfo, FileInfo> movedFiles) {
        this.movedFiles = new HashMap<FileInfo, FileInfo>(movedFiles);
    }

    public List<FileInfo> getNewFiles() {
        return newFiles;
    }

    public void setNewFiles(List<FileInfo> newFiles) {
        this.newFiles = new ArrayList<FileInfo>(newFiles);
    }

    public Map<FileInfo, List<FilenameProblem>> getProblemFiles() {
        return problemFiles;
    }

    public void setProblemFiles(
        Map<FileInfo, List<FilenameProblem>> problemFiles)
    {
        this.problemFiles = new HashMap<FileInfo, List<FilenameProblem>>(
            problemFiles);
    }

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    public void setTotalFilesCount(int totalFilesCount) {
        this.totalFilesCount = totalFilesCount;
    }

    public List<FileInfo> getRestoredFiles() {
        return restoredFiles;
    }

    public void setRestoredFiles(List<FileInfo> restoredFiles) {
        this.restoredFiles = new ArrayList<FileInfo>(restoredFiles);
    }

    public ResultState getResultState() {
        return resultState;
    }

    public void setResultState(ResultState resultState) {
        this.resultState = resultState;
    }

    public String toString() {
        return resultState + ", Total files: " + totalFilesCount
            + ", Newfiles: " + newFiles.size() + ", changed files: "
            + changedFiles.size() + ", deleted files: " + deletedFiles.size()
            + " restoredFiles: " + restoredFiles.size() + ", movedFiles: "
            + movedFiles.size() + ", problemFiles: " + problemFiles.size();
    }
}
