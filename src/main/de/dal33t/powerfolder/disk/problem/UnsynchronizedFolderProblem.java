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
 * $Id: UnsynchronizedFolderProblem.java 7985 2009-05-18 07:17:34Z harry $
 */
package de.dal33t.powerfolder.disk.problem;

import java.util.Date;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Translation;

/**
 * Problem where a folder has not been synchronized in n days.
 */
public class UnsynchronizedFolderProblem extends ResolvableProblem {

    private Date lastSyncDate;
    private FolderInfo folderInfo;

    public UnsynchronizedFolderProblem(FolderInfo folderInfo, Date lastSyncDate)
    {
        this.folderInfo = folderInfo;
        this.lastSyncDate = lastSyncDate;
    }

    public String getDescription() {
        long time = System.currentTimeMillis() - lastSyncDate.getTime();
        time /= 1000;
        time /= 60;

        // Minutes
        if (time <= 59) {
            return Translation.get(
                "folder_problem.unsynchronized_minutes", folderInfo.getLocalizedName(),
                String.valueOf(time));
        }

        // Hours
        time /= 60;
        if (time <= 23) {
            return Translation.get(
                "folder_problem.unsynchronized_hours", folderInfo.getLocalizedName(),
                String.valueOf(time));
        }

        // Days
        time /= 24;
        return Translation.get("folder_problem.unsynchronized",
            folderInfo.getLocalizedName(), String.valueOf(time));
    }

    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_UNSYNCED_FOLDER;
    }

    public Folder getFolder(final Controller controller) {
        return folderInfo.getFolder(controller);
    }

    public String getResolutionDescription() {
        return Translation
            .get("folder_problem.unsynchronized.soln_desc");
    }

    /**
     * Show folder unsynced files.
     *
     * @param controller
     * @return
     */
    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                controller.getUIController().openFilesInformationUnsynced(
                    folderInfo);
            }
        };
    }

    @Override
    public String toString() {
        return getDescription() + ". lastSyncDate=" + lastSyncDate;
    }

}
