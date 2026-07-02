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
 */
package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class AccessDeniedProblem extends ResolvableProblem {
    private final FolderInfo folderInfo;

    public AccessDeniedProblem(FolderInfo folderInfo) {
        Reject.ifNull(folderInfo, "Folder");
        this.folderInfo = folderInfo;
    }

    @Override
    public String getDescription() {
        return Translation.get("folder_problem.access_denied");
    }

    @Override
    public String getWikiLinkKey() {
        return WikiLinks.SECURITY_PERMISSION;
    }

    public Folder getFolder(final Controller controller) {
        return folderInfo.getFolder(controller);
    }

    @Override
    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                final Folder folder = controller.getFolderRepository()
                    .getFolder(folderInfo);
                if (folder != null) {
                    controller.getIOProvider().startIO(new Runnable() {
                        public void run() {
                            controller.getFolderRepository().addToIgnoredFolders(folder);
                            controller.getFolderRepository().removeFolder(
                                folder, false);
                        }
                    });
                }
            }
        };
    }

    @Override
    public String getResolutionDescription() {
        return Translation
            .get("folder_problem.access_denied.remove_folder");
    }

    @Override
    public String toString() {
        return "AccessDeniedProblem{" +
                "folderInfo=" + folderInfo +
                '}';
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
        if (!(obj instanceof AccessDeniedProblem))
            return false;
        AccessDeniedProblem other = (AccessDeniedProblem) obj;
        if (folderInfo == null) {
            if (other.folderInfo != null)
                return false;
        } else if (!folderInfo.equals(other.folderInfo))
            return false;
        return true;
    }

}
