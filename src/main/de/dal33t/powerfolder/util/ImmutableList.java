/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
 * $Id: $
 */
package de.dal33t.powerfolder.util;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * List implementation outside of the collections framework for immutable lists.
 * Benefits of using this class:
 * <ul>
 * <li>Inherently Thread-Safe
 * <li>Copying a list is never required
 * <li>Constant time appending of elements with constant space requirements for
 * the resulting list
 * </ul>
 *
 * @author Dennis Waldherr
 * @param <E>
 *            Type of the list
 */
public class ImmutableList<E> implements Iterable<E>, Serializable {
    private static final long serialVersionUID = -654244056844800570L;

    private final E head;
    private final ImmutableList<E> tail;

    public ImmutableList(E initial) {
        head = initial;
        tail = null;
    }

    private ImmutableList(E element, ImmutableList<E> tail) {
        assert tail != null;
        head = element;
        this.tail = tail;
    }

    public E getHead() {
        return head;
    }

    public ImmutableList<E> getTail() {
        return tail;
    }

    @Override
    public int hashCode() {
        ImmutableList<E> i = this;
        int hc = 17;
        while (i != null) {
            hc = hc * 37 + i.head.hashCode();
            i = i.tail;
        }
        return hc;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        ImmutableList<E> i = this, j = (ImmutableList<E>) obj;
        while (i != null && j != null) {
            if (i.head != j.head) {
                return false;
            }
            i = i.tail;
            j = j.tail;
        }
        return true;
    }

    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private ImmutableList<E> pos = ImmutableList.this;

            public boolean hasNext() {
                return pos.tail != null;
            }

            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                pos = pos.tail;
                return pos.head;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public ImmutableList<E> add(E e) {
        return new ImmutableList<E>(e, this);
    }

}
