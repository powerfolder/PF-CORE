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
 * $Id: WarningEvent.java 5975 2008-12-14 05:23:32Z harry $
 */
package de.dal33t.powerfolder.event;

/**
 * This class encapsulates a warning in the form of a Runnable. The
 * Runnable should advise the user of the problem and take action.
 */
public class WarningEvent {

    private final String name;
    private final Runnable runnable;

    public WarningEvent(String name, Runnable runnable) {
        this.name = name;
        this.runnable = runnable;
    }

    public String getName() {
        return name;
    }

    public Runnable getRunnable() {
        return runnable;
    }
}
