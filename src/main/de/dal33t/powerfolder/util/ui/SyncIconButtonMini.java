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
import java.util.TimerTask;

import javax.swing.Icon;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;

/**
 * Displays a rotating sync icon when spin is true.
 * 
 * @author sprajc
 */
public class SyncIconButtonMini extends JButtonMini {

    private static final long ROTATION_STEP_DELAY = 100L;
    private int state;
    private volatile boolean spin;
    private static final Icon ICON_ZERO = Icons.getIconById(Icons.SYNC_ANIMATION[0]);

    public SyncIconButtonMini(Controller controller) {
        super(ICON_ZERO, Translation.getTranslation("sync_icon_button_mini.tip"));
        state = 0;
        controller.scheduleAndRepeat(new MyUpdateTask(), ROTATION_STEP_DELAY);
    }

    private void rotate() {

        // Complete final rotation before stopping - do not jump to ICON_ZERO.
        if (state == 0 && !spin) {
            if (!getIcon().equals(ICON_ZERO)) {
                setIcon(ICON_ZERO);
            }
            return;
        }

        state++;
        if (state >= Icons.SYNC_ANIMATION.length) {
            state = 0;
        }
        Icon icon = Icons.getIconById(Icons.SYNC_ANIMATION[state]);
        setIcon(icon);
    }

    public void spin(boolean b) {
        spin = b;
    }

    private class MyUpdateTask extends TimerTask {
        public void run() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    if (isVisible()) {
                        rotate();
                    }
                }
            });
        }
    }
}
