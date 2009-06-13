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
* $Id$
*/
package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.disk.FilenameProblemHelper;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.ScanResult;
import de.dal33t.powerfolder.disk.FilenameProblem;
import de.dal33t.powerfolder.light.FileInfo;

public class FileNameProblemEvent extends EventObject {
    private ScanResult scanResult;
    private Map<FileInfo, List<FilenameProblem>> problems;

    public FileNameProblemEvent(Folder folder, ScanResult scanResult) {
        this(folder, scanResult.getProblemFiles());
        this.scanResult = scanResult;
    }

    public FileNameProblemEvent(Folder folder,
        Map<FileInfo, List<FilenameProblem>> problems)
    {
        super(folder);
        this.problems = problems;
    }

    public ScanResult getScanResult() {
        return scanResult;
    }

    public Map<FileInfo, List<FilenameProblem>> getProblems() {
        return problems;
    }

    public Folder getFolder() {
        return (Folder) getSource();
    }
}
