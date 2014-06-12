/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: NodeManager.java 12576 2010-06-14 14:28:23Z tot $
 */
package de.dal33t.powerfolder.util.intern;

import java.util.WeakHashMap;

/**
 * A general class that is able to internalize a given object to save memory
 * resources. The internalization is usually realized via {@link WeakHashMap}
 *
 * @author sprajc
 * @param <T>
 */
public interface Internalizer<T> {
    /**
     * @param item
     *            the item to internalize
     * @return the interned instance of the item
     */
    T intern(T item);
    
    T sudoIntern(T item);
}
