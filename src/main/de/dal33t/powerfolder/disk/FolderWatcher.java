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

import java.io.File;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
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
            Runnable r = new Runnable() {
                public void run() {
                    try {

                        FileInfo lookup = lookupInstance(rootPath, name);
                        FileInfo fileInfo = folder.scanChangedFile(lookup);
                        if (fileInfo == null) {
                            logWarning("Was not able to scan file: "
                                + lookup.toDetailString());
                        } else {
                            logWarning("Scaned file: "
                                + fileInfo.toDetailString());
                        }
                    } catch (Exception e) {
                        logSevere("Unable to scan changed file: " + rootPath
                            + ", " + name + ". " + e, e);
                    }
                }
            };
            getController().getIOProvider().startIO(r);
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
