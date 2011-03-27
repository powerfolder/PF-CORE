/*
 * Copyright 2004 - 2011 Christian Sprajc. All rights reserved.
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
 * $Id: NewFolderCandidateEvent.java 8169 2011-03-27 11:57:40Z harry $
 */
package de.dal33t.powerfolder.event;

import java.io.File;

public class NewFolderCandidateEvent {

    private File directory;

    public NewFolderCandidateEvent(File directory) {
        this.directory = directory;
    }

    public File getDirectory() {
        return directory;
    }
}
