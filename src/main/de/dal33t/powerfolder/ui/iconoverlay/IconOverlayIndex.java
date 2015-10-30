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
 * Representation of the index of the file overlay icon for LiferayNativity library.<br />
 * For the Mac OS X icons, there is additional data "Label" and "Filename".<br />
 * "Label" is a name that gets shown if icons are unavailable.<br />
 * "Filename" is the name of the icon file within the App Bundle's Resources folder.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public enum IconOverlayIndex {
    NO_OVERLAY     (0, "None",    ""),
    OK_OVERLAY     (1, "OK",      "ok.icns"),
    SYNCING_OVERLAY(2, "Syncing", "syncing.icns"),
    WARNING_OVERLAY(3, "Warning", "warning.icns"),
    IGNORED_OVERLAY(4, "Ignored", "ignored.icns"),
    LOCKED_OVERLAY (5, "Locked",  "locked.icns");

    private int index;
    private String label;
    private String filename;

    private IconOverlayIndex(int index, String label, String filename) {
        this.index = index;
        this.label = label;
        this.filename = filename;
    }

    public int getIndex() {
        return index;
    }

    public String getLabel() {
        return label;
    }

    public String getFilename() {
        return filename;
    }
}
