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
 * $Id: ExpandableFolderView.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.util.ui;

import java.awt.EventQueue;
import java.awt.Image;
import java.util.TimerTask;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.Icons;

/**
 * Displays a rotating sync icon when visible.
 * 
 * @author sprajc
 */
public class SyncIconHelper extends PFUIComponent {

    private static final long ROTATION_STEP_DELAY = 100L;
    private int state;
    private Image current;
    private boolean visible;

    public SyncIconHelper(Controller controller) {
        super(controller);
        state = 0;
        controller.scheduleAndRepeat(new MyUpdateTask(), ROTATION_STEP_DELAY);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) {
            getUIController().setTrayIcon(null);
        }
    }

    private void rotate() {
        state++;
        if (state >= Icons.SYNC_ANIMATION.length) {
            state = 0;
        }
        current = Icons.getImageById(Icons.SYNC_ANIMATION[state]);
        // System.out.println("Current: " + current + ", state: " + state);
        getUIController().setTrayIcon(current);
    }

    private class MyUpdateTask extends TimerTask {
        public void run() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if (visible) {
                        rotate();
                    }
                }
            });
        }
    }
}
