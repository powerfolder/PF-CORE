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
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.disk.dao.FileInfoDAO;
import de.dal33t.powerfolder.light.*;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A FileArchiver proxy that delegates all archive operations to the top folder's archiver,
 * mapping FileInfo paths between subfolder and top-folder views.
 * Mirrors the pattern of {@link de.dal33t.powerfolder.disk.dao.SubFolderFileInfoDAOProxy}.
 */
public class SubFolderFileArchiverProxy implements FileArchiver {

    private static final Logger log = Logger.getLogger(SubFolderFileArchiverProxy.class.getName());

    private final FileArchiverImpl delegate;
    private final FolderInfo subfolderInfo;
    private final String subfolderPath;

    public SubFolderFileArchiverProxy(FileArchiverImpl delegate, FolderInfo subfolderInfo) {
        Reject.ifNull(delegate, "delegate");
        Reject.ifNull(subfolderInfo, "subfolderInfo");
        Reject.ifFalse(subfolderInfo.isSubFolder(), "Must be subfolder");

        this.delegate = delegate;
        this.subfolderInfo = subfolderInfo;
        this.subfolderPath = subfolderInfo.getLocation().getRelativeName();
    }

    private FileInfo toTop(FileInfo f) {
        return FileInfoFactory.mapToTopFolder(f);
    }

    private FileInfo toSub(FileInfo f) {
        if (f == null) {
            return null;
        }
        return FileInfoFactory.mapToSubFolder(f, subfolderInfo);
    }

    @Override
    public void archive(FileInfo fileInfo, Path source, boolean forceKeepSource) throws IOException {
        delegate.archive(toTop(fileInfo), source, forceKeepSource);
    }

    @Override
    public boolean hasArchivedFileInfo(FileInfo fileInfo) {
        return delegate.hasArchivedFileInfo(toTop(fileInfo));
    }

    @Override
    public List<FileInfo> getArchivedFilesInfos(FileInfo fileInfo) {
        List<FileInfo> topResults = delegate.getArchivedFilesInfos(toTop(fileInfo));
        return topResults.stream().map(this::toSub).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<FileInfo> getSortedArchivedFilesInfos(FileInfo fileInfo) {
        List<FileInfo> topResults = delegate.getSortedArchivedFilesInfos(toTop(fileInfo));
        return topResults.stream().map(this::toSub).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public Path getArchivedFile(FileInfo fileInfo) {
        return delegate.getArchivedFile(toTop(fileInfo));
    }

    @Override
    public boolean restore(FileInfo versionInfo, Path target) throws IOException {
        return delegate.restore(toTop(versionInfo), target);
    }

    @Override
    public boolean restore(FileInfo versionInfo, FileInfo currentFile, Path target) throws IOException {
        return delegate.restore(toTop(versionInfo), currentFile != null ? toTop(currentFile) : null, target);
    }

    @Override
    public int getVersionsPerFile() {
        return delegate.getVersionsPerFile();
    }

    @Override
    public void setVersionsPerFile(int versionsPerFile) {
        // Subfolder version setting is informational only; actual archiving uses the delegate's value
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public void purge(Folder folder, Account account) throws IOException {
        Reject.ifFalse(folder.getFileArchiver() == this, "Folder archive mismatch");
        Path subArchive = delegate.getArchiveDir().resolve(subfolderPath);
        if (Files.exists(subArchive)) {
            PathUtils.recursiveDelete(subArchive);
        }
        folder.fireArchivePurged();
        log.info("Purged subfolder archive for " + folder.getName() + " by " + account);
    }

    @Override
    public void purge(FileInfo fileInfo, Folder folder, Account account) throws IOException {
        Reject.ifFalse(folder.getFileArchiver() == this, "Folder archive mismatch");
        FileInfo topFileInfo = toTop(fileInfo);
        if (fileInfo.isDiretory()) {
            Path subArchive = delegate.getArchiveDir().resolve(topFileInfo.getRelativeName());
            if (Files.exists(subArchive)) {
                PathUtils.recursiveDelete(subArchive);
            }
        } else {
            List<FileInfo> archived = delegate.getArchivedFilesInfos(topFileInfo);
            for (FileInfo archivedInfo : archived) {
                Path archivedFile = delegate.getArchivedFile(archivedInfo);
                if (archivedFile != null && Files.exists(archivedFile)) {
                    Files.delete(archivedFile);
                }
            }
        }
        folder.fireArchivePurged();
        log.info("Purged archive of " + (fileInfo.isDiretory() ? "directory " : "file ")
            + fileInfo.getRelativeName() + " in subfolder " + folder.getName() + " by " + account);
    }

    @Override
    public List<FileInfo> maintainAndCleanup(
        Date cleanupDate, FileInfoDAO dao, FolderInfo folderInfo, AccountInfo myAccount)
    {
        return Collections.emptyList();
    }
}
