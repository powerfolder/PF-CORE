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
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Translation;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class IllegalCharsFilenameProblem extends ResolvableProblem {
    private final String description;
    private final FileInfo fileInfo;
    private final String[] illegalChars;

    IllegalCharsFilenameProblem(FileInfo fileInfo, String[] illegalChars) {
        this.fileInfo = fileInfo;
        description = Translation.get("filename_problem.not_recommended_chars",
            fileInfo.getFilenameOnly());
        this.illegalChars = illegalChars;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public String getDescription() {
        return description;
    }

    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_ILLEGAL_CHARS;
    }

    public Folder getFolder(final Controller controller) {
        return fileInfo.getFolder(controller.getFolderRepository());
    }

    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                String newFilename = FileProblemHelper.removeChars(
                    fileInfo.getFilenameOnly(), illegalChars);
                FileProblemHelper.resolve(controller, fileInfo, newFilename,
                    IllegalCharsFilenameProblem.this);
            }
        };
    }

    public String getResolutionDescription() {
        return Translation.get("filename_problem.not_recommended_chars.soln_desc");
    }
}
