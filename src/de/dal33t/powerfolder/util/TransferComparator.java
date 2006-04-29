package de.dal33t.powerfolder.util;

import java.util.Comparator;

import de.dal33t.powerfolder.transfer.Transfer;

/**
 * Comparator for transfers in general
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.2 $
 */
public class TransferComparator implements Comparator {
    // All the available transfer comparators
    public static final TransferComparator BY_DATE = new TransferComparator(3);

    // sort by
    // 3 : modified date
    private int sortBy;

    private TransferComparator(int sortBy) {
        this.sortBy = sortBy;
    }

    public int compare(Object o1, Object o2) {
        if (o1 instanceof Transfer && o2 instanceof Transfer) {
            Transfer t1 = (Transfer) o1;
            Transfer t2 = (Transfer) o2;

            switch (sortBy) {
                case 3 :
                    return t2.getFile().getModifiedDate().compareTo(
                        t1.getFile().getModifiedDate());
            }
        }
        return 0;
    }
}