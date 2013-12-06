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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.FolderStatistic;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * Contains the statistic calculation result / infos about one folder. This
 * object is produced by {@link FolderStatistic}. May be used to transfer info
 * to another computer over the wire. So make sure it is fully serializable.
 * 
 * @author sprajc
 */
public class FolderStatisticInfo extends Loggable implements Serializable {

    private static final int MAX_FILE_SIZE = 500 * 1024;

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
    private Map<MemberInfo, Map<FileInfo, Long>> partialSyncStatMap = Util
        .createConcurrentHashMap();

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
        if (partialSyncStatMap == null) {
            partialSyncStatMap = Util.createConcurrentHashMap();
        }
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
            Map<FileInfo, Long> map = getPartialSyncStatMap().get(memberInfo);
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

    public boolean save(Path file) {
        if (isFiner()) {
            logFiner("Writing folder " + folder.getName() + " stats to " + file);
        }
        try (OutputStream fout = Files.newOutputStream(file)) {
            ObjectOutputStream oout = new ObjectOutputStream(
                new BufferedOutputStream(fout));
            oout.writeObject(this);
            oout.close();
        } catch (Exception e) {
            logWarning("Unable to store stats for folder " + folder.getName()
                + " to " + file + ". " + e);
        }

        return true;
    }

    public static FolderStatisticInfo load(Path file) {
        if (Files.notExists(file)) {
            return null;
        }
        try {
            if (Files.size(file) > MAX_FILE_SIZE) {
                Logger.getLogger(FolderStatisticInfo.class.getName()).warning(
                    "Not reading folder stats from " + file
                        + ". File is too big: "
                        + Format.formatBytes(Files.size(file)));
                return null;
            }
        } catch (IOException e) {
            Logger.getLogger(FolderStatisticInfo.class.getName()).severe(
                "Unable to read folder stats size from " + file + ". " + e);
        }
        try (InputStream fin = Files.newInputStream(file)) {
            ObjectInputStream oin = new ObjectInputStream(
                new BufferedInputStream(fin));
            FolderStatisticInfo stats = (FolderStatisticInfo) oin.readObject();
            // PFS-818: Check if not corrupt;
            if (stats.isValid()) {
                return stats;
            }
        } catch (Exception e) {
            Logger.getLogger(FolderStatisticInfo.class.getName()).fine(
                "Unable to read folder stats from " + file + ". " + e);
        } catch (OutOfMemoryError e) {
            Logger.getLogger(FolderStatisticInfo.class.getName()).severe(
                "Unable to read folder stats from " + file + ". " + e);

        }
        return null;
    }

    // Serialization *********************************************

    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException
    {
        in.defaultReadObject();
        if (this.partialSyncStatMap == null) {
            this.partialSyncStatMap = Util.createConcurrentHashMap();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (archiveSize ^ (archiveSize >>> 32));
        result = prime * result
            + ((estimatedSyncDate == null) ? 0 : estimatedSyncDate.hashCode());
        result = prime * result
            + ((filesCount == null) ? 0 : filesCount.hashCode());
        result = prime * result
            + ((filesCountInSync == null) ? 0 : filesCountInSync.hashCode());
        result = prime * result + ((folder == null) ? 0 : folder.hashCode());
        result = prime * result + incomingFilesCount;
        result = prime * result + ((sizes == null) ? 0 : sizes.hashCode());
        result = prime * result
            + ((sizesInSync == null) ? 0 : sizesInSync.hashCode());
        result = prime * result + totalFilesCount;
        result = prime * result + (int) (totalSize ^ (totalSize >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FolderStatisticInfo other = (FolderStatisticInfo) obj;
        if (archiveSize != other.archiveSize)
            return false;
        if (estimatedSyncDate == null) {
            if (other.estimatedSyncDate != null)
                return false;
        } else if (!estimatedSyncDate.equals(other.estimatedSyncDate))
            return false;
        if (filesCount == null) {
            if (other.filesCount != null)
                return false;
        } else if (!filesCount.equals(other.filesCount))
            return false;
        if (filesCountInSync == null) {
            if (other.filesCountInSync != null)
                return false;
        } else if (!filesCountInSync.equals(other.filesCountInSync))
            return false;
        if (folder == null) {
            if (other.folder != null)
                return false;
        } else if (!folder.equals(other.folder))
            return false;
        if (incomingFilesCount != other.incomingFilesCount)
            return false;
        if (sizes == null) {
            if (other.sizes != null)
                return false;
        } else if (!sizes.equals(other.sizes))
            return false;
        if (sizesInSync == null) {
            if (other.sizesInSync != null)
                return false;
        } else if (!sizesInSync.equals(other.sizesInSync))
            return false;
        if (totalFilesCount != other.totalFilesCount)
            return false;
        if (totalSize != other.totalSize)
            return false;
        return true;
    }
    
    /**
     * PFS-818
     * @return
     */
    public boolean isValid() {
        return folder != null && filesCount != null && filesCountInSync != null
            && sizes != null && sizesInSync != null
            && partialSyncStatMap != null;
    }

    
    @Override
    public String toString() {
        return "FolderStatisticInfo [folder=" + folder + ", totalSize="
            + totalSize + ", archiveSize=" + archiveSize + ", totalFilesCount="
            + totalFilesCount + ", estimatedSyncDate=" + estimatedSyncDate
            + ", incomingFilesCount=" + incomingFilesCount + ", analyzedFiles="
            + analyzedFiles + ", filesCount=" + filesCount
            + ", filesCountInSync=" + filesCountInSync + ", sizes=" + sizes
            + ", sizesInSync=" + sizesInSync + ", partialSyncStatMap="
            + partialSyncStatMap + "]";
    }

    public static void main(String... args) {
        FolderStatisticInfo info = load(FileSystems.getDefault().getPath("FolderStatistic"));
        System.out.println(info);
    }
}
