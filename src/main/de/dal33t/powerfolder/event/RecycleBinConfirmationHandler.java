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

/**
 * TODO maybe should return an enum with: OVERWRITE, SKIP, CANCEL,
 * OVERWRITE_ALL, SKIPP_ALL. if the event has a boolean that indicates that
 * there are more files queued to restore.
 */
public interface RecycleBinConfirmationHandler {
    public boolean confirmOverwriteOnRestore(
        RecycleBinConfirmEvent recycleBinConfirmEvent);
}
