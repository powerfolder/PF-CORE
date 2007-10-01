package de.dal33t.powerfolder.util.ui.directory;

import de.dal33t.powerfolder.ui.Icons;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;
import java.io.File;

/**
 * Class to render a directory tree node.
 * Shows an open or closed directory icon and text for the directory.
 */
public class DirectoryTreeCellRenderer extends DefaultTreeCellRenderer {

    /**
     * Render a directory cell.
     *
     * @param tree
     * @param value
     * @param sel
     * @param expanded
     * @param leaf
     * @param row
     * @param hasFocus
     * @return
     */
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        super.getTreeCellRendererComponent(
                tree, value, sel,
                expanded, leaf, row,
                hasFocus);

        // Set icon open / closed.
        if (expanded) {
            setIcon(Icons.DIRECTORY_OPEN);
        } else {
            setIcon(Icons.DIRECTORY);
        }

        // Set directory name.
        if (value instanceof DirectoryTreeNode) {
            DirectoryTreeNode dtn = (DirectoryTreeNode) value;
            File directory = (File) dtn.getUserObject();
            if (directory != null) {
                if (dtn.isVolume()) {
                    setText(directory.getAbsolutePath());
                } else {
                    setText(directory.getName());
                }
            }
        }

        return this;
    }
}
