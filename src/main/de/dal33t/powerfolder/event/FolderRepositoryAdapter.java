/*
 * Copyright 2004 - 2017 Christian Sprajc. All rights reserved.
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
 * Adapter that implement FolderRepositoryListener, for the convenience of handling
 * {@link FolderRepositoryEvent}.
 */
public abstract class FolderRepositoryAdapter implements FolderRepositoryListener {
    @Override
    public void folderRemoved(FolderRepositoryEvent e) {
    }

    @Override
    public void folderCreated(FolderRepositoryEvent e) {
    }

    @Override
    public void folderMoved(FolderRepositoryEvent e) {
    }

    @Override
    public void maintenanceStarted(FolderRepositoryEvent e) {
    }

    @Override
    public void maintenanceFinished(FolderRepositoryEvent e) {
    }

    @Override
    public void cleanupStarted(FolderRepositoryEvent e) {
    }

    @Override
    public void cleanupFinished(FolderRepositoryEvent e) {
    }
}
