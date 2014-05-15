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
package de.dal33t.powerfolder.util;

/**
 * A callback interface for methods, which acts on an input stream activity. May
 * break the read in of the inputstream
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public interface StreamCallback {
    /**
     * Indicates that a position of a stream has been reached
     *
     * @param position
     *            the position in the stream
     * @param totalAvailable
     *            the total available bytes. might not be filled
     * @return if the stream read should be broken
     */
    public boolean streamPositionReached(long position, long totalAvailable);
}