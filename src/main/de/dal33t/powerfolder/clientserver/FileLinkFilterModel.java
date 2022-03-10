package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.light.FolderInfo;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

public class FileLinkFilterModel implements Serializable {
    private static final long serialVersionUID = 100L;

    private Collection<FolderInfo> foldersInfo;
    private String query;

    public void setFolders(final Collection<FolderInfo> folders) {
        this.foldersInfo = folders;
    }
    public Collection<FolderInfo> getFolders() {
        return Collections.unmodifiableCollection(foldersInfo);
    }

    public void setQuery(String query) {
        this.query= query;
    }
    public String getQuery() {
        return query;
    }
}
