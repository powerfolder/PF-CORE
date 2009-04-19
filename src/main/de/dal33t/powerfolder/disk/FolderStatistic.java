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
package de.dal33t.powerfolder.disk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.ui.SimpleTimeEstimator;

/**
 * Class to hold pre-calculated static data for a folder. Only freshly
 * calculated if needed.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.22 $
 */
public class FolderStatistic extends PFComponent {

    /**
     * if the number of files is more than MAX_ITEMS the updates will be delayed
     * to a maximum that can be configured in
     * {@link ConfigurationEntry#FOLDER_STATS_CALC_TIME}
     */
    private static final int MAX_ITEMS = 5000;

    private final Folder folder;
    private final long delay;

    private CalculationResult calculating;
    private CalculationResult current;
    private SimpleTimeEstimator estimator;

    /** Map of bytes received for a file for a member. */
    private final Map<Member, Map<FileInfo, Long>> partialSyncStatMap;

    /**
     * The Date of the last change to a folder file.
     */
    private Date lastFileChangeDate;

    // Contains this Folder's download progress
    // It differs from other counters in that it does only count
    // the "accepted" traffic. (= If the downloaded chunk was saved to a file)
    // Used to calculate ETA
    private TransferCounter downloadCounter;
    private long lastCalc;
    private MyCalculatorTask calculatorTask;

