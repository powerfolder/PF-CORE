package de.dal33t.powerfolder.ui.model;

/**
 * Interface for table models that can indicate which column the data is
 * sorted on, and in which order.
 */
public interface SortedTableModel {

    int getSortColumn();
    boolean isSortAscending();
    boolean sortBy(int columnIndex);
}
