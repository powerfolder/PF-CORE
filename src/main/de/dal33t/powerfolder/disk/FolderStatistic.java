/* $Id: FolderStatistic.java,v 1.22 2006/03/15 16:50:46 schaatser Exp $
 */
package de.dal33t.powerfolder.disk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // The total sync percentage of the folder
    private double totalSyncPercentage;

    // Finer values
    private int incomingFilesCount;

    // Contains the sync percentages of the members
    // Member -> Double
    private Map<Member, Double> syncPercentages = new HashMap<Member, Double>();

    // Number of files
    // Member -> Integer
    private Map<Member, Integer> filesCount = new HashMap<Member, Integer>();

    // Size of folder per member
    // member -> Long
    private Map<Member, Long> sizes = new HashMap<Member, Long>();

    // Size of folder (what is in sync) per member
    // member -> Long
    private Map<Member, Long> sizesInSync = new HashMap<Member, Long>();

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
        this.downloadCounter = new TransferCounter();

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

        public void startStop(NodeManagerEvent e) {
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
        // if (true) {
        // return;
        // }
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

    /**
     * Calculates the statistics
     * 
     * @private public because for test
     */
    public synchronized void calculate0() {
        isCalculating = true;

        log().info("-------------Recalculation statisitcs on " + folder);
        long startTime = System.currentTimeMillis();
        // clear statistics before
        syncPercentages.clear();
        filesCount.clear();
        sizes.clear();
        sizesInSync.clear();
        totalSize = 0;
        totalFilesCount = 0;
        incomingFilesCount = 0;

        List<Member> members = Arrays.asList(folder.getMembers());
        Collection<Member> membersCalulated = new ArrayList<Member>(members
            .size());
        // considered.clear();
        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);
            calculateMemberStats(member, membersCalulated);
            membersCalulated.add(member);
        }
        calculateMemberAndTotalSync(membersCalulated);

        // Fire event
        folder.fireStatisticsCalculated();
        lastCalc = System.currentTimeMillis();
        isCalculating = false;
        if (logWarn) {
            log().info(
                "---------calc stats  " + folder.getName() + " done @: "
                    + (System.currentTimeMillis() - startTime));
        }
    }

    // Set<FileInfo> considered = new HashSet<FileInfo>();

    private void calculateMemberStats(Member member,
        Collection<Member> alreadyConsidered)
    {
        Collection<FileInfo> files;
        if (member.isMySelf()) {
            files = folder.getKnownFiles();
        } else {
            files = member.getLastFileListAsCollection(folder.getInfo());
        }
        if (files == null) {
            log().warn(
                "Unable to calc stats on member, no filelist yet: " + member);
            return;
        }

        int memberFilesCount = 0;
        // The total size of the folder at the member (including files not in
        // sync).
        long memberSize = 0;
        // Total size of files completely in sync at the member.
        long memberSizeInSync = 0;
        for (FileInfo fInfo : files) {
            // System.out.println("CALC ON: " + fInfo.toDetailString());
            if (fInfo.isDeleted()) {
                continue;
            }
            if (folder.getBlacklist().isIgnored(fInfo)) {
                continue;
            }
            memberFilesCount++;
            memberSize += fInfo.getSize();
            // FIXME: What if newest version is deleted?
            boolean isNewestVersion = !fInfo.isNewerAvailable(getController()
                .getFolderRepository());
            if (!isNewestVersion) {
                // Don't count file size to member and totals
                if (member.isMySelf()) {
                    incomingFilesCount++;
                }
                continue;
            }
            memberSizeInSync += fInfo.getSize();

            boolean addToTotals = true;
            int nIdenticals = 0;
            int nOthers = 0;
            for (Member alreadyM : alreadyConsidered) {
                // System.err.println(alreadyConsidered);
                FileInfo otherMemberFile = alreadyM.getFile(fInfo);
                if (otherMemberFile == null) {
                    continue;
                }
                nOthers++;
                // System.out.println("My: " + fInfo.toDetailString() +
                // "\nother: "
                // + otherMemberFile.toDetailString() + "\nidentical? "
                // + otherMemberFile.isCompletelyIdentical(fInfo));
                if (otherMemberFile.isSameVersion(fInfo)) {
                    // File already added to totals
                    nIdenticals++;
                    addToTotals = false;
                    break;
                }
                // lastNonIdentical = otherMemberFile;

            }
            if (addToTotals) {
                totalFilesCount++;
                totalSize += fInfo.getSize();
                // if (considered.contains(fInfo)) {
                //
                // System.out.println("("
                // + nOthers
                // + ", "
                // + nIdenticals
                // + ") DUPE: "
                // + fInfo.toDetailString()
                // + ": "
                // + ((lastNonIdentical != null) ? lastNonIdentical
                // .toDetailString() : "n/a"));
                // // System.err.println("GOT DUPE: " + fInfo);
                // }
                // considered.add(fInfo);
            } else {
                // System.out.println("Skipping " + fInfo.toDetailString());
            }
        }

        filesCount.put(member, memberFilesCount);
        sizes.put(member, memberSize);
        sizesInSync.put(member, memberSizeInSync);
    }

    private void calculateMemberAndTotalSync(Collection<Member> members) {
        double totalSync = 0;
        int considered = 0;
        for (Member member : members) {
            Long sizeInSync = sizesInSync.get(member);
            if (sizeInSync == null) {
                syncPercentages.put(member, -1d);
                continue;
            }
            double sync = ((double) sizeInSync) / totalSize * 100;
            if (sync > 100) {
                log().warn(
                    "Got " + sync + "% sync: " + member.getNick()
                        + ", size(in sync): " + sizeInSync + ", size: "
                        + sizes.get(member) + ", totalsize: " + totalSize);
            }
            if (totalSize == 0) {
                log().info("Got total size 0");
                sync = 100;
            }
            syncPercentages.put(member, sync);
            totalSync += sync;
            considered++;

            log().info(
                member.getNick() + ": size: " + sizes.get(member)
                    + ", size(insync): " + sizeInSync + ": " + sync + "%");
        }
        totalSyncPercentage = totalSync / considered;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    public int getIncomingFilesCount() {
        return incomingFilesCount;
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
        return sync != null ? sync.doubleValue() : 0;
    }

    /**
     * @return number of local files
     */
    public int getLocalFilesCount() {
        Integer l = filesCount.get(getController().getMySelf());
        return l != null ? l.intValue() : 0;
    }

    /**
     * @return the local sync percentage
     */
    public double getLocalSyncPercentage() {
        Double d = syncPercentages.get(getController().getMySelf());
        return d != null ? d.doubleValue() : 0;
    }

    /**
     * @return the total sync percentange across all members.
     */
    public double getTotalSyncPercentage() {
        return totalSyncPercentage;
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

    // Helper *****************************************************************

    // Logging interface ******************************************************

    public String getLoggerName() {
        return "FolderStatistic '" + folder.getName() + "'";
    }

    // General ****************************************************************

    public String toString() {
        return "Folder statistic on '" + folder.getName() + "'";
    }
}