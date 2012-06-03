package de.dal33t.powerfolder.ui.information.folder.files;

public class FilteredDirectory implements Comparable<FilteredDirectory> {

    private final String displayName;
    private final String relativeName;

    public FilteredDirectory(String displayName, String relativeName) {
        this.displayName = displayName;
        this.relativeName = relativeName;
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
