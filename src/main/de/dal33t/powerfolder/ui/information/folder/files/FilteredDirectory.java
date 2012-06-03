package de.dal33t.powerfolder.ui.information.folder.files;

import java.util.Collection;
import java.util.TreeSet;

public class FilteredDirectory implements Comparable<FilteredDirectory> {

    private final String displayName;
    private final String relativeName;
    private final Collection<FilteredDirectory> list = new TreeSet<FilteredDirectory>();
    private boolean files;
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
