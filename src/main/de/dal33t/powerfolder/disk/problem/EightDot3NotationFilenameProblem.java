/*
 * Copyright 2004 - 2022 Christian Sprajc, dal33t GmbH. All rights reserved.
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
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.Translation;

/**
 * PFC-3317
 */
public class EightDot3NotationFilenameProblem extends Problem {
    private final String description;
    private final FileInfo fileInfo;

    public EightDot3NotationFilenameProblem(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        description = Translation.get("filename_problem.8dot3notation",
                fileInfo.getFilenameOnly());
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public String getDescription() {
        return description;
    }

    public String getWikiLinkKey() {
        return null;
    }

    public Folder getFolder(final Controller controller) {
        return fileInfo.getFolder(controller.getFolderRepository());
    }

    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                Folder folder = fileInfo.getFolderInfo().getFolder(controller);
                if (folder != null) {
                    controller.getFolderRepository().removeFolder(folder, false);
                }
            }
        };
    }

    public String getResolutionDescription() {
        return Translation.get("filename_problem.8dot3notation.soln_desc");
    }

    @Override
    public String toString() {
        return "EightDot3NotationFilenameProblem{" +

                "fileInfo=" + fileInfo +
                ",description='" + description + '\'' +
                '}';
    }
}
