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
 * $Id: BasicSetupPanel.java 4499 2008-06-30 23:46:43Z tot $
 */
package de.dal33t.powerfolder.util;

/**
 * A general listener for task the can inform about a progress.
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public interface ProgressListener {
    /**
     * ATTENTION: Is NOT ensured to be called in Event-dispatching thread.
     *
     * @param percentageReached
     *            the percentage reached of the progress.
     */
    void progressReached(double percentageReached);
}
