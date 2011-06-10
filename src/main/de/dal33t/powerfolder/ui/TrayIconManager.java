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
 * $Id: TrayIconManager.java 15105 2011-03-27 09:36:16Z harry $
 */
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DelayedUpdater;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.TimerTask;

/**
 * Encapsualtes tray icon functionality.
 * Anything to do with the tray icon should be done *HERE*.
 * This keeps all functionality encapsulated.
 * Note, if blink is true and syncing, the icon does not blink.
 */
public class TrayIconManager extends PFComponent {

    private static final long ROTATION_STEP_DELAY = 200L;

    public enum TrayIconState {
        NORMAL,
        WARNING
    }

    private TrayIcon trayIcon;
    private volatile int angle = 0;
    private volatile TrayIconState state;
    private volatile boolean blink;
    private volatile boolean syncing;

    private DelayedUpdater syncUpdater;


    public TrayIconManager(Controller controller) {
        super(controller);
        syncUpdater = new DelayedUpdater(getController(), 1000L);

        Image image = Icons.getImageById(Icons.SYSTRAY_DEFAULT);
        if (image == null) {
            logSevere("Unable to retrieve default system tray icon. "
                    + "System tray disabled");
            OSUtil.disableSystray();
            return;
        }
        trayIcon = new TrayIcon(image);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip(getController().getMySelf().getNick()
                + " | "
                + Translation.getTranslation("systray.powerfolder",
                Controller.PROGRAM_VERSION));
        getController().scheduleAndRepeat(new MyUpdateTask(),
                ROTATION_STEP_DELAY);
        getController().getFolderRepository().addFolderRepositoryListener(
                new MyFolderRepositoryListener());
    }

    /**
     * Only use this to actually display the TrayIcon. Any modifiers should be
     * done through this.
     *
     * @return
     */
    public TrayIcon getTrayIcon() {
        return trayIcon;
    }

    public void setPopupMenu(PopupMenu menu) {
        if (trayIcon != null) {
            trayIcon.setPopupMenu(menu);
        }
    }

    public void addActionListener(ActionListener actionListener) {
        if (trayIcon != null) {
            trayIcon.addActionListener(actionListener);
        }
    }

    public void setToolTip(String s) {
        if (trayIcon != null) {
            trayIcon.setToolTip(s);
        }
    }

    private void updateIcon() {

        Image image;
        if (syncing) {
            // Increment angle and get new rotation image.
            angle++;
            if (angle >= Icons.SYNC_ANIMATION.length) {
                angle = 0;
            }
            image = Icons.getImageById(Icons.SYNC_ANIMATION[angle]);
            if (trayIcon != null) {
                trayIcon.setImage(image);
            }
        } else {
            angle = 0;
            if (blink && System.currentTimeMillis() / 1000 % 2 != 0) {
                // Blank for blink.
                image = Icons.getImageById(Icons.BLANK);
            } else {
                if (state == TrayIconState.NORMAL) {
                    image = Icons.getImageById(Icons.SYSTRAY_DEFAULT);
                } else if (state == TrayIconState.WARNING) {
                    image = Icons.getImageById(Icons.WARNING);
                } else {
                    logSevere("Indeterminate TrayIcon state " + state);
                    image = Icons.getImageById(Icons.SYSTRAY_DEFAULT);
                }
            }
            if (trayIcon != null) {
                trayIcon.setImage(image);
            }

        }
    }

    /**
     * Can be:
     * NORMAL (circle icon) or
     * WARNING (triange icon)
     *
     * @param state
     */
    public void setState(TrayIconState state) {
        this.state = state;
    }

    public void setBlink(boolean blink) {
        this.blink = blink;
    }

    private void updateSyncing() {
        syncUpdater.schedule(new Runnable() {
            public void run() {
                boolean anySynchronizing = false;
                for (Folder folder : getController().getFolderRepository()
                        .getFolders(true)) {
                    if (folder.isSyncing()) {
                        anySynchronizing = true;
                        break;
                    }
                }
                syncing = anySynchronizing;
            }
        });
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    /**
     * Timer to rotate the icon.
     */
    private class MyUpdateTask extends TimerTask {
        public void run() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    updateIcon();
                }
            });
        }
    }

    private class MyFolderRepositoryListener implements
            FolderRepositoryListener {

        public void folderCreated(FolderRepositoryEvent e) {
        }

        public void folderRemoved(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
            updateSyncing();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
            updateSyncing();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }


}
