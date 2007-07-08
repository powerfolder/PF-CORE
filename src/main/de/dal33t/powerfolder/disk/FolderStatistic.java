/* $Id: FolderStatistic.java,v 1.22 2006/03/15 16:50:46 schaatser Exp $
 */
package de.dal33t.powerfolder.disk;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import org.apache.commons.lang.time.DateUtils;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.TransferAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.TransferCounter;

/**
 * Class to hold pre-calculated static data for a folder. Only freshly
 * calculated if needed
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.22 $
 */
public class FolderStatistic extends PFComponent {
    private final Folder folder;

    // Total size of folder in bytes
    private long totalSize;

    // Total number of files
    private int totalFilesCount;

    // Finer values
    private int totalNormalFilesCount;
    private int totalExpectedFilesCount;
    private int totalDeletedFilesCount;

    // The total sync percentage of the folder
    private double totalSyncPercentage;

    // Contains the sync percentages of the members
    // Member -> Double
    private Map<Member, Double> syncPercentages = new HashMap<Member, Double>();

    // Number of files
    // Member -> Integer
    private Map<Member, Integer> filesCount = new HashMap<Member, Integer>();

    // Size of folder per member
    // member -> Long
    private Map<Member, Long> sizes = new HashMap<Member, Long>();

    // Contains this Folder's download progress
    // It differs from other counters in that it does only count
    // the "accepted" traffic. (= If the downloaded chunk was saved to a file)
    // Used to calculate ETA
    private TransferCounter downloadCounter;

    /**
     * if the number of files is more than MAX_ITEMS the updates will be delayed
     * to a maximum, one update every 20 seconds
     */
    private int MAX_ITEMS = 1000;
    private boolean isCalculating = false;
    private final long DELAY = DateUtils.MILLIS_PER_SECOND * 20;
    private long lastCalc;
    private MyTimerTask task;

    FolderStatistic(final Folder folder) {
        super(folder.getController());
        if (folder == null) {
            throw new NullPointerException("Folder is null");
        }
        this.folder = folder;

        folder.addFolderListener(new MyFolderListener());
        folder.addMembershipListener(new MyFolderMembershipListener());

        // Add to NodeManager
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());

