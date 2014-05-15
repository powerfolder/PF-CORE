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
 * $Id: DirectoryTreeNode.java 5170 2008-09-08 06:21:22Z tot $
 */
package de.dal33t.powerfolder.ui.dialog.directory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.util.Icons;

/**
 * Class to represent a directory node in the tree. Initially the node has a
 * dummy node, so that the node handle is displayed by the tree. A node can be
 * scanned, which removes the dummy node and replaces with real file system
 * subdirectories. NOTE: This class is package-private, not public, because it
 * should only be accessed through DirectoryChooser.
 */
class DirectoryTreeNode extends DefaultMutableTreeNode {

    private boolean volume;
    private boolean scanned;
    private Icon icon;
    private boolean real;
    private Controller controller;

    /**
     * For volumes, this is FileSystemView.getSystemDisplayName(f)
     * http://java.dzone.com/news/getting-file-system-details-ja
     */
    private String enhancedVolumeText;

    /**
     * Constructor.
     *
     * @param controller
     * @param enhancedVolumeText for volumes, pass in the FileSystemView
     * getSystemDisplayName
     * @param directory
     * @param volume
     * @param real
     */
    DirectoryTreeNode(Controller controller, String enhancedVolumeText,
                      Path directory, boolean volume, boolean real) {
        super(directory);
        this.controller = controller;
        this.volume = volume;
        this.real = real;

        try {
            if (volume) {
                add(new DefaultMutableTreeNode());
                this.enhancedVolumeText = enhancedVolumeText;
            } else if (directory != null && Files.isDirectory(directory)
                && Files.isReadable(directory) && !Files.isHidden(directory))
            {

                // A quick peek.
                // If there are any subdirectories,
                // set scanned false and add a dummy,
                // deferring the real scan untll the node is expanded.
                // Otherwise if there are no readable directories,
                // set as scanned with no dummy node.
                scanned = true;

                // Patch for Windows Vista.
                // Vista may deny access to directories
                // and this results in a null file list.

                try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
                    for (Path entry : files) {
                        if (entry != null && Files.isReadable(entry) && Files.isDirectory(entry)
                            && !Files.isHidden(entry))
                        {
                            add(new DefaultMutableTreeNode());
                            scanned = false;
                            break;
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Scans the node directory. Remove any dummy node, and add nodes for any
     * subdirectories.
     */
    public void scan(List<String> virtualDirectories) {
        while (getChildCount() > 0) {
            remove(0);
        }
        Path f = getDir();
        if (f != null && Files.isDirectory(f) && Files.isReadable(f)) {
            try (DirectoryStream<Path> realFiles = Files.newDirectoryStream(f)) {
                Map<Path, Boolean> files = new TreeMap<Path, Boolean>();
                for (Path realFile : realFiles) {
                    files.put(realFile, true);
                }
                String baseDir = controller.getFolderRepository().getFoldersBasedirString();
                if (baseDir.equals(getDir().toAbsolutePath().toString()) &&
                    virtualDirectories != null &&
                    !virtualDirectories.isEmpty()) {
                    for (String virtualDirectory : virtualDirectories) {
                        Path file = Paths.get(baseDir, virtualDirectory);
                        if (Files.notExists(file)) {
                            files.put(file, false);
                        }
                    }
                }
                if (!files.isEmpty()) {
                    for (Map.Entry<Path, Boolean> entry : files.entrySet()) {
                        Path f2 = entry.getKey();
                        boolean realDirectory = entry.getValue();
                        if (!realDirectory ||
                            Files.isDirectory(f2) && !Files.isHidden(f2) && Files.isReadable(f2)) {
                            DirectoryTreeNode dtn2 = new DirectoryTreeNode(controller,
                                null, f2, false, realDirectory);
                            add(dtn2);
                        }
                    }
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
        scanned = true;
    }

    public void unscan() {
        scanned = false;
    }

    /**
     * @return Whether the node has been scanned.
     */
    public boolean isScanned() {
        return scanned;
    }

    /**
     * @return Whether the node is a base file system volume.
     */
    public boolean isVolume() {
        return volume;
    }

    public Path getDir() {
        return (Path) getUserObject();
    }

    public Icon getIcon() {
        if (icon == null) {
            if (real) {
                icon = FileSystemView.getFileSystemView().getSystemIcon(getDir().toFile());
            } else {
                icon = Icons.getIconById(Icons.ONLINE_FOLDER_SMALL);
            }
        }
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public String getEnhancedVolumeText() {
        return enhancedVolumeText;
    }
}