    FolderStatistic(Folder folder) {
        super(folder.getController());

        // Empty at start
        partialSyncStatMap = new ConcurrentHashMap<Member, Map<FileInfo, Long>>();
        current = new CalculationResult();
        estimator = new SimpleTimeEstimator();
        this.folder = folder;
        downloadCounter = new TransferCounter();
        delay = 1000L * ConfigurationEntry.FOLDER_STATS_CALC_TIME
            .getValueInt(getController());

        folder.addFolderListener(new MyFolderListener());
        folder.addMembershipListener(new MyFolderMembershipListener());

        // Add to NodeManager
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    // package protected called from Folder
    void scheduleCalculate() {
        if (calculating != null) {
            return;
        }
        if (!getController().getFolderRepository().hasJoinedFolder(
            folder.getInfo()))
        {
            logWarning("Unable to calc stats. Folder not joined");
            return;
        }
        long millisPast = System.currentTimeMillis() - lastCalc;
        if (calculatorTask != null) {
            return;
        }
        if (millisPast > delay || current.totalFilesCount < MAX_ITEMS) {
            setCalculateIn(50);
        } else {
            setCalculateIn(delay);
        }
    }

    // Calculator timer code
    // *************************************************************

    private void setCalculateIn(long timeToWait) {
        if (calculatorTask != null) {
            return;
        }
        calculatorTask = new MyCalculatorTask();
        try {
            getController().schedule(calculatorTask, timeToWait);
        } catch (IllegalStateException ise) {
            // ignore this happends if this shutdown in debug mode
            logFiner("IllegalStateException", ise);
        }
    }

    /**
     * Calculates the statistics
     * 
     * @private public because for test
     */
    public synchronized void calculate0() {
        if (isFiner()) {
            logFiner("-------------Recalculation statisitcs on " + folder);
        }
        long startTime = System.currentTimeMillis();
        // clear statistics before
        calculating = new CalculationResult();

        Collection<Member> members = folder.getMembersAsCollection();
        Collection<Member> membersCalulated = new ArrayList<Member>(members
            .size());

        // Calc member stats.
        for (Member member : members) {
            if (member.isCompleteyConnected() || member.isMySelf()) {
                if (calculateMemberStats(member, membersCalulated)) {
                    membersCalulated.add(member);
                }
            }
        }

        // Update the estimator with the new total sync.
        calculating.estimatedSyncDate = estimator.updateEstimate(calculating
            .getTotalSyncPercentage());

        // Switch figures
        current = calculating;
        calculating = null;
        lastCalc = System.currentTimeMillis();

        // Recalculate the last modified date of the folder.
        Date date = null;
        for (FileInfo fileInfo : folder.getKnownFiles()) {
            if (fileInfo.getModifiedDate() != null) {
                if (date == null
                    || date.compareTo(fileInfo.getModifiedDate()) < 0)
                {
                    date = fileInfo.getModifiedDate();
                }
            }
        }
        if (date != null) {
            lastFileChangeDate = date;
        }

        if (isFiner()) {
            logFiner("---------calc stats  " + folder.getName() + " done @: "
                + (System.currentTimeMillis() - startTime));
        }

        // Fire event
        folder.notifyStatisticsCalculated();
    }

    /**
     * @return the date that one of the files in the folder changed.
     */
    public Date getLastFileChangeDate() {
        return lastFileChangeDate;
    }

    private static boolean inSync(FileInfo fileInfo, FileInfo newestFileInfo) {
        if (newestFileInfo == null) {
            // It is intended not to use Reject.ifNull for performance reasons.
            throw new NullPointerException("Newest FileInfo not found of "
                + fileInfo.toDetailString());
        }
        if (fileInfo == null) {
            return false;
        }
        return !newestFileInfo.isNewerThan(fileInfo);
    }

    /**
     * @param member
     * @param alreadyConsidered
     * @return true if the member stats could be calced. false if filelist is
     *         missing.
     */
    private boolean calculateMemberStats(Member member,
        Collection<Member> alreadyConsidered)
    {
        Collection<FileInfo> files = folder.getFilesAsCollection(member);
        if (files == null) {
            logWarning("Unable to calc stats on member, no filelist yet: "
                + member);
            return false;
        }

        FolderRepository repo = getController().getFolderRepository();
        int memberFilesCount = 0;
        int memberFilesCountInSync = 0;
        // The total size of the folder at the member (including files not in
        // sync).
        long memberSize = 0;
        // Total size of files completely in sync at the member.
        long memberSizeInSync = 0;
        for (FileInfo fileInfo : files) {
            if (fileInfo.isDeleted()) {
                continue;
            }
            if (folder.getDiskItemFilter().isExcluded(fileInfo)) {
                continue;
            }

            FileInfo newestFileInfo = fileInfo.getNewestVersion(repo);
            FileInfo myFileInfo = folder.getFile(fileInfo);

            if (newestFileInfo == null) {
                logSevere("zzzz");
                newestFileInfo = fileInfo.getNewestVersion(repo);
            }
            boolean inSync = inSync(fileInfo, newestFileInfo);

            if (inSync) {
                // Remove partial stat for this member / file, if it exists.
                Map<FileInfo, Long> memberMap = partialSyncStatMap.get(member);
                if (memberMap != null) {
                    Long removedBytes = memberMap.remove(fileInfo);
                    if (removedBytes != null) {
                        if (isFiner()) {
                            logFiner("Removed partial stat for "
                                + member.getInfo().nick + ", "
                                + fileInfo.getName() + ", " + removedBytes);
                        }
                    }
                }
            }

            if (inSync && !newestFileInfo.isDeleted()) {
                boolean incoming = true;
                for (Member alreadyMember : alreadyConsidered) {
                    FileInfo otherMemberFile = alreadyMember.getFile(fileInfo);
                    if (otherMemberFile == null) {
                        continue;
                    }

                    boolean otherInSync = inSync(otherMemberFile,
                        newestFileInfo);
                    if (otherInSync) {
                        incoming = false;
                        break;
                    }
                }
                if (incoming
                    && (myFileInfo == null || newestFileInfo
                        .isNewerThan(myFileInfo)))
                {
                    calculating.incomingFilesCount++;
                }
            }

            // Count file
            memberFilesCount++;
            memberSize += fileInfo.getSize();
            if (inSync) {
                memberFilesCountInSync++;
                memberSizeInSync += fileInfo.getSize();
            }

            if (!inSync) {
                // Not in sync, therefore not added to totals
                continue;
            }

            boolean addToTotals = !newestFileInfo.isDeleted();
            for (Member alreadyM : alreadyConsidered) {
                FileInfo otherMemberFile = alreadyM.getFile(fileInfo);
                if (otherMemberFile == null) {
                    continue;
                }
                boolean otherInSync = inSync(otherMemberFile, newestFileInfo);
                if (otherInSync) {
                    // File already added to totals
                    addToTotals = false;
                    break;
                }
            }
            if (addToTotals) {
                calculating.totalFilesCount++;
                calculating.totalSize += fileInfo.getSize();
            }
        }

        calculating.filesCount.put(member, memberFilesCount);
        calculating.filesCountInSync.put(member, memberFilesCountInSync);
        calculating.sizes.put(member, memberSize);
        // logWarning("put: " + member + ", sizeinSync: " + memberSizeInSync);
        calculating.sizesInSync.put(member, memberSizeInSync);
        return true;
    }

    public String toString() {
        return "Folder statistic on '" + folder.getName() + '\'';
    }

    public long getTotalSize() {
        return current.totalSize;
    }

    public int getTotalFilesCount() {
        return current.totalFilesCount;
    }

    public int getIncomingFilesCount() {
        return current.incomingFilesCount;
    }

    /**
     * @param member
     * @return the number of files this member has
     */
    public int getFilesCount(Member member) {
        Integer count = current.filesCount.get(member);
        return count != null ? count : 0;
    }

    /**
     * @param member
     * @return the number of files this member has in sync
     */
    public int getFilesCountInSync(Member member) {
        Integer count = current.filesCountInSync.get(member);
        return count != null ? count : 0;
    }

    /**
     * @param member
     * @return the members ACTUAL size of this folder.
     */
    public long getSize(Member member) {
        Long size = current.sizes.get(member);
        return size != null ? size : 0;
    }

    /**
     * @param member
     * @return the members size of this folder that is in sync with the latest
     *         version.
     */
    public long getSizeInSync(Member member) {
        Long size = current.sizesInSync.get(member);
        return size != null ? size : 0;
    }

    /**
     * @return number of local files
     */
    public int getLocalFilesCount() {
        Integer integer = current.filesCount.get(getController().getMySelf());
        return integer != null ? integer : 0;
    }

    /**
     * Answers the sync percentage of a member
     * 
     * @param member
     * @return the sync percentage of the member, -1 if unknown
     */
    public double getSyncPercentage(Member member) {
        return current.getSyncPercentage(member);
    }

    /**
     * @return the local sync percentage.-1 if unknown.
     */
    public double getLocalSyncPercentage() {
        return current.getSyncPercentage(getController().getMySelf());
    }

    /**
     * @return the total sync percentange across all members.
     */
    public double getTotalSyncPercentage() {
        return current.getTotalSyncPercentage();
    }

    /**
     * @return the estimated date the folder will be in sync. May be null.
     */
    public Date getEstimatedSyncDate() {
        return current.estimatedSyncDate;
    }

    /**
     * @return my ACTUAL size of this folder.
     */
    public long getLocalSize() {
        Long size = current.sizes.get(getController().getMySelf());
        return size != null ? size : 0;
    }

    /**
     * @return the most important sync percentage for the selected sync profile.
     *         -1 if unknown
     */
    public double getHarmonizedSyncPercentage() {
        if (SyncProfile.AUTOMATIC_SYNCHRONIZATION.equals(folder
            .getSyncProfile()))
        {
            return getTotalSyncPercentage();
        } else if (SyncProfile.BACKUP_SOURCE.equals(folder.getSyncProfile())) {
            // In this case only remote sides matter.
            // Calc average sync % of remote sides.
            double maxSync = -1;
            for (Member member : folder.getMembersAsCollection()) {
                if (member.isMySelf()) {
                    continue;
                }
                double memberSync = getSyncPercentage(member);
                maxSync = Math.max(maxSync, memberSync);
            }
            return maxSync;
        }
        return getLocalSyncPercentage();
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

    @Override
    public String getLoggerName() {
        return super.getLoggerName() + " '" + folder.getName() + '\'';
    }

    /**
     * Put a partial sync stat in the holding map.
     * 
     * @param fileInfo
     * @param member
     * @param bytesTransferred
     */
    public void putPartialSyncStat(FileInfo fileInfo, Member member,
        long bytesTransferred)
    {
        if (isFiner()) {
            logFiner("Partial stat for " + fileInfo.getName() + ", "
                + member.getInfo().nick + ", " + bytesTransferred);
        }
        Map<FileInfo, Long> memberMap = partialSyncStatMap.get(member);
        if (memberMap == null) {
            memberMap = new ConcurrentHashMap<FileInfo, Long>();
            partialSyncStatMap.put(member, memberMap);
        }
        memberMap.put(fileInfo, bytesTransferred);
        if (current != null) {
            current.estimatedSyncDate = estimator.updateEstimate(current
                .getTotalSyncPercentage());
        }
        folder.notifyStatisticsCalculated();
    }

    // Inner classes *********************************************************

    private class MyCalculatorTask extends TimerTask {
        public void run() {
            calculate0();
            calculatorTask = null;
        }
    }

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

        public boolean fireInEventDispatchThread() {
            return false;
        }

    }

    private class MyFolderListener extends FolderAdapter {

        public void remoteContentsChanged(FolderEvent folderEvent) {
            // Recalculate statistics
            scheduleCalculate();
        }

        public void scanResultCommited(FolderEvent folderEvent) {
            if (folderEvent.getScanResult().isChangeDetected()) {
                // Recalculate statistics
                scheduleCalculate();
            }
        }

        public void fileChanged(FolderEvent folderEvent) {
            // Recalculate statistics
            scheduleCalculate();
        }

        public void filesDeleted(FolderEvent folderEvent) {
            // Recalculate statistics
            scheduleCalculate();
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
            // Recalculate statistics
            scheduleCalculate();
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            // do not implement may cause loop!
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }

    /**
     * Listens to the nodemanager and triggers recalculation if required
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
            calculateIfRequired(e);
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
            // Do nothing
        }

        public void startStop(NodeManagerEvent e) {
            // Do nothing
        }

        public boolean fireInEventDispatchThread() {
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

    private class CalculationResult {

        // Total size of folder in bytes
        public long totalSize;

        // Total number of files
        public int totalFilesCount;

        // Date at which the folder should be synchronized.
        public Date estimatedSyncDate;

        // Finer values
        public int incomingFilesCount;

        // Number of files
        public Map<Member, Integer> filesCount = new HashMap<Member, Integer>();

        // Number of files in sync
        public Map<Member, Integer> filesCountInSync = new HashMap<Member, Integer>();

        // Size of folder per member
        public Map<Member, Long> sizes = new HashMap<Member, Long>();

        // Size of folder that are in sync per member
        public Map<Member, Long> sizesInSync = new HashMap<Member, Long>();

        /**
         * Calculate the sync percentage for a member. This is the size of files
         * in sync divided by the total size of the folder.
         * 
         * @param member
         * @return the sync percentage for the given member
         */
        public double getSyncPercentage(Member member) {
            Long size = sizesInSync.get(member);
            if (size == null) {
                size = 0L;
            }
            if (totalSize == 0) {
                return 100.0;
            } else if (size == 0) {
                return 0;
            } else {

                // Total up partial transfers for this member.
                Map<FileInfo, Long> map = partialSyncStatMap.get(member);
                long partialTotal = 0;
                if (map != null) {
                    for (FileInfo fileInfo : map.keySet()) {
                        Long partial = map.get(fileInfo);
                        if (partial != null) {
                            partialTotal += partial;
                        }
                    }
                }

                // Sync = synchronized file sizes plus any partials divided by
                // total size.
                double sync = 100.0 * (size + partialTotal)
                    / totalSize;
                if (isFiner()) {
                    logFiner("Sync for member " + member.getInfo().nick + ", "
                        + size + " + " + partialTotal
                        + " / " + totalSize + " = " + sync);
                }

                if (Double.compare(sync, 100.0) > 0) {
                    logSevere("Sync percentage > 100% - folder="
                        + folder.getInfo().name + ", member="
                        + member.getInfo().nick + ", sync=" + sync);
                    sync = 100.0;
                }
                return sync;
            }
        }

        /**
         * Calculate the total sync percentage for a folder. This is the sync
         * percentage for each member divided by the number of members (the
         * average).
         * 
         * @return the total sync percentage for a folder
         */
        public double getTotalSyncPercentage() {
            if (sizesInSync.isEmpty()) {
                return 100.0;
            }
            double syncSum = 0;
            for (Member member : sizesInSync.keySet()) {
                syncSum += getSyncPercentage(member);
            }
            double sync = syncSum / sizesInSync.size();
            if (Double.compare(sync, 100.0) > 0) {
                logSevere("Sync percentage > 100% - folder="
                    + folder.getInfo().name + ", sync=" + sync);
                sync = 100.0;
            }
            return sync;
        }
    }
}