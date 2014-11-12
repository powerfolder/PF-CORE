/*
 * Copyright 2004 - 2014 Christian Sprajc. All rights reserved.
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
 * $Id: FolderScanner.java 18828 2012-05-10 01:24:49Z tot $
 */
package de.dal33t.powerfolder.disk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.ListenerSupportFactory;
import de.dal33t.powerfolder.event.LockingEvent;
import de.dal33t.powerfolder.event.LockingListener;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.ByteSerializer;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;

/**
 * PFC-1962: The main class for locking and unlocking files.
 * 
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.114 $
 */
public class Locking extends PFComponent {
    private static final String LOCK_FILE_EXT = ".lck";
    private LockingListener listenerSupport;

    public Locking(Controller controller) {
        super(controller);
        this.listenerSupport = ListenerSupportFactory
            .createListenerSupport(LockingListener.class);
    }

    // API

    /**
     * @param fInfo
     *            the file to lock
     * @return true if the file is now successfully locked. false if problem
     *         occurred while locking
     */
    public boolean lock(FileInfo fInfo) {
        return lock(fInfo, getMySelf().getAccountInfo());
    }

    /**
     * @param fInfo
     *            the file to lock
     * @param by
     *            the account who locked
     * @return true if the file is now successfully locked. false if problem
     *         occurred while locking
     */
    public boolean lock(FileInfo fInfo, AccountInfo by) {
        Reject.ifNull(fInfo, "FileInfo");
        Lock lock = new Lock(fInfo, getMySelf().getInfo(), by);
        Path lockFile = getLockFile(fInfo);
        if (lockFile == null) {
            return false;
        }
        if (Files.exists(lockFile) && isWarning()) {
            Lock existingLock = getLock(fInfo);
            logWarning("Overwriting existing lock " + existingLock);
        }
        try {
            byte[] buf = ByteSerializer.serializeStatic(lock, false);
            PathUtils.copyFromStreamToFile(new ByteArrayInputStream(buf),
                lockFile);
            scanLockFile(fInfo.getFolderInfo(), lockFile);
            fireLocked(fInfo);
            logInfo("File locked: " + fInfo);
            return true;
        } catch (IOException e) {
            logWarning("Unable to create lock file: " + lockFile + ". " + e);
            return false;
        }
    }

    /**
     * @param fInfo
     *            the file to uplock
     * @return true if the file is not longer locked. false if problem occurred
     *         while unlocking
     */
    public boolean unlock(FileInfo fInfo) {
        Reject.ifNull(fInfo, "FileInfo");
        Path lockFile = getLockFile(fInfo);
        if (lockFile == null) {
            return true;
        }
        try {
            boolean deleted = Files.deleteIfExists(lockFile);
            if (deleted) {
                scanLockFile(fInfo.getFolderInfo(), lockFile);
                fireUnlocked(fInfo);
                logInfo("File un-locked: " + fInfo);
            }
            return true;
        } catch (IOException e) {
            try {
                // Try harder, maybe another process is working on it.
                Thread.sleep(200);
                boolean deleted = Files.deleteIfExists(lockFile);
                if (deleted) {
                    scanLockFile(fInfo.getFolderInfo(), lockFile);
                    fireUnlocked(fInfo);
                    logInfo("File un-locked: " + fInfo);
                }
                return true;
            } catch (IOException e2) {
                logWarning("Unable to remove lock file: " + lockFile + ". " + e);
                return false;
            } catch (InterruptedException e1) {
                return false;
            }
        }
    }

    /**
     * @param fInfo
     * @return true if the file is locked, other false
     */
    public boolean isLocked(FileInfo fInfo) {
        Reject.ifNull(fInfo, "FileInfo");
        Path lockFile = getLockFile(fInfo);
        return lockFile != null && Files.exists(lockFile);
    }

    /**
     * Reads detailed lock information about a file.
     * 
     * @param fInfo
     * @return the Lock object or null if not locked
     */
    public Lock getLock(FileInfo fInfo) {
        Path lockPath = getLockFile(fInfo);
        if (lockPath == null || Files.notExists(lockPath)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(lockPath)) {
            byte[] buf = StreamUtils.readIntoByteArray(in);
            return (Lock) ByteSerializer.deserializeStatic(buf, false);
        } catch (Exception e) {
            logWarning("Problems while reading lock file: " + lockPath + ". "
                + e);
            return null;
        }
    }

    /**
     * Callback from <code>MetaFolderDataHandler</code>
     * 
     * @param lockFileInfo
     */
    public void lockStateChanged(FileInfo lockFileInfo) {
        String originalFileName = lockFileInfo.getRelativeName();
        originalFileName = originalFileName.replace(Folder.METAFOLDER_LOCKS_DIR
            + "/", "");
        originalFileName = originalFileName.replace(LOCK_FILE_EXT, "");
        FolderInfo origFoInfo = lockFileInfo.getFolderInfo()
            .getParentFolderInfo();
        FileInfo fInfo = FileInfoFactory.lookupInstance(origFoInfo,
            originalFileName);
        if (lockFileInfo.isDeleted()) {
            logInfo("File locked by remote: " + fInfo);
            fireUnlocked(fInfo);
        } else {
            logInfo("File un-locked by remote: " + fInfo);
            fireLocked(fInfo);
        }
    }

    // PFC-1962 ***************************************************************

