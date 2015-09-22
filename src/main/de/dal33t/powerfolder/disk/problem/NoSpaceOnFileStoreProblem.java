/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;

/**
 * Raised when there is not enough space on the file store of a folder to store
 * file(s).
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class NoSpaceOnFileStoreProblem extends ResolvableProblem {

    private final FolderInfo foInfo;

    public NoSpaceOnFileStoreProblem(FolderInfo foInfo) {
        this.foInfo = foInfo;
    }

    @Override
    public String getDescription() {
        return Translation.get("problem.no_space_on_file_store");
    }

    @Override
    public String getWikiLinkKey() {
        return null;
    }

    @Override
    public int hashCode() {
        return foInfo.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NoSpaceOnFileStoreProblem)) {
            return false;
        }
        return true;
    }

    @Override
    public Runnable resolution(Controller controller) {
        return new Runnable() {

            @Override
            public void run() {
                foInfo.getFolder(controller)
                    .removeProblem(NoSpaceOnFileStoreProblem.this);
            }
        };
    }

    @Override
    public String getResolutionDescription() {
        return null;
    }
}
