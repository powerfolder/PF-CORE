package de.dal33t.powerfolder.ui.information.folder.files;

import java.util.Collection;
import java.util.TreeSet;

public class FilteredDirectory implements Comparable<FilteredDirectory> {

    private final String displayName;
    private final String relativeName;
    private final Collection<FilteredDirectory> list = new TreeSet<FilteredDirectory>();
    private boolean files;
    private boolean newFiles;
    private boolean deleted;

    public FilteredDirectory(String displayName, String relativeName,
        boolean deleted)
    {
        this.displayName = displayName;
        this.relativeName = relativeName;
        this.deleted = deleted;
    }

    public Collection<FilteredDirectory> getList() {
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

    public int compareTo(FilteredDirectory o) {
        return getDisplayName().compareToIgnoreCase(o.getDisplayName());
    }
}