        // Listener on transfer manager
        folder.getController().getTransferManager().addListener(
            new MyTransferManagerListener());
    }

    // Component listener *****************************************************

    /**
     * FolderMembershipListener
     */
    private class MyFolderMembershipListener implements
        FolderMembershipListener
    {
        public void memberJoined(FolderMembershipEvent folderEvent) {
            // Recalculate statistics
            scheduleCalculate();
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            // Recalculate statistics
            scheduleCalculate();
        }

        public boolean fireInEventDispathThread() {
            return false;
        }

    }

    private class MyFolderListener implements FolderListener {
        public void remoteContentsChanged(FolderEvent folderEvent) {
            scheduleCalculate();
        }

        public void folderChanged(FolderEvent folderEvent) {
            // Recalculate statistics
            scheduleCalculate();
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            // do not implement may cause loop!
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
        }

        public void scanResultCommited(FolderEvent folderEvent) {
        }
        
        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    /**
     * Listener on transfermanager
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyTransferManagerListener extends TransferAdapter {
        public void downloadCompleted(TransferManagerEvent event) {
            // Calculate new statistic when download completed
            if (event.getFile().getFolderInfo().equals(folder.getInfo())) {
                scheduleCalculate();
            }
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    /**
     * Listens to the nodemanager and triggers recalculcation if required
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyNodeManagerListener implements NodeManagerListener {
        public void nodeRemoved(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void nodeAdded(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void nodeConnected(NodeManagerEvent e) {
            // Do not calculate, since memberJoined is always fired
            // calculateIfRequired(e);
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void friendAdded(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void friendRemoved(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }

        private void calculateIfRequired(NodeManagerEvent e) {
            if (!folder.hasMember(e.getNode())) {
                // Member not on folder
                return;
            }
            scheduleCalculate();
        }
    }

    // package protected called from Folder
    void scheduleCalculate() {
        if (isCalculating) {
            return;
        }
        long millisPast = System.currentTimeMillis() - lastCalc;
        if (task != null) {
            return;
        }
        if (millisPast > DELAY || totalFilesCount < MAX_ITEMS) {
            setCalculateIn(0);
        } else {
            setCalculateIn(DELAY);
        }
    }

    // Timer code *************************************************************

    private class MyTimerTask extends TimerTask {
        public void run() {
            calculate0();
            task = null;
        }
    }

    private void setCalculateIn(long timeToWait) {
        if (task != null) {
            return;
        }
        task = new MyTimerTask();
        try {
            getController().schedule(task, timeToWait);
        } catch (IllegalStateException ise) {
            // ignore this happends if this shutdown in debug mode
            log().verbose(ise);
        }
    }

    long startTime = System.currentTimeMillis();

    /**
     * Calculates the statistics
     */
    synchronized void calculate0() {
        if (logVerbose) {
            log().verbose(
                "calc stats  " + folder.getName() + " stats@: "
                    + (System.currentTimeMillis() - startTime));
        }
        isCalculating = true;

        // log().verbose("Recalculation statisitcs on " + folder);
        // clear statistics before
        syncPercentages.clear();
        filesCount.clear();
        sizes.clear();

        // Get ALL files, also offline users
        Collection<FileInfo> allFiles = folder.getAllFilesAsCollection(true);

        totalNormalFilesCount = 0;
        totalExpectedFilesCount = 0;
        totalDeletedFilesCount = 0;

        // Containing all deleted files
        Set<FileInfo> deletedFiles = new HashSet<FileInfo>();

        for (FileInfo fInfo : deletedFiles) {
            if (fInfo.isDeleted()) {
                totalDeletedFilesCount++;
                deletedFiles.add(fInfo);
            } else if (fInfo.isExpected(folder.getController()
                .getFolderRepository()))
            {
                totalExpectedFilesCount++;
            } else {
                totalNormalFilesCount++;
            }
        }
        if (logVerbose) {
            log()
                .verbose(
                    "Got " + deletedFiles.size()
                        + " total deleted files on folder");
        }
        // calculate total sizes
        totalSize = calculateSize(allFiles, true);
        totalFilesCount = allFiles.size();

        int nCalculatedMembers = 0;
        double totalSyncTemp = 0;

        Member[] members = folder.getMembers();
        for (int i = 0; i < members.length; i++) {
            Member member = members[i];
            Collection<FileInfo> memberFileList = folder
                .getFilesAsCollection(member);
            if (memberFileList == null) {
                continue;
            }

            long memberSize = calculateSize(memberFileList, true);
            int memberFileCount = memberFileList.size();

            Set<FileInfo> memberFiles = new HashSet<FileInfo>(memberFileList);
            for (Iterator it = deletedFiles.iterator(); it.hasNext();) {
                FileInfo deletedOne = (FileInfo) it.next();
                if (!memberFiles.contains(deletedOne)) {
                    // Add to my size
                    memberSize += deletedOne.getSize();
                    memberFileCount++;
                }
            }

            if (member.isMySelf()) {
                // Size which this client is going to download based on sync
                // profile
                long downloadSize;
                long downloaded;
                if (folder.getSyncProfile().isAutodownload()) {
                    downloadSize = totalSize;
                    downloaded = memberSize;
                } else {
                    downloadSize = 0;
                    downloaded = 0;
                    for (FileInfo fi : allFiles) {
                        if (getController().getTransferManager()
                            .isDownloadingActive(fi)
                            || getController().getTransferManager()
                                .isDownloadingPending(fi))
                        {
                            downloadSize += fi.getSize();
                        }
                    }
                    if (logVerbose) {
                        log().verbose(
                            "memberSize: " + memberSize + ", downloadSize: "
                                + downloadSize);
                    }
                }

                if (downloadCounter == null
                    || downloadCounter.getBytesExpected() != downloadSize)
                {
                    // Initialize downloadCounter with appropriate values
                    assert (downloadSize >= downloaded);
                    downloadCounter = new TransferCounter(downloaded,
                        downloadSize);
                }
            }

            double syncPercentage = (((double) memberSize) / totalSize) * 100;
            if (totalSize == 0) {
                syncPercentage = 100;
            }

            nCalculatedMembers++;
            totalSyncTemp += syncPercentage;

            syncPercentages.put(member, new Double(syncPercentage));
            filesCount.put(member, new Integer(memberFileCount));
            sizes.put(member, new Long(memberSize));
        }

        // Calculate total sync
        totalSyncPercentage = totalSyncTemp / nCalculatedMembers;
        if (logVerbose) {
            log().verbose(
                "Recalculated: " + totalNormalFilesCount + " normal, "
                    + totalExpectedFilesCount + " expected, "
                    + totalDeletedFilesCount + " deleted");
        }
        // Fire event
        folder.fireStatisticsCalculated();
        lastCalc = System.currentTimeMillis();
        isCalculating = false;
        if (logVerbose) {
            log().verbose(
                "calc stats  " + folder.getName() + " done @: "
                    + (System.currentTimeMillis() - startTime));
        }
    }

    /**
     * @param files
     * @param countDeleted
     *            if deleted files should be counted to the total size
     * @return the total size in bytes of a filelist
     */
    private static long calculateSize(Collection<FileInfo> files,
        boolean countDeleted)
    {
        if (files == null) {
            return 0;
        }
        long totalSize = 0;
        for (FileInfo fInfo : files) {
            if ((countDeleted && fInfo.isDeleted()) || !fInfo.isDeleted()) {
                // do not count if file is deleted and count-deleted is enabled
                totalSize += fInfo.getSize();
            }
        }
        return totalSize;
    }

    /**
     * @param member
     * @return if we have statistics for this member
     */
    public boolean hasStatistic(Member member) {
        return syncPercentages.get(member) != null;
    }

    /**
     * @param member
     * @return the number of files this member has
     */
    public int getFilesCount(Member member) {
        Integer count = filesCount.get(member);
        return count != null ? count.intValue() : 0;
    }

    /**
     * @param member
     * @return the members size of this folder
     */
    public long getSize(Member member) {
        Long size = sizes.get(member);
        return size != null ? size.longValue() : 0;
    }

    /**
     * Answers the sync percentage of a member
     * 
     * @param member
     * @return the sync percentage of the member, -1 if unknown
     */
    public double getSyncPercentage(Member member) {
        Double sync = syncPercentages.get(member);
        return sync != null ? sync.doubleValue() : -1;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    public int getTotalDeletedFilesCount() {
        return totalDeletedFilesCount;
    }

    public int getTotalExpectedFilesCount() {
        return totalExpectedFilesCount;
    }

    /**
     * @return number of local files
     */
    public int getTotalNormalFilesCount() {
        return totalNormalFilesCount;
    }

    public double getTotalSyncPercentage() {
        return totalSyncPercentage;
    }

    public Folder getFolder() {
        return folder;
    }

    /**
     * Returns the download-TransferCounter for this Folder
     * 
     * @return a TransferCounter or null if no such information is available
     *         (might be available later)
     */
    public TransferCounter getDownloadCounter() {
        return downloadCounter;
    }

    // Logging interface ******************************************************

    public String getLoggerName() {
        return "FolderStatistic '" + folder.getName() + "'";
    }

    // General ****************************************************************

    public String toString() {
        return "Folder statistic on '" + folder.getName() + "'";
    }
}