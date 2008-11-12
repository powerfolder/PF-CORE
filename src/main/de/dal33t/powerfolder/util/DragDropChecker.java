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
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.folder.DirectoryTable;

import javax.swing.JTree;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class DragDropChecker {

    public static boolean allowDrop(DropTargetDragEvent dropTargetEvent,
        File targetLocation)
    {
        if (!dropTargetEvent
            .isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        {
            return false;
        }
        try {
            List<File> sourceFileList = (List<File>) dropTargetEvent
                .getTransferable().getTransferData(
                    DataFlavor.javaFileListFlavor);
            return allowDrop(sourceFileList, targetLocation);
        } catch (UnsupportedFlavorException e) {            
            return false;
        } catch (IOException e) {            
            return false;
        }
    }

    public static boolean allowDrop(List<File> sourceFileList,
        File targetLocation)
    {
        Reject.ifNull(sourceFileList, "sourceFileList must not be null");
        Reject.ifNull(targetLocation, "targetLocation must not be null");

        try {
            for (File sourceFile : sourceFileList) {
                if (!sourceFile.exists()) {
                    // deleted in the mean time?
                    return false;
                }
                if (sourceFile.isFile()) {
                    File sourceFileLocation = sourceFile.getParentFile();
                    if (sourceFileLocation.getCanonicalPath().equals(
                        targetLocation.getCanonicalPath()))
                    {   // do not copy on itself
                        return false;
                    }
                } else if (sourceFile.isDirectory()) {
                    if (sourceFile.getCanonicalPath().equals(
                        targetLocation.getCanonicalPath()))
                    {   // folder not on its self
                        return false;
                    }
                    if (isSubdir(targetLocation, sourceFile)) {
                        // do not allow to copy a folder to a subdir of itself
                        return false;
                    }
                } else {
                    // io error or deleted in the meantime?
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Thread.dumpStack();
            throw new RuntimeException(e);
           // return false;
        }
    }

    /**
     * copy allowed if valid target (must be a folder/Directory and not the same
     * dir and not a subdir
     */
    public static boolean allowDropCopy(Controller controller,
        DropTargetDragEvent dtde)
    {
        if (!(dtde.getDropAction() == DnDConstants.ACTION_COPY || dtde
            .getDropAction() == DnDConstants.ACTION_MOVE))
        {
            return false;
        }
        if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return false;
        }

        try {
            DropTarget dt = (DropTarget) dtde.getSource();
            Object whereDidEventOccur = dt.getComponent();
            DirectoryTable directoryTable = null;
            if (whereDidEventOccur == controller.getUIController()
                    .getInformationQuarter().getMyFolderPanel().getFilesTab()
                    .getDirectoryTable()) {
                directoryTable = controller.getUIController()
                        .getInformationQuarter().getMyFolderPanel().getFilesTab()
                        .getDirectoryTable();
            } else if (whereDidEventOccur == controller.getUIController()
                    .getInformationQuarter().getPreviewFolderPanel().getFilesTab()
                    .getDirectoryTable()) {
                directoryTable = controller.getUIController()
                        .getInformationQuarter().getMyFolderPanel().getFilesTab()
                        .getDirectoryTable();
            }
            // event on the file list:
            if (whereDidEventOccur instanceof DirectoryTable) {
                if (directoryTable != null) {
                    Point location = dtde.getLocation();
                    int row = directoryTable.rowAtPoint(location);
                    if (row < 0) {
                        Directory targetDirectory = directoryTable.getDirectory();
                        return allowDrop(dtde, targetDirectory
                            .getFile());

                    }
                    // check object on row
                    Object targetObject = directoryTable.getModel().getValueAt(row,
                        0);
                    if (targetObject instanceof Directory) {
                        Directory targetDirectory = (Directory) targetObject;
                        return allowDrop(dtde, targetDirectory
                            .getFile());

                        // on file
                    } else if (targetObject instanceof FileInfo) {
                        Directory targetDirectory = directoryTable.getDirectory();
                        return allowDrop(dtde, targetDirectory
                            .getFile());
                    } else {
                        return false;
                    }
                }
                return false;
                // event on the tree:
            } else if (whereDidEventOccur instanceof JTree) {
                Point location = dtde.getLocation();
//                JTree uiTree = controller.getUIController().getControlQuarter()
//                    .getTree();
////////                TreePath path = uiTree.getPathForLocation(location.x,
////////                    location.y);
//////                if (path == null) {
//////                    return false;
//////                }
//////                Object selection = UIUtil.getUserObject(path
//////                    .getLastPathComponent());
////                if (!(selection instanceof Folder ||
////                        selection instanceof Directory))
////                {
////                    return false;
////                }
////                Directory targetLocation;
////                if (selection instanceof Folder) {
////                    targetLocation = ((Folder) selection).getDirectory();
////                } else {
////                    targetLocation = (Directory) selection;
////                }
//                return allowDrop(dtde, targetLocation.getFile());
                return false;

            } else {
                if (directoryTable != null) {
                    Directory targetDirectory = directoryTable.getDirectory();
                    // folder must be the same to allow move
                    return allowDrop(dtde, targetDirectory
                        .getFile());
                }
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /** @return true if file1 is a subdir of file 2 */
    private static boolean isSubdir(File file1, File file2) throws IOException {
        File file1Parent = file1.getParentFile();
        do {
            if (file1Parent.getCanonicalPath().equals(file2.getCanonicalPath()))
            {
                return true;
            }
            file1Parent = file1Parent.getParentFile();
        } while (file1Parent != null);
        return false;
    }
}
