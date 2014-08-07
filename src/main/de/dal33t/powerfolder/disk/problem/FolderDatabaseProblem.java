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
import de.dal33t.powerfolder.message.FolderDBMaintCommando;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * Problem where a folder has a problem with the file database. Recommends
 * cleanup
 */
public class FolderDatabaseProblem extends ResolvableProblem {

    private FolderInfo folderInfo;

    public FolderDatabaseProblem(FolderInfo folderInfo) {
        Reject.ifNull(folderInfo, "FolderInfo");
        this.folderInfo = folderInfo;
    }

    public String getDescription() {
        return Translation.getTranslation(
            "folder_problem.folderdb.description", folderInfo.getLocalizedName());
    }

    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_FOLDER_DATABASE;
    }

    public String getResolutionDescription() {
        return Translation.getTranslation("folder_problem.folderdb.soln_desc");
    }

    /**
     * Cleans up the database
     *
     * @param controller
     * @return
     */
    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                final Folder folder = folderInfo.getFolder(controller);
                if (folder == null) {
                    return;
                }
                controller.getIOProvider().startIO(new Runnable() {
                    public void run() {
                        folder.removeProblem(FolderDatabaseProblem.this);
                        folder.broadcastMessages(new FolderDBMaintCommando(
                            folderInfo, new Date()));
                        folder.maintainFolderDB(System.currentTimeMillis());
                    }
                });
            }
        };
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((folderInfo == null) ? 0 : folderInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FolderDatabaseProblem other = (FolderDatabaseProblem) obj;
        if (folderInfo == null) {
            if (other.folderInfo != null)
                return false;
        } else if (!folderInfo.equals(other.folderInfo))
            return false;
        return true;
    }
}
