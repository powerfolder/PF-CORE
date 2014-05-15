/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
 * $Id: FileInfo.java 5858 2008-11-24 02:30:33Z tot $
 */
package de.dal33t.powerfolder.light;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import de.dal33t.powerfolder.util.ImmutableList;
import de.dal33t.powerfolder.util.Reject;

/**
 * Represents the history of a file. To prevent side effects, subclasses should
 * be immutable.
 *
 * @author Dennis Waldherr
 * @author Christian Sprajc
 */
public class FileHistory implements Serializable {
    private int hashCode;

    private static final Logger log = Logger.getLogger(FileHistory.class
        .getName());
    private static final long serialVersionUID = 100L;

    private final ImmutableList<Record> history;

    public static class Conflict {
        private final FileInfo localFileInfo;
        private final FileInfo otherFileInfo;
        private final FileInfo ancestorFileInfo;

        public Conflict(FileInfo localFileInfo, FileInfo otherFileInfo,
            FileInfo ancestorFileInfo)
        {
            this.localFileInfo = localFileInfo;
            this.otherFileInfo = otherFileInfo;
            this.ancestorFileInfo = ancestorFileInfo;
        }

        /**
         * Returns the most recent FileInfo of this FileHistory.
         *
         * @return
         */
        public FileInfo getLocalFileInfo() {
            return localFileInfo;
        }

        /**
         * Returns the most recent FileInfo of the other FileHistory.
         *
         * @return
         */
        public FileInfo getOtherFileInfo() {
            return otherFileInfo;
        }

        /**
         * Returns the FileInfo ancestor common to both, the local and the
         * remote, FileInfos.
         *
         * @return a common ancestor FileInfo or null if there isn't one
         */
        public FileInfo getAncestorFileInfo() {
            return ancestorFileInfo;
        }
    }

    public static class Record {
        private final FileInfo mergedFirst;
        private final FileInfo mergedSecond;
        private final FileInfo fileInfo;

        public Record(FileInfo fileInfoA, FileInfo fileInfoB, FileInfo fileInfo)
        {
            this.mergedFirst = fileInfoA;
            this.mergedSecond = fileInfoB;
            this.fileInfo = fileInfo;
        }

        /**
         * @return one of the files merged to create the resulting FileInfo or
         *         null if no merging was done
         */
        public FileInfo getMergedFirst() {
            return mergedFirst;
        }

        /**
         * @return one of the files merged to create the resulting FileInfo or
         *         null if no merging was done
         */
        public FileInfo getMergedSecond() {
            return mergedSecond;
        }

        public boolean wasMerged() {
            return mergedFirst != null;
        }

        public FileInfo getFileInfo() {
            return fileInfo;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                + ((fileInfo == null) ? 0 : fileInfo.hashCode());
            result = prime * result
                + ((mergedFirst == null) ? 0 : mergedFirst.hashCode());
            result = prime * result
                + ((mergedSecond == null) ? 0 : mergedSecond.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Record other = (Record) obj;
            if (!fileInfo.equals(other.fileInfo)) {
                return false;
            }
            if (mergedFirst == null) {
                if (other.mergedFirst != null) {
                    return false;
                }
            } else if (!mergedFirst.equals(other.mergedFirst)) {
                return false;
            }
            if (mergedSecond == null) {
                if (other.mergedSecond != null) {
                    return false;
                }
            } else if (!mergedSecond.equals(other.mergedSecond)) {
                return false;
            }
            return true;
        }
    }

    private FileHistory(Record record) {
        super();
        Reject.ifNull(record, "file is null!");
        history = new ImmutableList<Record>(record);
    }

    private FileHistory(ImmutableList<Record> history) {
        this.history = history;
    }

    /**
     * @return the Record with the most recent version.
     */
    public Record getRecord() {
        return history.getHead();
    }

    /**
     * Tests if the given FileHistory has a version conflict with this one. A
     * conflict happens if both histories have a common VersionedFile which is
     * not the most recent version for any of the histories or if they do not
     * have a common ancestor.
     *
     * @param other
     *            another history
     * @return a {@link Conflict} or null if none was detected
     */
    public Conflict getConflictWith(FileHistory other) {
        Reject.notNull(other, "other");

        if (getRecord().getFileInfo().isVersionDateAndSizeIdentical(
            other.getRecord().getFileInfo())
            || !getRecord().getFileInfo().equals(
                other.getRecord().getFileInfo()))
        {
            return null;
        }
        FileInfo cv = getCommonVersion(other);
        if (cv == null
            || !cv.isVersionDateAndSizeIdentical(getRecord().getFileInfo())
            && !cv.isVersionDateAndSizeIdentical(other.getRecord()
                .getFileInfo()))
        {
            return new Conflict(getRecord().getFileInfo(), other.getRecord()
                .getFileInfo(), cv);
        }
        return null;
    }

    /**
     * Adds a new version to the history and replaces the most recent file.
     *
     * @param newFileInfo
     *            the file version to add
     * @return a new FileHistory with the given fileInfo as the most recent
     *         version
     */
    public FileHistory addVersion(Record newRecord) {
        Reject.ifNull(newRecord, "newRecord is null");

        if (history.getTail() != null) {
            FileInfo newFileInfo = newRecord.fileInfo;
            FileInfo lastFileInfo = history.getTail().getHead().fileInfo;
            if (lastFileInfo.getVersion() >= newFileInfo.getVersion()) {
                // Only merged histories are allowed to have FileInfos with the
                // same version in it!
                throw new IllegalStateException(
                    "Strange history add. Last file: "
                        + lastFileInfo.toDetailString() + ", added: "
                        + newFileInfo.toDetailString());
            }
        }
        return new FileHistory(history.add(newRecord));
    }

    /**
     * Returns the most recent file version that is shared by this history and
     * the given one.
     *
     * @param other
     * @return null, if the given history has no common ancestor with this
     *         history
     */
    public FileInfo getCommonVersion(FileHistory other) {
        Set<FileInfo> tmp = new HashSet<FileInfo>();
        for (Record r : history) {
            tmp.add(r.fileInfo);
        }
        for (Record r : other.history) {
            if (tmp.contains(r.fileInfo)) {
                return r.fileInfo;
            }
        }
        return null;
    }

    /**
     * Returns true, if the given FileHistory is same as this one.
     *
     * @param other
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (o != null && o.getClass() != getClass()) {
            return false;
        }
        return structuralEquals((FileHistory) o);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = 19;
            for (Record r : history) {
                hashCode = hashCode * 37 + r.hashCode();
            }
            if (hashCode == 0) {
                hashCode = -1;
            }
        }
        return hashCode;
    }

    private boolean structuralEquals(FileHistory other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        Iterator<Record> i = history.iterator(), j = other.history.iterator();
        for (; i.hasNext() && j.hasNext();) {
            if (!i.next().equals(j.next())) {
                return false;
            }
        }
        return i.hasNext() == j.hasNext();
    }
}
