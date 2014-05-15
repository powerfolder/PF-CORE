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
* $Id$
*/
package de.dal33t.powerfolder.transfer;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Reject;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Assigns transfer priorities to files.
 * <i>Note:</i> Priority assignments won't prevent the garbage collector from discarding FileInfos.
 *
 * @author Dennis "Bytekeeper" Waldherr
 *
 */
public class TransferPriorities implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The possible priorities a file can have.
     */
    public enum TransferPriority {
        /**
         * Files with this priority should be requested at last.
         */
        LOW,
        /**
         * Files with this priority should be requested after all HIGH priority files are done.
         */
        NORMAL,
        /**
         * Files with this priority should be requested at first.
         */
        HIGH;
    }

    private transient Comparator<FileInfo> priorityComparator;

    /**
     * To save space, files with NORMAL priority are not stored
     */
    private Map<FileInfo, TransferPriority> priorities;

    /**
     * Creates a TransferPriorities object with all files set no NORMAL
     * priority.
     */
    public TransferPriorities() {
        priorities = new HashMap<FileInfo, TransferPriority>();

        priorityComparator = new Comparator<FileInfo>() {
            public int compare(FileInfo o1, FileInfo o2) {
                return getPriority(o2).compareTo(getPriority(o1));
            }
        };

        validatePriorities();
    }


    /**
     * @return a comparator which compares files based on their priority.
     */
    public Comparator<FileInfo> getComparator() {
        return priorityComparator;
    }

    /**
     * Retrieves the priority of the given file.
     * Any file whose priority hasn't been set defaults to NORMAL.
     * @param file the file to retrieve the priority of.
     * @return the priority of the file.
     */
    public synchronized TransferPriority getPriority(FileInfo file) {
        Reject.ifNull(file, "File is null");
        TransferPriority prio = priorities.get(file);
        assert prio != TransferPriority.NORMAL;
        return prio != null ? prio : TransferPriority.NORMAL;
    }

    /**
     * Assigns a file a priority.
     * @param file the file the priority should be assigned to.
     * @param priority the priority to assign.
     */
    public synchronized void setPriority(FileInfo file, TransferPriority priority) {
        Reject.noNullElements(file, priority);
        if (priority == TransferPriority.NORMAL) {
            priorities.remove(file);
        } else {
            priorities.put(file, priority);
        }
    }

    /**
     * Removes any priority given to a file.
     * A call to getPriority with the same file will yield a return
     * value of NORMAL priority.
     * @param file the file to remove the priority of
     */
    public synchronized void removeFile(FileInfo file) {
        Reject.ifNull(file, "Fileinfo is null");
        priorities.remove(file);
    }


    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        validatePriorities();
    }

    private void validatePriorities() {
        Reject.ifNull(priorities, "Priorities are null");
        assert !priorities.values().contains(TransferPriority.NORMAL) : "Found 'NORMAL' files!";
    }
}
