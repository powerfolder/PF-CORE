/* $Id: FileInfoComparator.java,v 1.15 2006/03/13 12:51:17 schaatser Exp $
 */
package de.dal33t.powerfolder.util.compare;

import java.util.Comparator;

import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.FileInfoHolder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Loggable;

/**
 * Comparator for FileInfo
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.15 $
 */
public class FileInfoComparator extends Loggable implements Comparator<FileInfo> {

    // All the available file comparators
    public static final int BY_FILETYPE = 0;
    public static final int BY_NAME = 1;
    public static final int BY_SIZE = 2;
    public static final int BY_MEMBER = 3;
    public static final int BY_MODIFIED_DATE = 4;
    public static final int BY_AVAILABILITY = 5;
    public static final int BY_FOLDER = 6;

    private static final int BEFORE = -1;
    private static final int EQUAL = 0;
    private static final int AFTER = 1;
    
    private Directory directory;
    private int sortBy;
    private static FileInfoComparator[] comparators;

    static {
        comparators = new FileInfoComparator[7];
        comparators[BY_FILETYPE] = new FileInfoComparator(
            FileInfoComparator.BY_FILETYPE);
        comparators[BY_NAME] = new FileInfoComparator(
            FileInfoComparator.BY_NAME);
        comparators[BY_SIZE] = new FileInfoComparator(
            FileInfoComparator.BY_SIZE);
        comparators[BY_MEMBER] = new FileInfoComparator(
            FileInfoComparator.BY_MEMBER);
        comparators[BY_MODIFIED_DATE] = new FileInfoComparator(
            FileInfoComparator.BY_MODIFIED_DATE);
        comparators[BY_AVAILABILITY] = new FileInfoComparator(
            FileInfoComparator.BY_AVAILABILITY);
        comparators[BY_FOLDER] = new FileInfoComparator(
            FileInfoComparator.BY_FOLDER);
    }
    
    public FileInfoComparator(int sortBy) {
        this.sortBy = sortBy;     
    }
    
    public FileInfoComparator(int sortBy, Directory directory) {
        this.sortBy = sortBy;
        this.directory = directory;
    }

    public static FileInfoComparator getComparator(int sortByArg) {
        return comparators[sortByArg];
    }

    public int compare(FileInfo o1, FileInfo o2) {
        
            switch (sortBy) {
                case BY_FILETYPE : {
                    String ext1 = o1.getExtension();
                    String ext2 = o2.getExtension();
                    if (ext1 == null || ext2 == null) {
                        return EQUAL;
                    }
                    return ext1.compareTo(ext2);
                }
                case BY_NAME :
                    return o1.getLowerCaseName().compareTo(
                        o2.getLowerCaseName());
                case BY_SIZE :
                    if (o1.getSize() < o2.getSize()) {
                        return BEFORE;
                    }
                    if (o1.getSize() > o2.getSize()) {
                        return AFTER;
                    }
                    return EQUAL;
                case BY_MEMBER :
                    return o1.getModifiedBy().nick.toLowerCase().compareTo(
                        o2.getModifiedBy().nick.toLowerCase());
                case BY_MODIFIED_DATE :                    
                   return o2.getModifiedDate().compareTo(
                        o1.getModifiedDate());
                case BY_AVAILABILITY : {
                    if (directory == null) {
                        throw new IllegalStateException(
                            "need a directoy to compare by BY_AVAILABILITY");
                    }
                    FileInfoHolder holder1 = directory.getFileInfoHolder(o1);
                    FileInfoHolder holder2 = directory.getFileInfoHolder(o2);
                    if (holder1 != null && holder2 != null) {
                        int av1 = holder1.getAvailability();
                        int av2 = holder2.getAvailability();
                        if (av1 == av2) {
                            return EQUAL;
                        }
                        if (av1 < av2) {
                            return BEFORE;
                        }
                        return AFTER;
                    }
                    return EQUAL;
                }
                case BY_FOLDER :
                   return o1.getFolderInfo().name.compareToIgnoreCase(
                        o2.getFolderInfo().name);
            }
        return 0;
    }

    // General ****************************************************************

    public String toString() {
        String text = "FileInfo comparator, sorting by ";
        switch (sortBy) {
            case BY_FILETYPE :
                text += "file type";
                break;
            case BY_NAME :
                text += "name";
                break;
            case BY_SIZE :
                text += "size";
                break;
            case BY_MEMBER :
                text += "member";
                break;
            case BY_MODIFIED_DATE :
                text += "modified date";
                break;
            case BY_AVAILABILITY :
                text += "availability";
        }
        return text;
    }
}
