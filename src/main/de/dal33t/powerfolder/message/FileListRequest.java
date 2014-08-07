package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * A message to re-request the file list from another member.
 *
 * @author Sprajc
 */
public class FileListRequest extends FolderRelatedMessage {
    private static final long serialVersionUID = 100L;

    public FileListRequest(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        folder = foInfo;
    }

    @Override
    public String toString() {
        return "FileListRequest [folder=" + folder.getLocalizedName() + "/" + folder.id + "]";
    }
}
