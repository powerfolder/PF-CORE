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

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.Controller;

/**
 * Filenames like AUX (excludes the extension) are reserved system words.
 */
public class ReservedWordFilenameProblem extends ResolvableProblem {

    private final String description;
    private final FileInfo fileInfo;

    public ReservedWordFilenameProblem(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        description = Translation.get("filename_problem.reserved_filename",
                fileInfo.getFilenameOnly());
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public String getDescription() {
        return description;
    }

    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_RESERVED_WORD;
    }

    public Runnable resolution(final Controller controller) {
        return new Runnable() {
            public void run() {
                String newFilename = FilenameProblemHelper.makeUnique(
                        controller, fileInfo);
                FilenameProblemHelper.resolve(controller, fileInfo, newFilename,
                        ReservedWordFilenameProblem.this);
            }
        };
    }

    public String getResolutionDescription() {
        return Translation.get("filename_problem.reserved_filename.soln_desc");
    }

}