/* $Id: DownloadFileAction.java,v 1.18 2006/01/30 00:51:49 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;
import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.SwingWorker;

/**
 * Action called upon the selected file in files table
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.18 $
 */
public class DownloadFileAction extends SelectionBaseAction {

    public DownloadFileAction(Controller controller,
        SelectionModel selectionModel)
    {
        super("downloadfile", controller, selectionModel);
        setEnabled(false);
    }

    public void selectionChanged(SelectionChangeEvent event) {
        setEnabled(true);
        SelectionModel selectionModel = getSelectionModel();
        Object[] target = selectionModel.getSelections();
        if (target != null && target.length > 0 && target[0] != null) {
            for (int i = 0; i < target.length; i++) {
                if (target[i] instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) target[i];
                    FolderRepository repo = getController()
                        .getFolderRepository();

                    if (fileInfo.diskFileExists(getController())
                        && !fileInfo.isNewerAvailable(repo))
                    {
                        setEnabled(false);
                        break;
                    }
                    if (!(fileInfo.isDeleted() || fileInfo.isExpected(repo) || fileInfo
                        .isNewerAvailable(repo)))
                    {
                        setEnabled(false);
                        break;
                    }
                    TransferManager tm = getController().getTransferManager();
                    if (tm.getActiveDownload(fileInfo) != null) {
                        setEnabled(false);
                        break;
                    }

                    // } else {
                    // TODO: check dir for available files
                    // setEnabled(true);
                }
            }

        } else {
            setEnabled(false);
        }
    }

    public void actionPerformed(ActionEvent e) {
        DownloadWorker worker = new DownloadWorker(getSelectionModel().getSelections());
        worker.start();
    }
    
    /**
     * Starts to download upon the targets
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private final class DownloadWorker extends SwingWorker {
        private Object[] targets;

        private DownloadWorker(Object[] someTarget) {
            targets = someTarget;
        }

        @Override
        public Object construct()
        {
            if (targets != null && targets.length > 0) {
                for (int i = 0; i < targets.length; i++) {
                    if (targets[i] instanceof FileInfo) {
                        FileInfo file = (FileInfo) targets[i];
                        download(file);
                    } else if (targets[i] instanceof Directory) {
                        // download a complete dir including subs
                        Directory directory = (Directory) targets[i];
                        download(directory);
                    }
                }
            }
            return null;
        }
    }

    // download a complete dir including subs
    private void download(Directory directory) {
        List<FileInfo> files = directory.getValidFiles();
        for (int i = 0; i < files.size(); i++) {
            FileInfo file = files.get(i);
            download(file);
        }
        List subs = directory.listSubDirectories();
        for (int i = 0; i < subs.size(); i++) {
            Directory sub = (Directory) subs.get(i);
            download(sub);
        }
    }

    private void download(FileInfo fInfo) {
        FolderRepository repo = getController().getFolderRepository();
        Folder folder = fInfo.getFolder(repo);
        if (folder == null) {
            // we do not have the folder
            return;
        }
        if (!fInfo.isDownloading(getController())) {
            if (fInfo.isDeleted() || fInfo.isExpected(repo)
                || fInfo.isNewerAvailable(repo))
            {
                getController().getTransferManager().downloadNewestVersion(
                    fInfo);
            }
        }
    }
}