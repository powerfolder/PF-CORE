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
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.dialog.FileNameProblemDialog;
import de.dal33t.powerfolder.event.FileNameProblemEvent;
import de.dal33t.powerfolder.event.FileNameProblemHandler;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * shows a dialog to solve problems with filenames. may edit the scan result
 * before commit.
 */
public class FileNameProblemHandlerDefaultImpl extends PFUIComponent implements
    FileNameProblemHandler
{

    private static final Logger log = Logger.getLogger(FileNameProblemHandlerDefaultImpl.class.getName());

    private Lock dialogOpenLock = new ReentrantLock(); 
    
    public FileNameProblemHandlerDefaultImpl(Controller controller) {
        super(controller);
    }

    public void fileNameProblemsDetected(
        FileNameProblemEvent fileNameProblemEvent)
    {
        if (log.isLoggable(Level.FINE)) {
            logFine(
                fileNameProblemEvent.getFolder() + " "
                    + fileNameProblemEvent.getProblems().size()
                    + " problematic files");
        }

        if (PreferencesEntry.FILE_NAME_CHECK.getValueBoolean(getController())) {
            dialogOpenLock.lock();
            FileNameProblemDialog dialog = new FileNameProblemDialog(
                getController(), fileNameProblemEvent);
            // Modal dialog causes wait here
            dialog.open();
            if (dialog.getOption() == FileNameProblemDialog.OK
                && !dialog.askAgain())
            {
                PreferencesEntry.FILE_NAME_CHECK.setValue(getController(),
                    false);
            }
            dialogOpenLock.unlock();
        }
    }
}