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
        return "[" + getFolderInfo().name + "]:"
            + (isDeleted() ? "(del) /" : "/") + getRelativeName() + " (D)";
    }

    /**
     * appends to buffer
     * 
     * @param str
     *            the stringbuilder to add the detail info to.
     */
    private final void toDetailString(StringBuilder str) {
        str.append(toString());
        str.append(", version: ");
        str.append(getVersion());
        str.append(", modified: ");
        str.append(getModifiedDate());
        str.append(" (");
        if (getModifiedDate() != null) {
            str.append(getModifiedDate().getTime());
        } else {
            str.append("-n/a-");
        }
        str.append(") by '");
        if (getModifiedBy() == null) {
            str.append("-n/a-");
        } else {
            str.append(getModifiedBy().nick);
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
    
    // hashCode() is used from FileInfo

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof DirectoryInfo) {
            DirectoryInfo otherInfo = (DirectoryInfo) other;
            boolean caseMatch = IGNORE_CASE ? Util.equalsIgnoreCase(this
                .getRelativeName(), otherInfo.getRelativeName()) : Util.equals(
                this.getRelativeName(), otherInfo.getRelativeName());
            return caseMatch
                && Util.equals(this.getFolderInfo(), otherInfo.getFolderInfo());
        }

        return false;
    }

}
