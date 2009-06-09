package de.dal33t.powerfolder.light;

import java.io.File;
import java.util.Date;

import de.dal33t.powerfolder.disk.Folder;

/**
 * A lightweight object representing a actual directory in the PowerFolder.
 * <p>
 * Related ticket: #378
 * 
 * @author sprajc
 */
public class DirectoryInfo extends FileInfo {
    private static final long serialVersionUID = 100L;

    private DirectoryInfo(String fileName, MemberInfo modifiedBy,
        Date lastModifiedDate, int version, boolean deleted,
        FolderInfo folderInfo)
    {
        super(fileName, 0, modifiedBy, lastModifiedDate, version, deleted,
            folderInfo);
    }

    /**
     * Initialize within a folder
     * 
     * @param folder
     * @param localDir
     * @param creator
     * @return the instance
     */
    public static DirectoryInfo newDirectory(Folder folder, File localDir,
        MemberInfo creator)
    {
        return new DirectoryInfo(buildFileName(folder, localDir), creator,
            new Date(localDir.lastModified()), 0, false, folder.getInfo());
    }
}
