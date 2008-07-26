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
package de.dal33t.powerfolder.ui.folder;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;

/**
 * shows a dialog to solve problems with filenames. may edit the scan result
 * before commit.
 */
public class FileNameProblemHandlerDefaultImpl extends PFUIComponent implements
    FileNameProblemHandler
{
    private Lock dialogOpenLock = new ReentrantLock(); 
    
    public FileNameProblemHandlerDefaultImpl(Controller controller) {
        super(controller);
    }

    public void fileNameProblemsDetected(
        FileNameProblemEvent fileNameProblemEvent)
    {
        if (isLogFine()) {
            logFine(
                fileNameProblemEvent.getFolder() + " "
                    + fileNameProblemEvent.getProblems().size()
                    + " problematic files");
        }

        if (PreferencesEntry.FILE_NAME_CHECK.getValueBoolean(getController())) {
            dialogOpenLock.lock();
            FilenameProblemDialog dialog = new FilenameProblemDialog(
                getController(), fileNameProblemEvent);
            // Modal dialog causes wait here
            dialog.open();
            if (dialog.getOption() == FilenameProblemDialog.OK
                && !dialog.askAgain())
            {
                PreferencesEntry.FILE_NAME_CHECK.setValue(getController(),
                    false);
            }
            dialogOpenLock.unlock();
        }
    }
}