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
package de.dal33t.powerfolder.util.ui;

import javax.swing.Icon;

import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Helper for rendering sync profiles.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class SyncProfileUtil {
    private SyncProfileUtil() {
    }

    /**
     * @param syncPercentage
     * @return the rendered sync percentage.
     */
    public static final String renderSyncPercentage(double syncPercentage) {
        if (syncPercentage >= 0) {
            return Translation.getTranslation("percent.place.holder",
                    Format.getNumberFormat().format(syncPercentage));
        }
        return Translation.getTranslation("percent.place.holder", "?");
    }

    /**
     * Returns the icon for the sync status percentage.
     * 
     * @param syncPercentage
     *            in % between 0 and 100. Below 0 unkown sync status icons is
     *            returned
     * @return
     */
    public static Icon getSyncIcon(double syncPercentage) {
        if (syncPercentage < 0) {
            return Icons.getIconById(Icons.FOLDER_SYNC_UNKNOWN);
        } else if (syncPercentage <= 20) {
            return Icons.getIconById(Icons.FOLDER_SYNC_0);
        } else if (syncPercentage <= 50) {
            return Icons.getIconById(Icons.FOLDER_SYNC_1);
        } else if (syncPercentage <= 80) {
            return Icons.getIconById(Icons.FOLDER_SYNC_2);
        } else {
            return Icons.getIconById(Icons.FOLDER_SYNC_3);
        }
    }
}
