/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: Folder.java 10493 2009-11-18 23:24:26Z tot $
 */
package de.dal33t.powerfolder.disk;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * TRAC #711: Automatic change detection by watching the filesystem.
 * <p>
 * Does NOT watch Meta Folders.
 * 
 * @author sprajc
 */
public class FolderWatcher extends PFComponent {

    private static final boolean UNREGISTER_WATCHERS = true;
    private static Boolean LIB_LOADED;

    private Folder folder;
    private int watchID = -1;
    private NotifyListener listener;
    private Map<String, FileInfo> dirtyFiles = Util.createConcurrentHashMap();
    private volatile boolean ignoreAll;
    private Map<FileInfo, FileInfo> ignoreFiles = Util
        .createConcurrentHashMap();
    private AtomicBoolean scheduled = new AtomicBoolean(false);
    private ReentrantLock scannerLock = new ReentrantLock();
    private long delay;

    FolderWatcher(Folder folder) {
        super(folder.getController());
        this.folder = folder;
        this.listener = new NotifyListener();
        reconfigure(folder.getSyncProfile());
    }

    public boolean isSupported() {
        return isLibLoaded()
            && ConfigurationEntry.FOLDER_WATCHER_ENABLED
                .getValueBoolean(getController());
    }

    /**
     * Adds a file to the ingore list. Files won't get scanned by FolderWatcher
     * until they get removed.
     * 
     * @param fInfo
     */
    void addIgnoreFile(FileInfo fInfo) {
        if (!isSupported()) {
            return;
        }
        Reject.ifNull(fInfo, "FileInfo");
        ignoreFiles.put(fInfo, fInfo);
        if (isFiner()) {
            logFiner("Added to ignore: " + fInfo.toDetailString());
        }
    }

    /**
     * Removes a file from the ignore list. Files afterwards get automatically
     * scanned if a file system change event occurs.
     * 
     * @param fInfo
     */
    void removeIgnoreFile(final FileInfo fInfo) {
        if (!isSupported()) {
            return;
        }
        Reject.ifNull(fInfo, "FileInfo");
        // Delay the removal by ~1000 ms because file system events occur
        // delayed.
        getController().schedule(new TimerTask() {
            @Override
            public void run() {
                ignoreFiles.remove(fInfo.getRelativeName());
                if (isFiner()) {
                    logFiner("Removed from ignore: " + fInfo.toDetailString());
                }
            }
        }, 1000);
    }

    /**
     * @param ignoreAll
     *            if ignore all file system events. Basically suspends the
     *            FolderWatcher.
     */
    public void setIngoreAll(boolean ignoreAll) {
        this.ignoreAll = ignoreAll;
    }

    public synchronized static boolean isLibLoaded() {
        if (LIB_LOADED == null) {
            try {
                LIB_LOADED = OSUtil.loadLibrary(JNotify.class, "jnotify");
            } catch (Error e) {
                LIB_LOADED = false;
            }
        }
        return LIB_LOADED;
    }

    synchronized void remove() {
        if (!isLibLoaded()) {
            return;
        }
        if (watchID >= 0) {
            if (!UNREGISTER_WATCHERS) {
                logWarning("NOT unregistering filesystem watcher from "
                    + folder
                    + " to prevent crash. Ignoring further filesystem events");
                watchID = -1;
                return;
            }
            try {
                JNotify.removeWatch(watchID);
            } catch (JNotifyException e) {
                logWarning(e);
            } finally {
                watchID = -1;
            }
        }
    }

    synchronized void reconfigure(SyncProfile syncProfile) {
        if (folder.isEncrypted()) {
            return;
        }
        if (!isSupported()) {
            return;
        }
        if (!syncProfile.isInstantSync()) {
            remove();
            return;
        }
        if (folder.getInfo().isMetaFolder()) {
            remove();
            return;
        }
        if (folder.checkIfDeviceDisconnected()) {
            remove();
            return;
        }
        delay = 1000L * ConfigurationEntry.FOLDER_WATCHER_DELAY
            .getValueInt(getController());
        String path = folder.getLocalBase().getAbsolutePath();
        boolean watchSubtree = true;
        try {
            watchID = JNotify.addWatch(path, JNotify.FILE_ANY, watchSubtree,
                listener);
            logFine("Initialized filesystem watch on " + path + " / " + folder);
        } catch (JNotifyException e) {
            logSevere("Unable to initialize filesystem watch for " + folder
                + ". " + e);
            logFiner(e);
            watchID = -1;
        }
    }

