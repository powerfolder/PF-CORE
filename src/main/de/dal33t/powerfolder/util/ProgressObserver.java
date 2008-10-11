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
 * $Id: AbstractDownloadManager.java 5151 2008-09-04 21:50:35Z bytekeeper $
 */
package de.dal33t.powerfolder.util;

/**
 * Utility class providing progress updates. Using PropertyChangeListeners etc.
 * just didn't look good. While this class might need some refining, it's a lot
 * easier to use.
 * 
 * @author Dennis "Bytekeeper" Waldherr
 */
public interface ProgressObserver {
    /**
     * Invoked after progress on a computation has been made.
     * 
     * @param percent
     */
    void progressed(double percent);
}
