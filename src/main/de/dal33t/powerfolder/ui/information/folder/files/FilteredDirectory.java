package de.dal33t.powerfolder.ui.information.folder.files;

import java.util.List;
import java.util.ArrayList;

public class FilteredDirectory {

    private final String displayName;
    private final String relativeName;
    private final List<FilteredDirectory> list = new ArrayList<FilteredDirectory>();
    private boolean files;
    private boolean newFiles;
    private boolean deleted;
    private boolean deletedFiles;

    public FilteredDirectory(String displayName, String relativeName, boolean deleted) {
        this.displayName = displayName;
        this.relativeName = relativeName;
        this.deleted = deleted;
    }

    public List<FilteredDirectory> getList() {
        return list;
    }

    public void setFiles(boolean files) {
        this.files = files;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setNewFiles(boolean newFiles) {
        this.newFiles = newFiles;
    }

    public boolean hasFilesDeep() {
        if (files) {
            return true;
        }
        for (FilteredDirectory directory : list) {
            if (directory.hasFilesDeep()) {
                return true;
            }
        }
        return false;
    }

    public void setDeletedFiles(boolean deletedFiles) {
        this.deletedFiles = deletedFiles;
    }

    public boolean hasDeletedFilesDeep() {
        if (deletedFiles) {
            return true;
        }
        for (FilteredDirectory directory : list) {
            if (directory.hasDeletedFilesDeep()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNewFilesDeep() {
        if (newFiles) {
            return true;
        }
        for (FilteredDirectory directory : list) {
            if (directory.hasNewFilesDeep()) {
                return true;
            }
        }
        return false;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRelativeName() {
        return relativeName;
    }
}
