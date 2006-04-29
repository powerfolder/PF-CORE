/* $Id: ReverseComparator.java,v 1.2 2004/09/24 03:37:47 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

import java.util.Comparator;

/**
 * Comparator for reversing the original sort
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class ReverseComparator implements Comparator {
    private Comparator original;

    /**
     * Initalizes
     */
    public ReverseComparator(Comparator original) {
        if (original == null) {
            throw new NullPointerException("Original comparator is null");
        }
        this.original = original;
    }

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object o1, Object o2) {
    	// reverse order
        return -original.compare(o1, o2);
    }
}
