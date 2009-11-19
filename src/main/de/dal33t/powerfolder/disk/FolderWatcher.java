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

import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * TRAC #711: Automatic change detection by watching the filesystem.
 * 
 * @author sprajc
 */
public class FolderWatcher extends PFComponent {
    private static Boolean LIB_LOADED;

    private Folder folder;
    private int watchID = -1;
    private NotifyListener listener;
    private Map<String, FileInfo> dirtyFiles = Util.createConcurrentHashMap();

    FolderWatcher(Folder folder) {
        super(folder.getController());
        this.folder = folder;
        this.listener = new NotifyListener();
        reconfigure(folder.getSyncProfile());
    }

    public static boolean isSupported() {
        return Feature.WATCH_FILESYSTEM.isEnabled() && isLibLoaded();
    }

    private synchronized static boolean isLibLoaded() {
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
        if (!isLibLoaded()) {
            return;
        }
        if (!syncProfile.isAutoDetectLocalChanges()) {
            remove();
            return;
        }

        String path = folder.getLocalBase().getAbsolutePath();
        int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED
            | JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED;
        boolean watchSubtree = true;
        try {
            watchID = JNotify.addWatch(path, mask, watchSubtree, listener);
            logWarning("Initialized filesystem watch on " + path + " / "
                + folder);
        } catch (JNotifyException e) {
            logSevere("Unable to initialize filesystem watch for " + folder
                + ". " + e, e);
            watchID = -1;
        }
    }

    private ReentrantLock scannerLock = new ReentrantLock();

    private class DirtyFilesScanner implements Runnable {

        public void run() {
            synchronized (scannerLock) {
                if (scannerLock.isLocked()) {
                    // Already locked
                    return;
                }
                if (dirtyFiles.isEmpty()) {
                    return;
                }
                scannerLock.lock();
            }
            FileInfo dirtyFile = null;
            try {
                for (Entry<String, FileInfo> entry : dirtyFiles.entrySet()) {
                    dirtyFile = entry.getValue();
                    FileInfo fileInfo = folder.scanChangedFile(dirtyFile);
                    if (fileInfo == null) {
                        logWarning("Was not able to scan file: "
                            + dirtyFile.toDetailString());
                    } else {
                        logWarning("Scaned file: " + fileInfo.toDetailString());
                    }
                }
                logWarning("Scanned " + dirtyFiles.size() + " dirty files");
                dirtyFiles.clear();
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

        private void fileChanged(final String rootPath, final String name) {
            if (name.contains(Constants.POWERFOLDER_SYSTEM_SUBDIR)) {
                // Ignore
                return;
            }
            if (dirtyFiles.containsKey(name)) {
                // Skipping already dirty file
                return;
            }
            try {
                FileInfo lookup = lookupInstance(rootPath, name);
                dirtyFiles.put(name, lookup);
                synchronized (scannerLock) {
                    if (!scannerLock.isLocked()) {
                        getController().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                getController().getIOProvider().startIO(
                                    new DirtyFilesScanner());
                            }
                        }, 500);
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
            // File file = new File(rootPath + File.separatorChar + name);
            // return FileInfoFactory.lookupInstance(folder, file);
        }
    }

}
