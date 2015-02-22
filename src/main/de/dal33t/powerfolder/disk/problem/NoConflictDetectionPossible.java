/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
 * $Id: $
 */
package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class NoConflictDetectionPossible extends ResolvableProblem {
    private final String desc;
    private final FileInfo fileInfo;

    public NoConflictDetectionPossible(FileInfo fileInfo, MemberInfo oldSource)
    {
        Reject.notNull(fileInfo, "fileInfo");
        Reject.notNull(oldSource, "oldSource");
        this.fileInfo = fileInfo;
        desc = Translation.get("folder_problem.noconflictdetection",
            fileInfo.getRelativeName(), oldSource.nick);
    }

    @Override
    public String getResolutionDescription() {
        return null;
    }

    @Override
    public Runnable resolution(Controller controller) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_NO_CONFLICT_DETECTION_POSSIBLE;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != getClass()) {
            return false;
        }
        NoConflictDetectionPossible other = (NoConflictDetectionPossible) obj;
        return other.fileInfo.equals(fileInfo);
    }

    @Override
    public int hashCode() {
        return 37 + fileInfo.hashCode();
    }
}
