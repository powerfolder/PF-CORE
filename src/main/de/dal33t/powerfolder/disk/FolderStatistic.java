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
 * $Id: FolderStatistic.java 20344 2012-11-26 00:58:39Z sprajc $
 */
package de.dal33t.powerfolder.disk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.DiskItemFilterListener;
import de.dal33t.powerfolder.event.FolderAdapter;
import de.dal33t.powerfolder.event.FolderEvent;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.event.PatternChangedEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderStatisticInfo;
import de.dal33t.powerfolder.util.SimpleTimeEstimator;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Util;

/**
 * Class to hold pre-calculated static data for a folder. Only freshly
 * calculated if needed.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.22 $
 */
public class FolderStatistic extends PFComponent {
    private static final Logger LOG = Logger.getLogger(FolderStatistic.class
        .getName());

    public static final int UNKNOWN_SYNC_STATUS = -1;

    /**
     * if the number of files is more than MAX_ITEMS the updates will be delayed
     * to a maximum that can be configured in
     * {@link ConfigurationEntry#FOLDER_STATS_CALC_TIME}
     */
    private static final int MAX_ITEMS = 5000;

    private final Folder folder;
    private final long delay;

    private volatile FolderStatisticInfo calculating;
    private volatile FolderStatisticInfo current;
    private SimpleTimeEstimator estimator;

    /**
     * The Date of the last change to a folder file.
     */
    private Date lastFileChangeDate;

    // Contains this Folder's download progress
    // It differs from other counters in that it does only count
    // the "accepted" traffic. (= If the downloaded chunk was saved to a file)
    // Used to calculate ETA
    private TransferCounter downloadCounter;
    private volatile MyCalculatorTask calculatorTask;
    private NodeManagerListener nodeManagerListener;

    FolderStatistic(Folder folder) {
        super(folder.getController());

        estimator = new SimpleTimeEstimator();
        this.folder = folder;
        downloadCounter = new TransferCounter();
        delay = 1000L * ConfigurationEntry.FOLDER_STATS_CALC_TIME
            .getValueInt(getController());

        MyFolderListener listener = new MyFolderListener();
        folder.addFolderListener(listener);
        folder.addMembershipListener(listener);
        folder.getDiskItemFilter().addListener(listener);

        nodeManagerListener = new MyNodeManagerListener();
        // Add to NodeManager
        getController().getNodeManager().addWeakNodeManagerListener(
            nodeManagerListener);

        if (!folder.isDeviceDisconnected()) {
            Path file = folder.getSystemSubDir().resolve(
                Folder.FOLDER_STATISTIC);
            // Load cached disk results
            current = FolderStatisticInfo.load(file);
        }
        if (current == null) {
            current = new FolderStatisticInfo(folder.getInfo());
        }
    }

    // package protected called from Folder
    public long scheduleCalculate() {
        if (calculating != null) {
            return -1L;
        }
        if (!getController().getFolderRepository().hasJoinedFolder(
            folder.getInfo()))
        {
            logFine("Unable to calc stats. Folder not joined");
            return -1L;
        }

        // long millisPast = System.currentTimeMillis() - lastCalc;
        if (calculatorTask != null) {
            return -1L;
        }
        if (current.getAnalyzedFiles() < MAX_ITEMS) {
            return setCalculateIn(2000);
        } else {
            return setCalculateIn(delay);
        }
    }

    // Calculator timer code
    // *************************************************************

    private Object calculatorInit = new Object();

    private long setCalculateIn(long minimumWait) {
        synchronized (calculatorInit) {
            if (calculatorTask != null) {
                return -1;
            }
            // logWarning("Scheduled new calculation", new
            // RuntimeException("here"));
            calculatorTask = new MyCalculatorTask();
            try {
                // PFC-2941: Avoid peaks in threadpool:
                // Best effort: 1-5ms takes calculation in avg. per folder
                double delay = Math.random()
                    * getController().getFolderRepository().getFoldersCount()
                    * 2;
                long wait = minimumWait + (long) delay;
                getController().schedule(calculatorTask, wait);
                return wait;
            } catch (IllegalStateException ise) {
                // ignore this happens if this shutdown in debug mode
                logFiner("IllegalStateException", ise);
            }
            return -1;
        }
    }

