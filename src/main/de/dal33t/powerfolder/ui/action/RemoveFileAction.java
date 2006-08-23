/* $Id: RemoveFileAction.java,v 1.19 2006/02/18 10:00:30 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SwingWorker;

/**
 * Action, which removes files locally on disk
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.19 $
 */
public class RemoveFileAction extends SelectionBaseAction {
    /**
     * @param controller
     */
    public RemoveFileAction(Controller controller, SelectionModel selectionModel)
    {
        super("deletefile", controller, selectionModel);
        setEnabled(false);
    }

    public void selectionChanged(SelectionChangeEvent event) {
        Object[] selections = getSelectionModel().getSelections();
        if (selections != null && selections.length != 0) {            
            setEnabled(true);
            for (int i = 0; i < selections.length; i++) {
                if (selections[i] instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) selections[i];
                    if (fileInfo.isDeleted()
                        || fileInfo.isDownloading(getController())
                        || fileInfo.isExpected(getController()
                            .getFolderRepository()))
                    {
                        setEnabled(false);
                        break;
                    }
                } else if (selections[i] instanceof Directory) {
                    Directory directory = (Directory) selections[i];
                    if (directory.isDeleted()
                        || directory.isExpected(getController()
                            .getFolderRepository()))
                    {
                        setEnabled(false);
                        break;
                    }
                } else {
                    setEnabled(false);
                    break;
                }
            }
        }
    }

    /** will move to recycle bin files from this dir and all its subdirs */
    private boolean moveToRecycleBin(Directory directory) {
        FolderRepository repo = directory.getRootFolder().getController()
            .getFolderRepository();
        List<Directory> subs = directory.listSubDirectories();
        for (Directory sub : subs) {
            moveToRecycleBin(sub);
        }
        List<FileInfo> files = directory.getFiles();
        List<FileInfo> existingFiles = new ArrayList<FileInfo>();
        for (FileInfo fileInfo : files) {
            File file = fileInfo.getDiskFile(repo);
            if (file.exists()) {
                existingFiles.add(fileInfo);
            }
        }
        FileInfo[] filesArray = new FileInfo[existingFiles.size()];
        filesArray = existingFiles.toArray(filesArray);
        directory.getRootFolder().removeFilesLocal(filesArray);
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        Object target = getUIController().getInformationQuarter()
            .getDisplayTarget();
        final Folder folder;
        if (target instanceof Directory) {
            folder = ((Directory) target).getRootFolder();
        } else if (target instanceof Folder) {
            folder = (Folder) target;
        } else {
            log().warn("Unable to remove files on target: " + target);
            return;
        }
        Object[] selections = getSelectionModel().getSelections();
        if (selections != null && selections.length > 0) {
            final List toRemove = new ArrayList(selections.length);
            for (int i = 0; i < selections.length; i++) {
                if (selections[i] instanceof FileInfo) {
                    toRemove.add(selections[i]);
                } else if (selections[i] instanceof Directory) {
                    toRemove.add(selections[i]);
                } else {
                    log().debug(
                        "cannot remove: " + selections[i].getClass().getName());
                    return;
                }
            }
            boolean containsDirectory = false;
            String fileListText = "";
            for (int i = 0; i < toRemove.size(); i++) {
                Object object = toRemove.get(i);
                if (object instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) object;
                    fileListText += fileInfo.getFilenameOnly() + "\n";
                } else if (object instanceof Directory) {
                    containsDirectory = true;
                    Directory directory = (Directory) object;
                    fileListText += directory.getName()
                        + "     "
                        + Translation
                            .getTranslation("delete_confimation.text_movetorecyclebin_DIR")
                        + "\n";
                }
            }

            String warningText;
            if (containsDirectory) {
                warningText = Translation
                    .getTranslation("delete_confimation.text_movetorecyclebin_directory");
            } else {
                warningText = Translation
                    .getTranslation("delete_confimation.text_movetorecyclebin");
            }

            int choice = DialogFactory.showScrollableOkCancelDialog(
                getController(), true, // modal
                true, // border
                Translation.getTranslation("delete_confimation.title"),
                warningText, fileListText, Icons.DELETE);

            if (choice == JOptionPane.OK_OPTION) {
                // TODO Use activiy visualizationworker
                SwingWorker worker = new SwingWorker() {
                    @Override
                    public Object construct()
                    {
                        boolean dirRemoved = false;
                        List<FileInfo> filesToRemove = new ArrayList<FileInfo>();
                        for (Object object : toRemove) {
                            if (object instanceof FileInfo) {
                                filesToRemove.add((FileInfo) object);
                            } else if (object instanceof Directory) {
                                Directory directoryToRemove = (Directory) object;
                                if (!moveToRecycleBin(directoryToRemove)) {
                                    log().error(
                                        "move to recyclebin failed for:"
                                            + directoryToRemove);
                                }
                                dirRemoved = true;
                            }
                        }

                        FileInfo[] filesToRemoveArray = new FileInfo[filesToRemove
                            .size()];
                        filesToRemoveArray = filesToRemove
                            .toArray(filesToRemoveArray);
                        if (filesToRemoveArray.length > 0) {
                            folder.removeFilesLocal(filesToRemoveArray);
                        }
                        if (dirRemoved) {
                            // TODO Schaatser please check this
                            folder.maintain();
                        }
                        return null;
                    }
                };

                // Use swingworker to execute deletion in background
                worker.start();
            }
        }
    }
}
