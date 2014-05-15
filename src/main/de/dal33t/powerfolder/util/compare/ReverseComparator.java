/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder.util.compare;

import java.util.Comparator;

/**
 * Comparator for reversing the original sort
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 * @param <T>
 *            type of the comparator.
 */
public class ReverseComparator<T> implements Comparator<T> {
    private Comparator<T> original;

    /**
     * @param original
     */
    public ReverseComparator(Comparator<T> original) {
        if (original == null) {
            throw new NullPointerException("Original comparator is null");
        }
        this.original = original;
    }

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(T o1, T o2) {
        // reverse order
        return -original.compare(o1, o2);
    }
}
