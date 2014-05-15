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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.disk.problem.Problem;

/**
 * This class lists all the differences between the database of files in a
 * folder (knownFiles) and the ones available on disk.
 */
public class ScanResult {
    public enum ResultState {
        SCANNED, USER_ABORT, HARDWARE_FAILURE, BUSY
    }

    private ResultState resultState;
    /** Files not in the database (remaining) and are NEW are collected here. */
    Collection<FileInfo> newFiles;
    /**
     * Files that have their size or modification date changed are collected
     * here.
     */
    Collection<FileInfo> changedFiles;
    Collection<FileInfo> deletedFiles;
    /** from, to */
    Map<FileInfo, FileInfo> movedFiles;
    /**
     * Files that where marked deleted in the database but are available on disk
     * are collected here.
     */
    Collection<FileInfo> restoredFiles;

    /** files with potential problems in filenames (like 2 long or illegal chars) */
    private Map<FileInfo, List<Problem>> problemFiles;

    private volatile int totalFilesCount;

    public ScanResult(ResultState result) {
        this(false);
        Reject.ifNull(result, "Result state is null");
        resultState = result;
    }

    public ScanResult(boolean initFields) {
        resultState = ResultState.SCANNED;
        if (initFields) {
            newFiles = Collections.synchronizedList(new ArrayList<FileInfo>());
            changedFiles = Collections
                .synchronizedList(new ArrayList<FileInfo>());
            deletedFiles = Collections
                .synchronizedList(new ArrayList<FileInfo>());
            movedFiles = Collections
                .synchronizedMap(new HashMap<FileInfo, FileInfo>());
            restoredFiles = Collections
                .synchronizedList(new ArrayList<FileInfo>());
            problemFiles = Collections
                .synchronizedMap(new HashMap<FileInfo, List<Problem>>());
        }
    }

    public boolean isChangeDetected() {
        return !changedFiles.isEmpty() || !deletedFiles.isEmpty()
            || !newFiles.isEmpty() || !movedFiles.isEmpty()
            || !restoredFiles.isEmpty();
    }

    public Collection<FileInfo> getChangedFiles() {
        return Collections.unmodifiableCollection(changedFiles);
    }

    // public void setChangedFiles(List<FileInfo> changedFiles) {
    // this.changedFiles = new ArrayList<FileInfo>(changedFiles);
    // }

    public Collection<FileInfo> getDeletedFiles() {
        return Collections.unmodifiableCollection(deletedFiles);
    }

    // public void setDeletedFiles(Collection<FileInfo> deletedFiles) {
    // this.deletedFiles = new ArrayList<FileInfo>(deletedFiles);
    // }

    /** from, to */
    public Map<FileInfo, FileInfo> getMovedFiles() {
        return Collections.unmodifiableMap(movedFiles);
    }

    // public void setMovedFiles(Map<FileInfo, FileInfo> movedFiles) {
    // this.movedFiles = new HashMap<FileInfo, FileInfo>(movedFiles);
    // }

    public Collection<FileInfo> getNewFiles() {
        return Collections.unmodifiableCollection(newFiles);
    }

    // public void setNewFiles(List<FileInfo> newFiles) {
    // this.newFiles = new ArrayList<FileInfo>(newFiles);
    // }

    public Map<FileInfo, List<Problem>> getProblemFiles() {
        return Collections.unmodifiableMap(problemFiles);
    }

    /**
     * @param problemFiles
     * @deprecated for tests only
     */
    public void setProblemFiles(
        Map<FileInfo, List<Problem>> problemFiles)
    {
        this.problemFiles = problemFiles;
    }

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    // public void setTotalFilesCount(int totalFilesCount) {
    // this.totalFilesCount = totalFilesCount;
    // }

    public Collection<FileInfo> getRestoredFiles() {
        return Collections.unmodifiableCollection(restoredFiles);
    }

    // public void setRestoredFiles(List<FileInfo> restoredFiles) {
    // this.restoredFiles = new ArrayList<FileInfo>(restoredFiles);
    // }

    public void putFileProblems(FileInfo fileInfo, List<Problem> problemList) {
        problemFiles.put(fileInfo, problemList);
    }

    public ResultState getResultState() {
        return resultState;
    }

    public void setResultState(ResultState resultState) {
        this.resultState = resultState;
    }

    public void incrementTotalFilesCount() {
            totalFilesCount++;
    }

    public String toString() {
        return resultState + ", Total files: " + totalFilesCount
            + ", Newfiles: " + newFiles.size() + ", changed files: "
            + changedFiles.size() + ", deleted files: " + deletedFiles.size()
            + " restoredFiles: " + restoredFiles.size() + ", movedFiles: "
            + movedFiles.size() + ", problemFiles: " + problemFiles.size();
    }
}
