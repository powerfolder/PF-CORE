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
package de.dal33t.powerfolder.util.db;

/**
 * Generic Data Access Object for any persistent class.
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 * @param <T>
 *            Type of the mapped class
 */
public interface GenericDAO<T> {
    /**
     * Find an object of type T by using its id.
     *
     * @param id
     *            The id of the object to load
     * @return The object associated with the id
     */
    T findByID(String id);

    /**
     * Store the object of type T to the persistence layer.
     *
     * @param object
     *            The object to store
     */
    void store(T object);

    /**
     * Delete the object of type T.
     *
     * @param object
     *            The object to delete
     */
    void delete(T object);
}