    /**
     * Calculates the statistics
     *
     * @private public because for test
     */
    public synchronized void calculate0() {
        if (isFiner()) {
            logFiner("-------------Recalculation statistics on " + folder);
        }
        if (!folder.isStarted()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        // clear statistics before
        calculating = new FolderStatisticInfo(folder.getInfo());

        Collection<Member> members = folder.getMembersAsCollection();
        Collection<Member> membersCalulated = new ArrayList<Member>(
            members.size());

        // Calc member stats.
        for (Member member : members) {
            if (member.isCompletelyConnected() || member.isMySelf()) {
                if (calculateMemberStats(member, membersCalulated)) {
                    membersCalulated.add(member);
                }
            }
        }

        // Update the estimator with the new total sync.
        calculating.setEstimatedSyncDate(estimator.updateEstimate(calculating
            .getAverageSyncPercentage()));

        // Archive size
        long archiveStart = System.currentTimeMillis();
        calculating.setArchiveSize(folder.getFileArchiver().getSize());
        long archiveTook = System.currentTimeMillis() - archiveStart;
        if (archiveTook > 1000L * 60 && isWarning()) {
            logWarning("Calculating archive size took " + (archiveTook / 1000)
                + "s");
        }

        // Switch figures / Take over partial sync infos.
        calculating.getPartialSyncStatMap().putAll(
            current.getPartialSyncStatMap());
        current = calculating;
        calculating = null;

        if (!folder.isDeviceDisconnected()) {
            Path tempFile = folder.getSystemSubDir().resolve(
                Folder.FOLDER_STATISTIC + ".writing");
            Path file = folder.getSystemSubDir().resolve(
                Folder.FOLDER_STATISTIC);
            if (current.save(tempFile)) {
                try {
                    Files.deleteIfExists(file);
                    Files.move(tempFile, file);
                } catch (IOException e) {
                    try {
                        Files.copy(tempFile, file);
                        Files.delete(tempFile);
                    } catch (IOException e2) {
                    }
                }
                // Ignore exceptions. Folder Statistics are not crucial for operations.
            }
        }

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

        if (isFine()) {
            long took = System.currentTimeMillis() - startTime;
            double perf = took != 0 ? (current.getAnalyzedFiles() / took) : 0;
            logFine("Recalculation completed (" + current.getAnalyzedFiles()
                + " Files analyzed) in " + took + "ms. Performance: " + perf
                + " ana/ms. Sync: " + getHarmonizedSyncPercentage());
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

    private static boolean inSync(Member member, FileInfo fileInfo,
        FileInfo newestFileInfo)
    {
        if (newestFileInfo == null) {
            // It is intended not to use Reject.ifNull for performance reasons.
            throw new NullPointerException("Newest FileInfo not found of "
                + fileInfo.toDetailString());
        }
        if (fileInfo == null) {
            return false;
        }
        boolean insync = !newestFileInfo.isNewerThan(fileInfo)
            && !fileInfo.isNewerThan(newestFileInfo);
        if (insync && newestFileInfo.getSize() != fileInfo.getSize()
            && !fileInfo.getFolderInfo().isMetaFolder()
            && LOG.isLoggable(Level.WARNING))
        {
            LOG.warning("File in sync, but size differs.\n" + "Newest: "
                + newestFileInfo.toDetailString() + "\n@" + member.getNick()
                + ":" + fileInfo.toDetailString());
        }
        return insync;
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
            if (!folder.isStarted()) {
                return false;
            }
            calculating.setAnalyzedFiles(calculating.getAnalyzedFiles() + 1);
            if (fileInfo.isDeleted()) {
                continue;
            }
            if (folder.getDiskItemFilter().isExcluded(fileInfo)) {
                continue;
            }

            FileInfo newestFileInfo = fileInfo.getNewestVersion(repo);
            FileInfo myFileInfo = folder.getFile(fileInfo);

            if (newestFileInfo == null) {
                if (folder.hasWritePermission(member)) {
                    logWarning("Newest version not found for "
                        + fileInfo.toDetailString());
                }
                // newestFileInfo = fileInfo;
                continue;
            }
            boolean inSync = inSync(member, fileInfo, newestFileInfo);

            if (inSync) {
                // Remove partial stat for this member / file, if it exists.
                Map<FileInfo, Long> memberMap = current.getPartialSyncStatMap()
                    .get(member.getInfo());
                if (memberMap != null) {
                    Long removedBytes = memberMap.remove(fileInfo);
                    if (removedBytes != null) {
                        if (isFiner()) {
                            logFiner("Removed partial stat for "
                                + member.getInfo().nick + ", "
                                + fileInfo.getRelativeName() + ", "
                                + removedBytes);
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

                    boolean otherInSync = inSync(alreadyMember,
                        otherMemberFile, newestFileInfo);
                    if (otherInSync) {
                        incoming = false;
                        break;
                    }
                }
                if (incoming
                    && (myFileInfo == null || newestFileInfo
                        .isNewerThan(myFileInfo)))
                {
                    calculating.setIncomingFilesCount(calculating
                        .getIncomingFilesCount() + 1);
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
                boolean otherInSync = inSync(alreadyM, otherMemberFile,
                    newestFileInfo);
                if (otherInSync) {
                    // File already added to totals
                    addToTotals = false;
                    break;
                }
            }
            if (addToTotals) {
                calculating
                    .setTotalFilesCount(calculating.getTotalFilesCount() + 1);
                calculating.setTotalSize(calculating.getTotalSize()
                    + fileInfo.getSize());
            }
        }

        calculating.getFilesCount().put(member.getInfo(), memberFilesCount);
        calculating.getFilesCountInSync().put(member.getInfo(),
            memberFilesCountInSync);
        calculating.getSizes().put(member.getInfo(), memberSize);
        // logWarning("put: " + member + ", sizeinSync: " + memberSizeInSync);
        calculating.getSizesInSync().put(member.getInfo(), memberSizeInSync);
        return true;
    }

    public String toString() {
        return "Folder statistic on '" + folder.getName() + '\'';
    }

    /**
     * @return the current statistic info.
     */
    public FolderStatisticInfo getInfo() {
        return current;
    }

    public long getTotalSize() {
        return current.getTotalSize();
    }

    public int getTotalFilesCount() {
        return current.getTotalFilesCount();
    }

    public int getIncomingFilesCount() {
        return current.getIncomingFilesCount();
    }

    /**
     * @param member
     * @return the number of files this member has
     */
    public int getFilesCount(Member member) {
        Integer count = current.getFilesCount().get(member.getInfo());
        return count != null ? count : 0;
    }

    /**
     * @param member
     * @return the number of files this member has in sync
     */
    public int getFilesCountInSync(Member member) {
        Integer count = current.getFilesCountInSync().get(member.getInfo());
        return count != null ? count : 0;
    }

    /**
     * @param member
     * @return the members ACTUAL size of this folder.
     */
    public long getSize(Member member) {
        Long size = current.getSizes().get(member.getInfo());
        return size != null ? size : 0;
    }

    /**
     * @param member
     * @return the members size of this folder that is in sync with the latest
     *         version.
     */
    public long getSizeInSync(Member member) {
        Long size = current.getSizesInSync().get(member.getInfo());
        return size != null ? size : 0;
    }

    /**
     * @return number of local files
     */
    public int getLocalFilesCount() {
        Integer integer = current.getFilesCount().get(getMySelf().getInfo());
        return integer != null ? integer : 0;
    }

    /**
     * Answers the sync percentage of a member
     *
     * @param member
     * @return the sync percentage of the member, -1 if unknown
     */
    public double getSyncPercentage(Member member) {
        return current.getSyncPercentage(member.getInfo());
    }

    /**
     * @return the local sync percentage.-1 if unknown.
     */
    public double getLocalSyncPercentage() {
        return getSyncPercentage(getMySelf());
    }

    /**
     * @return the average sync percentange across all members.
     */
    public double getAverageSyncPercentage() {
        return current.getAverageSyncPercentage();
    }

    /**
     * @return the sync percentrage of the server(s). Returns -1 if unknown/not
     *         backed up by server
     */
    public double getServerSyncPercentage() {
        double sync = -1;
        for (Member member : folder.getMembersAsCollection()) {
            if (!member.isServer()) {
                continue;
            }
            if (member.isMySelf()) {
                continue;
            }
            sync = Math.max(folder.getStatistic().getSyncPercentage(member),
                sync);
        }
        return sync;
    }

    /**
     * @return the most important sync percentage for the selected sync profile.
     *         -1 if unknown
     */
    public double getHarmonizedSyncPercentage() {
        // There are other members
        if (folder.getMembersCount() > 1) {
            // If there are no members (connected), the sync percentage is
            // unknown.
            if (folder.getConnectedMembersCount() == 0
                || current.getSizesInSync().size() <= 1)
            {
                return UNKNOWN_SYNC_STATUS;
            }
        }

        boolean twoSides = folder.getMembersCount() == 2;
        boolean backupShare = SyncProfile.HOST_FILES.equals(folder
            .getSyncProfile())
            || SyncProfile.BACKUP_SOURCE.equals(folder.getSyncProfile());

        if (twoSides) {
            // In these cases only remote sides matter.
            // Calc maximum sync % of remote sides.
            double minSync = getLocalSyncPercentage();
            for (Member member : folder.getConnectedMembers()) {
                double memberSync = getSyncPercentage(member);
                if (memberSync < 0) {
                    continue;
                }
                minSync = Math.min(minSync, memberSync);
            }
            return minSync;
        } else if (backupShare) {
            // In these cases only remote sides matter.
            // Calc maximum sync % of remote sides.
            double maxSync = 0;
            for (Member member : folder.getConnectedMembers()) {
                double memberSync = getSyncPercentage(member);
                maxSync = Math.max(maxSync, memberSync);
            }
            return maxSync;
        } else if (SyncProfile.AUTOMATIC_SYNCHRONIZATION.equals(folder
            .getSyncProfile()))
        {
            // SYNC-143
            if (folder.getMembersCount() == 1) {
                return UNKNOWN_SYNC_STATUS;
            }
            // SYNC-143

            // Average of all folder member sync percentages.
            return getLocalSyncPercentage();
        }

        // Otherwise, just return the local sync percentage.
        return getLocalSyncPercentage();
    }

    /**
     * @return the estimated date the folder will be in sync. May be null.
     */
    public Date getEstimatedSyncDate() {
        return current.getEstimatedSyncDate();
    }

    /**
     * @return my ACTUAL size of this folder.
     */
    public long getLocalSize() {
        Long size = current.getSizes().get(getMySelf().getInfo());
        return size != null ? size : 0;
    }

    /**
     * @return the archive size in stats.
     */
    public long getArchiveSize() {
        return current.getArchiveSize();
    }

    /**
     * @return the size of the server(s) backup.
     */
    public long getServerSize() {
        long size = 0;
        for (Member member : folder.getMembersAsCollection()) {
            if (!member.isServer()) {
                continue;
            }
            size = Math.max(folder.getStatistic().getSizeInSync(member), size);
        }
        return size;
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

//    @Override
//    public String getLoggerName() {
//        return super.getLoggerName() + " '" + folder.getName() + '\'';
//    }

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
            logFiner("Partial stat for " + fileInfo.getRelativeName() + ", "
                + member.getInfo().nick + ", " + bytesTransferred);
        }
        Map<FileInfo, Long> memberMap = current.getPartialSyncStatMap().get(
            member.getInfo());
        if (memberMap == null) {
            memberMap = Util.createConcurrentHashMap(4);
            current.getPartialSyncStatMap().put(member.getInfo(), memberMap);
        }
        memberMap.put(fileInfo, bytesTransferred);
        if (current != null) {
            current.setEstimatedSyncDate(estimator.updateEstimate(current
                .getAverageSyncPercentage()));
        }
        folder.notifyStatisticsCalculated();
    }
    
    public void removePartialSyncStats(Member member) {
        current.getPartialSyncStatMap().remove(member.getInfo());
    }

    // Inner classes *********************************************************

    private class MyCalculatorTask extends TimerTask {
        public void run() {
            calculate0();
            calculatorTask = null;
        }

        @Override
        public String toString() {
            return "FolderStatistic calculator for '" + folder;
        }
    }

    private class MyFolderListener extends FolderAdapter implements
        FolderMembershipListener, DiskItemFilterListener
    {

        public void memberJoined(FolderMembershipEvent folderEvent) {
            if (folderEvent.getMember().isCompletelyConnected()) {
                // Recalculate statistics
                scheduleCalculate();
            }
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            if (getController().isStarted()) {
                // Recalculate statistics
                scheduleCalculate();
            }
        }

        public void remoteContentsChanged(FolderEvent folderEvent) {
            calculateIfRequired(folderEvent);
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

        public void patternAdded(PatternChangedEvent e) {
            scheduleCalculate();
        }

        public void patternRemoved(PatternChangedEvent e) {
            scheduleCalculate();
        }

        private void calculateIfRequired(FolderEvent e) {
            if (e.getMember() != null && !e.getMember().isCompletelyConnected())
            {
                // Member not completely connected.
                return;
            }
            if (e.getMember() != null
                && !e.getMember().hasCompleteFileListFor(folder.getInfo()))
            {
                // Not full filelist yet.
                return;
            }
            scheduleCalculate();
        }
    }

    /**
     * Listens to the nodemanager and triggers recalculation if required
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyNodeManagerListener extends NodeManagerAdapter {

        public void nodeConnected(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }

        private void calculateIfRequired(NodeManagerEvent e) {
            // Reset partial stats
            if (folder == null || folder.getStatistic() == null) {
                return;
            }
            folder.getStatistic().removePartialSyncStats(e.getNode());
            if (!folder.hasMember(e.getNode())) {
                // Member not on folder
                return;
            }
            // PFS-1144: Fallback:
            if (folder.getStatistic().getHarmonizedSyncPercentage() != 100.0d) {
                scheduleCalculate();
                return;
            }
            // Fixes: PFS-1144
            if (!e.getNode().hasCompleteFileListFor(folder.getInfo())
                && e.getNode().isConnected())
            {
                // Not full filelist yet.
                return;
            }
            scheduleCalculate();
        }
    }
}
