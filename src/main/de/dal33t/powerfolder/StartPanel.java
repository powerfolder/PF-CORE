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
* $Id$
*/
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Loggable;

/**
 * Enumeration of preferred preselected navigation panel.
 */
public enum StartPanel {

    OVERVIEW("preferences.dialog.startPanel.overview"), MY_FOLDERS(
        "preferences.dialog.startPanel.myFolders"), DOWNLOADS(
        "preferences.dialog.startPanel.downloads");

    private String description;

    StartPanel(String descriptionKey) {
        description = Translation.getTranslation(descriptionKey);
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        return description;
    }

    public static StartPanel valueForLegacyName(String startPanelName) {
        // Migration
        try {

            if (startPanelName != null) {
                startPanelName = startPanelName.toUpperCase();
                if (startPanelName.equalsIgnoreCase("myFolders")) {
                    startPanelName = StartPanel.MY_FOLDERS.name();
                }
            }
            return valueOf(startPanelName);
        } catch (Exception e) {
            Loggable.logSevereStatic(StartPanel.class, e);
            return OVERVIEW;
        }
    }
}
