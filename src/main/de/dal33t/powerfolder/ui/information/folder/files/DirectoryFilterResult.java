/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: DirectoryFilter.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.files;

import java.util.concurrent.atomic.AtomicLong;

public class DirectoryFilterResult {
    private AtomicLong filteredFileCount = new AtomicLong();
    private AtomicLong originalFileCount = new AtomicLong();
    private AtomicLong deletedCount = new AtomicLong();
    private AtomicLong incomingCount = new AtomicLong();
    private AtomicLong localCount = new AtomicLong();

    public AtomicLong getFilteredCount() {
        return filteredFileCount;
    }

    public AtomicLong getOriginalCount() {
        return originalFileCount;
    }

    public AtomicLong getDeletedCount() {
        return deletedCount;
    }

    public AtomicLong getIncomingCount() {
        return incomingCount;
    }

    public AtomicLong getLocalCount() {
        return localCount;
    }

}
