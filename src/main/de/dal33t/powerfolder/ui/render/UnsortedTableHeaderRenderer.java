package de.dal33t.powerfolder.ui.render;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Enumeration;

/**
 * Table header renderer for tables with models that are not sorted.
 * This allows for a consistent look to all table headers.
 * Has the same borders and alignment as with the SortedTableHeaderRenderer.
 */
public class UnsortedTableHeaderRenderer extends JLabel implements TableCellRenderer {

    public static void associateHeaderRenderer(TableColumnModel columnModel) {
        UnsortedTableHeaderRenderer uthr = new UnsortedTableHeaderRenderer();
        // Associate columns with this renderer.
        Enumeration<TableColumn> columns = columnModel.getColumns();
        while (columns.hasMoreElements()) {
            columns.nextElement().setHeaderRenderer(uthr);
        }
    }

    /**
     * This method is called each time a column header using this renderer
     * needs to be rendered. Implements the TableCellRenderer method.
     *
     * @param table
     * @param value
     * @param isSelected
     * @param hasFocus
     * @param row
     * @param column
     * @return
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus, int row,
                                                   int column) {
        // Configure component.
        setText(value.toString());
        setBorder(BorderFactory.createEtchedBorder());
        setHorizontalAlignment(CENTER);

        // Since the renderer is a component, return itself
        return this;
    }

    public void validate() {
    }

    public void revalidate() {
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }
}