/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.domain;

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.collection.CompositeCollection;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;

import java.io.Serializable;
import java.util.*;

/**
 * #2469: Simple transfer web log (stream, audit)
 *
 * @author Sprajc
 */
public class NewsItem implements Comparable<NewsItem>, Serializable {
    private static final long serialVersionUID = 100L;

    private Date date;
    private AccountInfo account;
    private FolderInfo folder;
    private Set<MemberInfo> computers;
    private SortedSet<FileInfo> newFiles;
    private SortedSet<FileInfo> updatedFiles;
    private SortedSet<FileInfo> deletedFiles;

    NewsItem(Date date, AccountInfo account, FolderInfo folder) {
        super();
        Reject.ifNull(date, "Date");
        Reject.ifNull(folder, "Folder");
        this.date = date;
        this.account = account;
        this.folder = folder;
        this.newFiles = new TreeSet<>(
                FileInfoComparator
                        .getComparator(FileInfoComparator.BY_RELATIVE_NAME));
        this.updatedFiles = new TreeSet<>(
                FileInfoComparator
                        .getComparator(FileInfoComparator.BY_RELATIVE_NAME));
        this.deletedFiles = new TreeSet<>(
                FileInfoComparator
                        .getComparator(FileInfoComparator.BY_RELATIVE_NAME));
        this.computers = new HashSet<>();
    }

    public AccountInfo getAccount() {
        return account;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        Reject.ifNull(date, "Date");
        this.date = date;
    }

    public FolderInfo getFolder() {
        return folder;
    }

    public Collection<MemberInfo> getComputers() {
        return Collections.unmodifiableCollection(computers);
    }

    public SortedSet<FileInfo> getNewFiles() {
        return Collections.unmodifiableSortedSet(newFiles);
    }

    public SortedSet<FileInfo> getUpdatedFiles() {
        return Collections.unmodifiableSortedSet(updatedFiles);
    }

    public SortedSet<FileInfo> getDeletedFiles() {
        return Collections.unmodifiableSortedSet(deletedFiles);
    }

    public Collection<FileInfo> getFiles() {
        return new CompositeCollection<>(newFiles, updatedFiles, deletedFiles);
    }

    public void addFile(FileInfo fInfo) {
        Reject.ifNull(fInfo, "FileInfo");
        if (fInfo.getVersion() == 0) {
            newFiles.add(fInfo);
        } else {
            if (newFiles.contains(fInfo)) {
                newFiles.remove(fInfo);
                newFiles.add(fInfo);
            } else {
                if (fInfo.isDeleted()) {
                    deletedFiles.add(fInfo);
                } else {
                    updatedFiles.add(fInfo);
                }
            }
        }
        computers.add(fInfo.getModifiedBy());
    }

    @Override
    public String toString() {
        return "LogPost [account=" + account + ", date=" + date + ", folder="
                + folder + ", newFiles="
                + newFiles + ", updatedFiles="
                + updatedFiles + ", deletedFiles="
                + deletedFiles + "]";
    }

    public int compareTo(NewsItem o) {
        return -this.date.compareTo(o.date);
    }
}