    /**
     * PFC-1962
     * 
     * @param fInfo
     *            potential lock file for office suite.
     */
    public void handlePotentialLockfile(FileInfo fInfo) {
        Reject.ifNull(fInfo, "FileInfo");
        boolean msOffice = fInfo.getRelativeName().contains(
            Constants.MS_OFFICE_FILENAME_PREFIX);
        if (msOffice) {
            autoLockMSOfficeFiles(fInfo);
        }
        boolean libreOffice = fInfo.getRelativeName().contains(
            Constants.LIBRE_OFFICE_FILENAME_PREFIX);
        if (libreOffice) {
            autoLockLibreOfficeFiles(fInfo);
        }
    }

    private void autoLockMSOfficeFiles(FileInfo fInfo) {
        FileInfo localFInfo = fInfo.getLocalFileInfo(getController()
            .getFolderRepository());
        if (localFInfo == null) {
            return;
        }
        // QUICK;
        int i = localFInfo.getRelativeName().indexOf(
            Constants.MS_OFFICE_FILENAME_PREFIX);
        if (i < 0) {
            return;
        }
        // Details:
        String fn = localFInfo.getFilenameOnly();
        if (!fn.startsWith(Constants.MS_OFFICE_FILENAME_PREFIX)) {
            return;
        }
        String editFileName = localFInfo.getRelativeName().replace(
            Constants.MS_OFFICE_FILENAME_PREFIX, "");
        FileInfo editFInfo = FileInfoFactory.lookupInstance(
            localFInfo.getFolderInfo(), editFileName);
        editFInfo = editFInfo.getLocalFileInfo(getController()
            .getFolderRepository());
        if (editFInfo == null) {
            // Try harder...
            Folder folder = localFInfo.getFolder(getController().getFolderRepository());
            int slashIndex = editFileName.indexOf("/");
            if (slashIndex >= 0) {
                editFileName = editFileName.substring(slashIndex + 1);
            }
            for (FileInfo cFInfo : folder.getKnownFiles()) {
                if (cFInfo.isDeleted()) {
                    continue;
                }
                if (cFInfo.getRelativeName().endsWith(editFileName)) {
                    if (cFInfo.getRelativeName().contains(
                        Constants.MS_OFFICE_FILENAME_PREFIX))
                    {
                        continue;
                    }
                    editFInfo = cFInfo;
                    break;
                }
            }
            if (editFInfo == null) {
                return;                
            }
        }
        if (localFInfo.isDeleted()) {
            editFInfo.unlock(getController());
        } else {
            editFInfo.lock(getController());
        }
    }

    private void autoLockLibreOfficeFiles(FileInfo fInfo) {
        FileInfo localFInfo = fInfo.getLocalFileInfo(getController()
            .getFolderRepository());
        if (localFInfo == null) {
            return;
        }
        // QUICK;
        int i = localFInfo.getRelativeName().indexOf(
            Constants.LIBRE_OFFICE_FILENAME_PREFIX);
        if (i < 0) {
            return;
        }
        // Details:
        String fn = localFInfo.getFilenameOnly();
        if (!fn.startsWith(Constants.LIBRE_OFFICE_FILENAME_PREFIX)) {
            return;
        }
        String editFileName = localFInfo.getRelativeName().replace(
            Constants.LIBRE_OFFICE_FILENAME_PREFIX, "");
        editFileName = editFileName.replace("#", "");
        FileInfo editFInfo = FileInfoFactory.lookupInstance(
            localFInfo.getFolderInfo(), editFileName);
        editFInfo = editFInfo.getLocalFileInfo(getController()
            .getFolderRepository());
        if (editFInfo == null) {
            return;
        }
        if (localFInfo.isDeleted()) {
            editFInfo.unlock(getController());
        } else {
            editFInfo.lock(getController());
        }
    }

    // PFC-1962 ***************************************************************

    public void addListener(LockingListener listener) {
        ListenerSupportFactory.addListener(listenerSupport, listener);
    }

    public void removeListener(LockingListener listener) {
        ListenerSupportFactory.removeListener(listenerSupport, listener);
    }

    // Internal helper

    private void fireLocked(FileInfo fInfo) {
        LockingEvent event = new LockingEvent(fInfo);
        listenerSupport.locked(event);
    }

    private void fireUnlocked(FileInfo fInfo) {
        LockingEvent event = new LockingEvent(fInfo);
        listenerSupport.unlocked(event);
    }

    private void scanLockFile(FolderInfo foInfo, Path lockFile) {
        Folder metaFolder = getController().getFolderRepository()
            .getMetaFolderForParent(foInfo);
        if (metaFolder == null) {
            logWarning("Meta-folder for " + foInfo + " not found");
            return;
        }
        FileInfo lockFileInfo = FileInfoFactory.lookupInstance(metaFolder,
            lockFile);
        if (metaFolder.scanChangedFile(lockFileInfo) == null) {
            logWarning("Scanning of lock file not necessary: " + lockFileInfo);
        }
    }

    private Path getLockFile(FileInfo fInfo) {
        Folder metaFolder = getController().getFolderRepository()
            .getMetaFolderForParent(fInfo.getFolderInfo());
        if (metaFolder == null) {
            logWarning("Meta-folder for " + fInfo.getFolderInfo()
                + " not found");
            return null;
        }
        Path baseDir = metaFolder.getLocalBase().resolve(
            Folder.METAFOLDER_LOCKS_DIR);
        return baseDir.resolve(FileInfoFactory.encodeIllegalChars(fInfo
            .getRelativeName() + LOCK_FILE_EXT));
    }
}