    // Logger methods *********************************************************

    @Override
    public String getLoggerName() {
        return super.getLoggerName() + " '" + folder.getName() + '\'';
    }

    private class DirtyFilesScanner implements Runnable {

        public void run() {
            if (!scannerLock.tryLock()) {
                // Already locked
                return;
            }
            if (dirtyFiles.isEmpty()) {
                return;
            }
            if (ignoreAll) {
                return;
            }
            FileInfo dirtyFile = null;
            try {
                List<FileInfo> fileInfos = new LinkedList<FileInfo>();
                if (folder.checkIfDeviceDisconnected()) {
                    logFine("Device disconnected while scanning " + folder
                        + ": " + folder.getLocalBase());
                    dirtyFiles.clear();
                    return;
                }
                for (Entry<String, FileInfo> entry : dirtyFiles.entrySet()) {
                    dirtyFile = entry.getValue();
                    if (ignoreAll) {
                        return;
                    }
                    if (ignoreFiles.containsKey(dirtyFile)) {
                        // Ignore.
                        continue;
                    }
                    fileInfos.add(dirtyFile);
                }
                dirtyFiles.clear();
                if (!fileInfos.isEmpty()) {
                    folder.scanChangedFiles(fileInfos);
                }
                if (fileInfos.size() > 0 && isFine()) {
                    logFine("Scanned " + fileInfos.size() + " changed files");
                }
            } catch (Exception e) {
                logSevere("Unable to scan changed file: " + dirtyFile + ". "
                    + e, e);
            } finally {
                scannerLock.unlock();
            }
        }

    }

    private class NotifyListener implements JNotifyListener {
        public void fileRenamed(int wd, String rootPath, String oldName,
            String newName)
        {
            fileChanged(rootPath, oldName);
            fileChanged(rootPath, newName);
        }

        public void fileModified(int wd, String rootPath, String name) {
            fileChanged(rootPath, name);
        }

        public void fileDeleted(int wd, String rootPath, String name) {
            fileChanged(rootPath, name);
        }

        public void fileCreated(int wd, String rootPath, String name) {
            fileChanged(rootPath, name);
        }

        private void fileChanged(String rootPath, String name) {
            if (watchID < 0) {
                // Illegal / Useless
                return;
            }
            if (!isSupported()) {
                // No supported
                return;
            }
            if (!folder.scanAllowedNow()) {
                // Not allowed
                return;
            }
            if (!FileUtils.isScannable(name, folder.getInfo())) {
                return;
            }
            if (ignoreAll) {
                return;
            }
            // For linux
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }
            name = FileInfoFactory.decodeIllegalChars(name);
            if (dirtyFiles.containsKey(name)) {
                // Skipping already dirty file
                return;
            }
            try {
                FileInfo lookup = lookupInstance(rootPath, name);
                if (ignoreFiles.containsKey(lookup)) {
                    // Skipping ignored file
                    return;
                }
                dirtyFiles.put(name, lookup);
                if (!scannerLock.isLocked()) {
                    if (scheduled.compareAndSet(false, true)) {
                        getController().schedule(new Runnable() {
                            public void run() {
                                getController().getIOProvider().startIO(
                                    new DirtyFilesScanner());
                                scheduled.set(false);
                            }
                        }, delay);
                    }
                }
            } catch (Exception e) {
                logSevere("Unable to enqueue changed file for scan: "
                    + rootPath + ", " + name + ". " + e, e);
            }
        }

        private FileInfo lookupInstance(String rootPath, String rawName) {
            String name = rawName;
            if (name.contains("\\")) {
                name = name.replace('\\', '/');
            }
            return FileInfoFactory.lookupInstance(folder.getInfo(), name);
        }
    }

}
