/* $Id: FolderComparator.java,v 1.6 2005/10/14 13:33:37 totmacherr Exp $
 */
package de.dal33t.powerfolder.util.compare;

import java.util.Comparator;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Loggable;

/**
 * Comparator which sorts folders
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class FolderComparator extends Loggable implements Comparator {

    public int compare(Object o1, Object o2) {
        int value = 0;
        String name1;
        String name2;

        if (o1 instanceof Folder) {
            Folder f1 = (Folder) o1;
            value -= (f1.isSecret() ? 2000 : 1000);
            name1 = f1.getName();
        } else if (o1 instanceof FolderInfo) {
            value -= 500;
            name1 = ((FolderInfo) o1).name;
        } else {
            throw new IllegalArgumentException(
                "Only Folder, FolderInfo or FolderDetails as argument allowed");
        }

        if (o2 instanceof Folder) {
            Folder f2 = (Folder) o2;
            value += (f2.isSecret() ? 2000 : 1000);
            name2 = f2.getName();
        } else if (o2 instanceof FolderInfo) {
            value += 500;
            name2 = ((FolderInfo) o2).name;
        } else {
            throw new IllegalArgumentException(
                "Only Folder, FolderInfo or FolderDetails as argument allowed");
        }

        // now add name
        int nameComp = name1.toLowerCase().compareTo(name2.toLowerCase());
        if (nameComp > 0) {
            nameComp = 1;
        } else if (nameComp < 0) {
            nameComp = -1;
        }

        value += nameComp;

        return value;
    }

}