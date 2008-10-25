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
 * $Id: SyncButtonComponent.java 5500 2008-10-25 04:23:44Z harry $
 */
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.actionold.SyncAllFoldersAction;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JLabel;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class to display and handle Sync All button.
 */
public class SyncButtonComponent extends PFUIComponent {

    private JLabel syncAllLabel;
    private final AtomicBoolean mouseOver;

    /**
     * Constructor
     *
     * @param controller
     */
    public SyncButtonComponent(Controller controller) {
        super(controller);
        mouseOver = new AtomicBoolean();

    }

    /**
     * Gets the sync 'button'.
     *
     * @return
     */
    public Component getUIComponent() {
        if (syncAllLabel == null) {
            initComponents();
        }
        return syncAllLabel;
    }

    private void initComponents() {
        syncAllLabel = new JLabel(Icons.SYNC_40_NORMAL);
        syncAllLabel.setToolTipText(
                Translation.getTranslation("scan_all_folders.description"));
        syncAllLabel.addMouseListener(new MyMouseAdapter(syncAllLabel));
    }

    /**
     * Class to handle mouse overs and clicks.
     */
    private class MyMouseAdapter extends MouseAdapter {

        private final JLabel syncLabel;

        private MyMouseAdapter(JLabel syncLabel) {
            this.syncLabel = syncLabel;
        }

        public void mousePressed(MouseEvent e) {
            syncLabel.setIcon(Icons.SYNC_40_PUSH);
        }

        public void mouseReleased(MouseEvent e) {
            boolean bool = mouseOver.get();
            if (bool) {
                syncLabel.setIcon(Icons.SYNC_40_HOVER);
                SyncAllFoldersAction.perfomSync(getController());
            } else {
                syncLabel.setIcon(Icons.SYNC_40_NORMAL);
            }
        }

        public void mouseEntered(MouseEvent e) {
            syncLabel.setIcon(Icons.SYNC_40_HOVER);
            mouseOver.set(true);
        }

        public void mouseExited(MouseEvent e) {
            syncLabel.setIcon(Icons.SYNC_40_NORMAL);
            mouseOver.set(false);
        }
    }
}
