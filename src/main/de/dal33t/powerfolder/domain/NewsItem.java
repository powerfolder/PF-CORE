package de.dal33t.powerfolder.domain;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.collection.CompositeCollection;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;

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

    public NewsItem(Date date, AccountInfo account, FolderInfo folder) {
        super();
        Reject.ifNull(date, "Date");
        Reject.ifNull(folder, "Folder");
        this.date = date;
        this.account = account;
        this.folder = folder;
        this.newFiles = new TreeSet<FileInfo>(
            FileInfoComparator
                .getComparator(FileInfoComparator.BY_RELATIVE_NAME));
        this.updatedFiles = new TreeSet<FileInfo>(
            FileInfoComparator
                .getComparator(FileInfoComparator.BY_RELATIVE_NAME));
        this.computers = new HashSet<MemberInfo>();
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

    public Collection<FileInfo> getFiles() {
        return new CompositeCollection<FileInfo>(newFiles, updatedFiles);
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
                updatedFiles.add(fInfo);
            }
        }
        computers.add(fInfo.getModifiedBy());
    }

    @Override
    public String toString() {
        return "LogPost [account=" + account + ", date=" + date + ", folder="
            + folder + ", newFiles=" + newFiles + ", updatedFiles="
            + updatedFiles + "]";
    }

    public int compareTo(NewsItem o) {
        return -this.date.compareTo(o.date);
    }
}
