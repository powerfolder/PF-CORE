package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.light.FolderInfo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class FileLinkFilterModel implements Serializable {


    private Collection<FolderInfo> foldersInfo;
    private String query;

    public void setFolders(final Collection<FolderInfo> folders) {
        this.foldersInfo = folders;
    }


    public void setQuery(String query) {
        this.query= query;
    }

    public Collection<FolderInfo> getFoldersInfo() {
        return foldersInfo;
    }

    public void setFoldersInfo(Collection<FolderInfo> foldersInfo) {
        this.foldersInfo = foldersInfo;
    }

    public String getQuery() {
        return query;
    }
}
