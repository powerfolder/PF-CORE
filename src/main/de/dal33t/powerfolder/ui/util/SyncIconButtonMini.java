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
 *
 */
package de.dal33t.powerfolder.ui.util;

import java.awt.EventQueue;
import java.lang.ref.WeakReference;
import java.util.TimerTask;

import javax.swing.Icon;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Translation;

/**
 * Displays a rotating sync icon when spin is true.
 *
 * @author sprajc
 */
public class SyncIconButtonMini extends JButtonMini {

    private static final long ROTATION_STEP_DELAY = 200L;
    private int angle;
    private volatile boolean spin;
    // PFC-3643: when > 0, render the (modern blue) spinner at this px size instead
    // of the bundled base frame - e.g. the avatar-sized main status glyph.
    private int spinnerSize;
    private static final Icon ICON_ZERO = Icons
        .getIconById(Icons.SYNC_ANIMATION[0]);

    public SyncIconButtonMini(Controller controller) {
        super(ICON_ZERO, Translation
            .get("sync_icon_button_mini.tip"));
        angle = 0;
        // TODO Possible memory leak
        controller.scheduleAndRepeat(new MyUpdateTask(this),
            ROTATION_STEP_DELAY);
    }

    private void rotate() {

        if (!spin) {
            Icon idle = spinnerSize > 0
                ? Icons.getSyncSpinnerFrame(0, spinnerSize) : ICON_ZERO;
            if (!idle.equals(getIcon())) {
                setIcon(idle);
            }
            return;
        }

        angle++;
        if (angle >= Icons.SYNC_ANIMATION.length) {
            angle = 0;
        }
        Icon icon = spinnerSize > 0
            ? Icons.getSyncSpinnerFrame(angle, spinnerSize)
            : Icons.getIconById(Icons.SYNC_ANIMATION[angle]);
        setIcon(icon);
    }

    public void spin(boolean b) {
        spin = b;
    }

    /** PFC-3643: render the spinner at {@code size} px (0 = bundled base size). */
    public void setSpinnerSize(int size) {
        spinnerSize = size;
        if (size > 0) {
            setIcon(Icons.getSyncSpinnerFrame(angle, size));
        }
    }

    private static class MyUpdateTask extends TimerTask {
        private WeakReference<SyncIconButtonMini> buttonReference;

        private MyUpdateTask(SyncIconButtonMini button) {
            buttonReference = new WeakReference<SyncIconButtonMini>(button);
        }

        public void run() {
            final SyncIconButtonMini button = buttonReference.get();
            if (button == null) {
                cancel();
                return;
            }
            if (button.isVisible() && button.isShowing()) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (button.isVisible() && button.isShowing()) {
                            button.rotate();
                        }
                    }
                });
            }
        }
    }
}
