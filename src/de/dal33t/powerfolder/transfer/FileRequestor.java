package de.dal33t.powerfolder.transfer;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;

/**
 * The filerequestor handles all stuff about requesting new downloads
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.17.2.1 $
 */
public class FileRequestor extends PFComponent {
    private Thread myThread;
    private Object requestTrigger = new Object();

    public FileRequestor(Controller controller) {
        super(controller);
    }

    /**
     * Starts the file requestor
     */
    public void start() {
        myThread = new Thread(new PeriodicalRequestor(), "FileRequestor");
        myThread.setPriority(Thread.MIN_PRIORITY);
        myThread.start();
        log().debug("Started");
    }

    /**
     * Triggers to request missing files on folders
     */
    public void triggerFileRequesting() {
        synchronized (requestTrigger) {
            requestTrigger.notifyAll();
        }
    }

    /**
     * Stops file requsting
     */
    public void shutdown() {
        if (myThread != null) {
            myThread.interrupt();
        }

        log().debug("Stopped");
    }

    /**
     * Requests periodically new files from the folders
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.17.2.1 $
     */
    private class PeriodicalRequestor implements Runnable {
        public void run() {
            long waitTime = getController().getWaitTime() * 4;

            while (!myThread.isInterrupted()) {
                log().debug("Requesting files");
                FolderInfo[] folders = getController().getFolderRepository()
                    .getJoinedFolderInfos();
                for (FolderInfo folderInfo : folders) {
                    Folder folder = getController().getFolderRepository()
                        .getFolder(folderInfo);
                    // Download new files on folder if autodownload is wanted
                    if (folder != null) { //maybe null during shutdown                        
                        folder.requestMissingFilesForAutodownload();
                    }
                }

                try {
                    synchronized (requestTrigger) {
                        // use waiter, will quit faster
                        requestTrigger.wait(waitTime);
                    }
                    
                    // Sleep a bit to avoid spamming
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    log().verbose(e);
                    break;
                }
            }
        }
    }
}