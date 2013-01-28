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

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderOwnerPermission;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.task.FolderObtainPermissionTask;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * There is no owner in this folder.
 */
public class NoOwnerProblem extends ResolvableProblem {

    private final FolderInfo foInfo;

    public NoOwnerProblem(FolderInfo folder) {
        Reject.ifNull(folder, "Folder");
        this.foInfo = folder;
    }

    /**
     * @param folderPermissions
     * @return true if a owner could be found. false if not.
     */
    public static final boolean hasOwner(
        Map<Serializable, FolderPermission> folderPermissions)
    {
        for (Entry<Serializable, FolderPermission> entry : folderPermissions
            .entrySet())
        {
            if (entry.getKey() == null) {
                continue;
            }
            if (entry.getValue() instanceof FolderOwnerPermission) {
                return true;
            }
        }
        return false;
    }

    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_NO_OWNER;
    }

    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                AccountInfo aInfo = controller.getOSClient().getAccountInfo();
                if (aInfo == null) {
                    DialogFactory
                        .genericDialog(
                            controller,
                            Translation
                                .getTranslation("folder_problem.no_owner.no_login.title"),
                            Translation
                                .getTranslation("folder_problem.no_owner.no_login.message"),
                            GenericDialogType.ERROR);
                    return;
                }
                Folder folder = foInfo.getFolder(controller);
                if (folder != null) {
                    folder.removeProblem(NoOwnerProblem.this);
                }
                controller.getTaskManager().scheduleTask(
                    new FolderObtainPermissionTask(aInfo, foInfo));
            }
        };
    }

    @Override
    public String getDescription() {
        return Translation.getTranslation("folder_problem.no_owner", foInfo.name);
    }

    @Override
    public String getResolutionDescription() {
        return Translation.getTranslation("folder_problem.no_owner.soln_desc");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((foInfo == null) ? 0 : foInfo.hashCode());
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
        NoOwnerProblem other = (NoOwnerProblem) obj;
        if (foInfo == null) {
            if (other.foInfo != null)
                return false;
        } else if (!foInfo.equals(other.foInfo))
            return false;
        return true;
    }

}