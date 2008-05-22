package de.dal33t.powerfolder;

import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.FolderInfo;

import java.util.Date;

/**
 * Common interface for FileInfo and Directory,
 * for doing comparisons in {@link de.dal33t.powerfolder.util.compare.DiskItemComparator}.
 */
public interface DiskItem {
    String getExtension();
    String getName();
    String getLowerCaseName();
    long getSize();
    MemberInfo getModifiedBy();
    Date getModifiedDate();
    FolderInfo getFolderInfo();
}
