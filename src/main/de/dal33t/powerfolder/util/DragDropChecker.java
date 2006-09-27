package de.dal33t.powerfolder.util;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.folder.DirectoryTable;
import de.dal33t.powerfolder.util.ui.UIUtil;

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
            Thread.dumpStack();
            return false;
        } catch (IOException e) {
            Thread.dumpStack();
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
                    {
                        return false;
                    }
                } else if (sourceFile.isDirectory()) {
                    if (sourceFile.getCanonicalPath().equals(
                        targetLocation.getCanonicalPath()))
                    {
                        return false;
                    }
                    if (isSubdir(targetLocation, sourceFile)) {
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
            return false;
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
            DirectoryTable directoryTable = controller.getUIController()
                .getInformationQuarter().getFolderPanel().getDirectoryPanel()
                .getDirectoryTable();
            DropTarget dt = (DropTarget) dtde.getSource();
            Object whereDidEventOccur = dt.getComponent();
            // event on the file list:
            if (whereDidEventOccur instanceof DirectoryTable) {
                Point location = dtde.getLocation();
                int row = directoryTable.rowAtPoint(location);
                if (row < 0) {
                    Directory targetDirectory = directoryTable.getDirectory();
                    return DragDropChecker.allowDrop(dtde, targetDirectory
                        .getFile());

                }
                // check object on row
                Object targetObject = directoryTable.getModel().getValueAt(row,
                    0);
                if (targetObject instanceof Directory) {
                    Directory targetDirectory = (Directory) targetObject;
                    return DragDropChecker.allowDrop(dtde, targetDirectory
                        .getFile());

                    // on file
                } else if (targetObject instanceof FileInfo) {
                    Directory targetDirectory = directoryTable.getDirectory();
                    return DragDropChecker.allowDrop(dtde, targetDirectory
                        .getFile());
                } else {
                    return false;
                }
                // event on the tree:
            } else if (whereDidEventOccur instanceof JTree) {
                Point location = dtde.getLocation();
                JTree uiTree = controller.getUIController().getControlQuarter()
                    .getTree();
                TreePath path = uiTree.getPathForLocation(location.x,
                    location.y);
                if (path == null) {
                    return false;
                }
                Object selection = UIUtil.getUserObject(path
                    .getLastPathComponent());
                if (!(selection instanceof Folder || selection instanceof Directory))
                {
                    return false;
                }
                Directory targetLocation = null;
                if (selection instanceof Folder) {
                    targetLocation = ((Folder) selection).getDirectory();
                } else {
                    targetLocation = (Directory) selection;
                }
                return DragDropChecker
                    .allowDrop(dtde, targetLocation.getFile());

            } else {
                Directory targetDirectory = directoryTable.getDirectory();
                // folder must be the same to allow move
                return DragDropChecker.allowDrop(dtde, targetDirectory
                    .getFile());
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
