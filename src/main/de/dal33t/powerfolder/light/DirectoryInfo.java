package de.dal33t.powerfolder.light;

import java.io.File;
import java.util.Date;

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;

/**
 * A lightweight object representing a actual directory in the PowerFolder.
 * <p>
 * Related ticket: #378
 * 
 * @author sprajc
 */
public class DirectoryInfo extends FileInfo {
    private static final long serialVersionUID = 100L;

    DirectoryInfo(String fileName, MemberInfo modifiedBy,
        Date lastModifiedDate, int version, boolean deleted,
        FolderInfo folderInfo)
    {
        super(fileName, 0, modifiedBy, lastModifiedDate, version, deleted,
            folderInfo);
    }

    DirectoryInfo(String fileName, long size, MemberInfo modifiedBy,
        Date lastModifiedDate, int version, boolean deleted,
        FolderInfo folderInfo)
    {
        super(fileName, size, modifiedBy, lastModifiedDate, version, deleted,
            folderInfo);
    }

    DirectoryInfo(FolderInfo folder, String name) {
        super(folder, name);
    }

    public static DirectoryInfo getTemplate(FolderInfo folder, String name) {
        return new DirectoryInfo(folder, name);
    }

    public boolean isDiretory() {
        return true;
    }

    public boolean isFile() {
        return false;
    }

    @Override
    public String toString() {
        return "[" + folderInfo.name + "]:" + (deleted ? "(del) /" : "/")
            + fileName + " (D)";
    }

    /**
     * appends to buffer
     * 
     * @param str
     *            the stringbuilder to add the detail info to.
     */
    private final void toDetailString(StringBuilder str) {
        str.append(toString());
        str.append(", size: ");
        str.append(size);
        str.append(" bytes, version: ");
        str.append(getVersion());
        str.append(", modified: ");
        str.append(lastModifiedDate);
        str.append(" (");
        if (lastModifiedDate != null) {
            str.append(lastModifiedDate.getTime());
        } else {
            str.append("-n/a-");
        }
        str.append(") by '");
        if (modifiedBy == null) {
            str.append("-n/a-");
        } else {
            str.append(modifiedBy.nick);
        }
        str.append("'");
    }

    public String toDetailString() {
        StringBuilder str = new StringBuilder();
        toDetailString(str);
        return str.toString();
    }

    /**
     * @param diskFile
     *            the file on disk.
     * @return true if the fileinfo is in sync with the file on disk.
     */
    public boolean inSyncWithDisk(File diskFile) {
        Reject.ifNull(diskFile, "Diskfile is null");
        return super.inSyncWithDisk0(diskFile, true);
    }

    @Override
    public int hashCode() {
        int hash = fileName.hashCode();
        hash += folderInfo.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof DirectoryInfo) {
            DirectoryInfo otherInfo = (DirectoryInfo) other;
            return Util.equals(this.fileName, otherInfo.fileName)
                && Util.equals(this.folderInfo, otherInfo.folderInfo);
        }

        return false;
    }

}
