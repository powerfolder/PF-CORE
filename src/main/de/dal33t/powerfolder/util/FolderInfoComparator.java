/* $Id: FolderInfoComparator.java,v 1.8 2006/03/13 12:51:41 schaatser Exp $
 */
package de.dal33t.powerfolder.util;

import java.util.Comparator;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * Comparator for Folders
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.8 $
 */
public class FolderInfoComparator extends PFComponent implements Comparator {
    // All the available folder comparators
    public final static int BY_NAME = 0;
    public final static int BY_NUMBER_OF_FILES = 1;
    public final static int BY_SIZE = 2;
    public final static int BY_NUMBER_OF_MEMBERS = 3;
    public final static int BY_AVAILABILITY = 4;
    public final static int BY_MODIFIED_DATE = 5;
   
    private static final int BEFORE = -1;
    private static final int EQUAL = 0;
    private static final int AFTER = 1;
    
    private int sortBy;

    public FolderInfoComparator(Controller controller, int sortBy) {
        super(controller);
        this.sortBy = sortBy;
    }

    public int compare(Object o1, Object o2) {
        
        if (o1 instanceof FolderInfo && o2 instanceof FolderInfo) {
            FolderInfo folder1 = (FolderInfo) o1;
            FolderInfo folder2 = (FolderInfo) o2;

            switch (sortBy) {
                case BY_NAME :
                    return folder1.name.toLowerCase().compareTo(
                        folder2.name.toLowerCase());
                case BY_NUMBER_OF_FILES :
                    if (folder1.filesCount < folder2.filesCount)
                        return BEFORE;
                    if (folder1.filesCount > folder2.filesCount)
                        return AFTER;
                    return EQUAL;
                case BY_SIZE :
                    if (folder1.bytesTotal < folder2.bytesTotal)
                        return BEFORE;
                    if (folder1.bytesTotal > folder2.bytesTotal)
                        return AFTER;
                    return EQUAL;
                case BY_NUMBER_OF_MEMBERS : {
                    FolderDetails folderDetails1 = folder1
                        .getFolderDetails(getController());
                    FolderDetails folderDetails2 = folder2
                        .getFolderDetails(getController());
                    if (folderDetails1 == null || folderDetails2 == null) {
                        return EQUAL;
                    }
                    int memberCount1 = folder1
                        .getFolderDetails(getController()).memberCount();
                    int memberCount2 = folder2
                        .getFolderDetails(getController()).memberCount();
                    if (memberCount1 < memberCount2)
                        return BEFORE;
                    if (memberCount1 > memberCount2)
                        return AFTER;
                    return EQUAL;
                }
                case BY_MODIFIED_DATE : {
                    FolderDetails folderDetails1 = folder1
                        .getFolderDetails(getController());
                    FolderDetails folderDetails2 = folder2
                        .getFolderDetails(getController());
                    if (folderDetails1 == null || folderDetails2 == null
                        || folderDetails1.getLastModifiedDate() == null
                        || folderDetails2.getLastModifiedDate() == null)
                    {
                        return EQUAL;
                    }
                    return folderDetails1.getLastModifiedDate().compareTo(
                        folderDetails2.getLastModifiedDate());
                }
                case BY_AVAILABILITY : {
                    FolderDetails folderDetails1 = folder1
                        .getFolderDetails(getController());
                    FolderDetails folderDetails2 = folder2
                        .getFolderDetails(getController());
                    if (folderDetails1 == null || folderDetails2 == null) {
                        return EQUAL;
                    }
                    return folderDetails1.countOnlineMembers(getController())
                        - folderDetails2.countOnlineMembers(getController());
                }
            }
        }
        return EQUAL;
    }

    // General ****************************************************************

    public String toString() {
        String text = "FileInfo comparator, sorting by ";
        switch (sortBy) {
            case BY_NAME :
                text += "name";
                break;
            case BY_NUMBER_OF_FILES :
                text += "number of files";
                break;
            case BY_SIZE :
                text += "size";
                break;
            case BY_NUMBER_OF_MEMBERS :
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
