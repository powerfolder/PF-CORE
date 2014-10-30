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
package de.dal33t.powerfolder.event;

import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.FolderFilesChanged;
import de.dal33t.powerfolder.util.Reject;

/**
 * Event about changes in the folder, mostly file changes. Exception: Stats
 * calculated event.
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public class FolderEvent extends EventObject {
    /**
     * In case the local folder has been scanned.
     */
    private ScanResult scanResult;

    /**
     * Changes from the remote side. new list
     */
    private FileList fileList;

    /**
     * Changes from the remote side. new list
     */
    private FolderFilesChanged changeList;

    /**
     * The member which send the remote content changes.
     */
    private Member member;

    /**
     * The files that has been freshly scanned
     */
    private Collection<FileInfo> scannedFileInfos;

    /**
     * The locally deleted files
     */
    private Collection<FileInfo> deletedFileInfos;

    /**
     * The new syncprofile of the folder.
     */
    private SyncProfile newSyncProfile;

    public FolderEvent(Folder source) {
        super(source);
    }

    public FolderEvent(Folder source, ScanResult sr) {
        super(source);
        Reject.ifNull(sr, "ScanResult is null");
        this.scanResult = sr;
    }

    public FolderEvent(Folder source, FileList fileList, Member member) {
        super(source);
        Reject.ifNull(fileList, "Filelist is null");
        this.fileList = fileList;
        this.member = member;
    }

    public FolderEvent(Folder source, FolderFilesChanged changeList,
        Member member)
    {
        super(source);
        Reject.ifNull(changeList, "ChangeList is null");
        this.changeList = changeList;
        this.member = member;
    }

    public FolderEvent(Folder source, FileInfo fileInfo) {
        this(source, Collections.singleton(fileInfo), true);
    }

    public FolderEvent(Folder source, Collection<FileInfo> fileInfos,
        boolean scanned)
    {
        super(source);
        Reject.ifNull(fileInfos, "FileInfo is null");
        if (scanned) {
            this.scannedFileInfos = fileInfos;
        } else {
            this.deletedFileInfos = fileInfos;
        }
    }

    public FolderEvent(Folder source, Collection<FileInfo> fileInfos) {
        this(source, fileInfos, false);
    }

    public FolderEvent(Folder source, SyncProfile profile) {
        super(source);
        Reject.ifNull(profile, "New sync profile is null");
        this.newSyncProfile = profile;
    }

    public Folder getFolder() {
        return (Folder) getSource();
    }

    public ScanResult getScanResult() {
        return scanResult;
    }

    public FileList getFileList() {
        return fileList;
    }

    public FolderFilesChanged getChangeList() {
        return changeList;
    }

    public Member getMember() {
        return member;
    }

    public Collection<FileInfo> getScannedFileInfos() {
        return scannedFileInfos;
    }

    public Collection<FileInfo> getDeletedFileInfos() {
        return deletedFileInfos;
    }

    public SyncProfile getNewSyncProfile() {
        return newSyncProfile;
    }
}
