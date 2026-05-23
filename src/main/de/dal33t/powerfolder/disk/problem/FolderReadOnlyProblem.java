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

import java.nio.file.Path;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class FolderReadOnlyProblem extends ResolvableProblem {
    private final Path path;
    private final boolean revertedOnly;
    private final Folder folder;

    public FolderReadOnlyProblem(Folder folder, Path path) {
        this(folder, path, false);
    }

    public FolderReadOnlyProblem(Folder folder, Path path, boolean revertedOnly) {
        Reject.ifNull(path, "Path");
        this.path = path;
        this.revertedOnly = revertedOnly;
        this.folder = folder;
    }

    @Override
    public String getDescription() {
        if (revertedOnly) {
            return Translation.get(
                "folder_problem.read_only_folder_reverted", path.getFileName()
                    .toString());
        } else {
            return Translation.get(
                "folder_problem.read_only_folder", path.getFileName()
                    .toString());
        }
    }

    @Override
    public String getWikiLinkKey() {
        return null;
    }

    public Folder getFolder(final Controller controller) {
        return folder;
    }

    @Override
    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            @Override
            public void run() {
                PathUtils.openFileIfExists(path.getParent());

                folder.removeProblem(FolderReadOnlyProblem.this);
            }
        };
    }

    @Override
    public String getResolutionDescription() {
        return Translation.get("folder_problem.read_only_folder_reverted.resolution_description");
    }

}
