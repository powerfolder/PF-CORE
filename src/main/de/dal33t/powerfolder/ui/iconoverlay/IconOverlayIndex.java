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
 */
package de.dal33t.powerfolder.ui.iconoverlay;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
enum IconOverlayIndex {
    NO_OVERLAY(0),
    OK_OVERLAY(1),
    SYNCING_OVERLAY(2),
    WARNING_OVERLAY(3),
    IGNORED_OVERLAY(4),
    LOCKED_OVERLAY(5);

    private int index;

    private IconOverlayIndex(int index) {
        this.index = index;
    }

    int getIndex() {
        return index;
    }
}
