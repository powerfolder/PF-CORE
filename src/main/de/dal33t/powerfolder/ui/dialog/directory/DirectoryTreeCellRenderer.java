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
 * $Id: DirectoryTreeCellRenderer.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.ui.dialog.directory;

import java.awt.Component;
import java.nio.file.Path;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * Class to render a directory tree node. Shows an open or closed directory icon
 * and text for the directory. NOTE: This class is package-private, not public,
 * because it should only be accessed through DirectoryChooser.
 */
class DirectoryTreeCellRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
    {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
            row, hasFocus);

        Icon icon = null;

        // Set directory name.
        if (value instanceof DirectoryTreeNode) {
            DirectoryTreeNode dtn = (DirectoryTreeNode) value;
            Path directory = (Path) dtn.getUserObject();
            if (directory != null) {
                if (dtn.isVolume()) {
                    String text = dtn.getEnhancedVolumeText();
                    if (StringUtils.isEmpty(text)) {
                        // In case there was no enhanced volume text available.
                        text = directory.toAbsolutePath().toString();
                    }
                    setText(text);
                } else {
                    setText(directory.getFileName().toString());
                }
            }
            icon = dtn.getIcon();
        }

        // Set icon open / closed.

        if (icon == null) {
            if (expanded) {
                icon = Icons.getIconById(Icons.DIRECTORY_OPEN);
            } else {
                icon = Icons.getIconById(Icons.DIRECTORY);
            }
        }
        setIcon(icon);

        return this;
    }
}
