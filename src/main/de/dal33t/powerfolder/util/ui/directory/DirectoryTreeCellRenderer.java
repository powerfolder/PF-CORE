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
