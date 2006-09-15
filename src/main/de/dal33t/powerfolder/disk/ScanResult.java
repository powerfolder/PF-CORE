package de.dal33t.powerfolder.disk;

import java.util.*;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * This class lists all the differences between the database of files in a
 * folder (knownFiles) and the ones available on disk.
 */
public class ScanResult {
	public enum ResultState {
		SCANNED, USER_ABORT, HARDWARE_FAILURE
	}

	private List<FileInfo> newFiles;

	private List<FileInfo> changedFiles;

	private List<FileInfo> deletedFiles;

	private List<FileInfo> movedFiles;

	private List<FileInfo> restoredFiles;

	/** files with potential problems in filenames (like 2 long or illegal chars) */
	private Map<FileInfo, List<String>> problemFiles;

	private ResultState resultState;

	private int totalFilesCount = 0;

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

	public List<FileInfo> getRestoredFiles() {
		return restoredFiles;
	}

	public void setRestoredFiles(List<FileInfo> restoredFiles) {
		this.restoredFiles = restoredFiles;
	}

	public ResultState getResultState() {
		return resultState;
	}

	public void setResultState(ResultState resultState) {
		this.resultState = resultState;
	}

	public String toString() {
		return resultState + " Newfiles: " + newFiles.size()
				+ " changed files: " + changedFiles.size() + " deleted files: "
				+ deletedFiles.size() + " restoredFiles: "
				+ restoredFiles.size() + " movedFiles: " + movedFiles.size()
				+ " proplemFiles: " + problemFiles.size();
	}
}
