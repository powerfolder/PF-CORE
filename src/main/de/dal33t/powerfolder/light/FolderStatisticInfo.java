/*
 * Copyright 2004 - 2011 Christian Sprajc. All rights reserved.
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
 * $Id: FolderStatistic.java 15056 2011-03-21 15:12:38Z tot $
 */
package de.dal33t.powerfolder.light;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.logging.Loggable;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;

/**
 * Contains the statistic calculation result / infos about one folder. This
 * object is produced by {@link FolderStatistic}. May be used to transfer info
 * to another computer over the wire. So make sure it is fully serializable.
 * 
 * @author sprajc
 */
public class FolderStatisticInfo extends Loggable implements Serializable {

    private static final long serialVersionUID = 1L;

    private FolderInfo folder;

    // Total size of folder in bytes
    private volatile long totalSize;

    // The archive size.
    private volatile long archiveSize;

    // Total number of files
    private volatile int totalFilesCount;

    // Date at which the folder should be synchronized.
    private volatile Date estimatedSyncDate;

    // Finer values
    private volatile int incomingFilesCount;

    private transient int analyzedFiles;

    // Number of files
    private final Map<MemberInfo, Integer> filesCount = new HashMap<MemberInfo, Integer>();

    // Number of files in sync
    private final Map<MemberInfo, Integer> filesCountInSync = new HashMap<MemberInfo, Integer>();

    // Size of folder per member
    private final Map<MemberInfo, Long> sizes = new HashMap<MemberInfo, Long>();

    // Size of folder that are in sync per member
    private final Map<MemberInfo, Long> sizesInSync = new HashMap<MemberInfo, Long>();

    /** Map of bytes received for a file for a member. */
    private final Map<MemberInfo, Map<FileInfo, Long>> partialSyncStatMap = new ConcurrentHashMap<MemberInfo, Map<FileInfo, Long>>();

    public FolderStatisticInfo(FolderInfo folder) {
        super();
        Reject.ifNull(folder, "Folder");
        this.folder = folder;
    }

    public FolderInfo getFolder() {
        return folder;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    public long getArchiveSize() {
        return archiveSize;
    }

    public void setArchiveSize(long archiveSize) {
        this.archiveSize = archiveSize;
    }

    public void setTotalFilesCount(int totalFilesCount) {
        this.totalFilesCount = totalFilesCount;
    }

    public Date getEstimatedSyncDate() {
        return estimatedSyncDate;
    }

    public void setEstimatedSyncDate(Date estimatedSyncDate) {
        this.estimatedSyncDate = estimatedSyncDate;
    }

    public int getIncomingFilesCount() {
        return incomingFilesCount;
    }

    public void setIncomingFilesCount(int incomingFilesCount) {
        this.incomingFilesCount = incomingFilesCount;
    }

    public int getAnalyzedFiles() {
        return analyzedFiles;
    }

    public void setAnalyzedFiles(int analyzedFiles) {
        this.analyzedFiles = analyzedFiles;
    }

    public Map<MemberInfo, Integer> getFilesCount() {
        return filesCount;
    }

    public Map<MemberInfo, Integer> getFilesCountInSync() {
        return filesCountInSync;
    }

    public Map<MemberInfo, Long> getSizes() {
        return sizes;
    }

    public Map<MemberInfo, Long> getSizesInSync() {
        return sizesInSync;
    }

    public Map<MemberInfo, Map<FileInfo, Long>> getPartialSyncStatMap() {
        return partialSyncStatMap;
    }

    /**
     * Calculate the sync percentage for a member. This is the size of files in
     * sync divided by the total size of the folder.
     * 
     * @param member
     * @return the sync percentage for the given member
     */
    public double getSyncPercentage(MemberInfo memberInfo) {
        Long size = sizesInSync.get(memberInfo);
        if (size == null) {
            size = 0L;
        }
        if (totalSize == 0) {
            return 100.0;
        } else if (size == 0) {
            return 0;
        } else {
            // Total up partial transfers for this member.
            Map<FileInfo, Long> map = partialSyncStatMap.get(memberInfo);
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
            double sync = 100.0 * (size + partialTotal) / totalSize;
            if (isFiner()) {
                logFiner("Sync for member " + memberInfo.nick + ", " + size
                    + " + " + partialTotal + " / " + totalSize + " = " + sync
                    + ". map: " + map);
            }
            if (Double.compare(sync, 100.0) > 0) {
                logFiner("Sync percentage > 100% - folder=" + folder.name
                    + ", member=" + memberInfo.nick + ", sync=" + sync);
                sync = 100.0;
            }
            return sync;
        }
    }

    /**
     * Calculate the average sync percentage for a folder. This is the sync
     * percentage for each member divided by the number of members.
     * 
     * @return the total sync percentage for a folder
     */
    public double getAverageSyncPercentage() {
        if (sizesInSync.isEmpty()) {
            return 100.0;
        }
        double syncSum = 0;
        for (MemberInfo memberInfo : sizesInSync.keySet()) {
            syncSum += getSyncPercentage(memberInfo);
        }
        double sync = syncSum / sizesInSync.size();
        if (Double.compare(sync, 100.0) > 0) {
            logWarning("Average sync percentage > 100% - folder=" + folder.name
                + ", sync=" + sync);
            sync = 100.0;
        }
        return sync;
    }

    /**
     * @param controller
     * @return a sever node which is syncing the folder. null if not found.
     */
    public Member getServerNode(Controller controller) {
        for (MemberInfo nodeInfo : filesCount.keySet()) {
            Member node = nodeInfo.getNode(controller, false);
            if (node != null && node.isServer()) {
                return node;
            }
        }
        return null;
    }

    // Writing / Loading *****************************************************

    /**
     * Saves this FolderStatisticInfo contents to the given OutputStream.
     * 
     * @param out
     * @throws IOException
     */
    public boolean save(File file) {
        OutputStream fout = null;
        if (isFiner()) {
            logFiner("Writing folder " + folder.getName() + " stats to " + file);
        }
        try {
            fout = new TFileOutputStream(file);
            ObjectOutputStream oout = new ObjectOutputStream(
                new BufferedOutputStream(fout));
            oout.writeObject(this);
            oout.close();
        } catch (Exception e) {
            logWarning("Unable to store stats for folder " + folder.getName()
                + " to " + file + ". " + e);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                }
            }
        }

        return true;
    }

    public static FolderStatisticInfo load(File file) {
        if (!file.exists()) {
            return null;
        }
        InputStream fin = null;
        try {
            fin = new TFileInputStream(file);
            ObjectInputStream oin = new ObjectInputStream(
                new BufferedInputStream(fin));
            return (FolderStatisticInfo) oin.readObject();
        } catch (Exception e) {
            Logger.getLogger(FolderStatisticInfo.class.getName()).warning(
                "Unable to read folder stats from " + file + ". " + e);
        } finally {
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}