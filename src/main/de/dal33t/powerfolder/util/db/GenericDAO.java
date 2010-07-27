package de.dal33t.powerfolder.util.db;

/**
 * Generic Data Access Object for any persistent class.
 * 
 * @author <a href="max@dasmaximum.net">Maximilian Krickl</a>
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
    void persistOrMerge(T object);

    /**
     * Delete the object of type T.
     * 
     * @param object
     *            The object to delete
     */
    void delete(T object);
}
