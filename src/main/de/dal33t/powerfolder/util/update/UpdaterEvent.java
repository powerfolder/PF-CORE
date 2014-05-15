/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: Updater.java 6236 2008-12-31 15:44:10Z tot $
 */
package de.dal33t.powerfolder.util.update;

import java.net.URL;
import java.util.EventObject;

/**
 * An update event.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.27 $
 */
public class UpdaterEvent extends EventObject {
    private static final long serialVersionUID = 1L;

    private String newReleaseVersion;
    private URL newWindowsExeURL;

    public UpdaterEvent(Updater source) {
        super(source);
    }

    public UpdaterEvent(Updater source, String newReleaseVersion,
        URL newWindowsExeURL)
    {
        super(source);
        this.newReleaseVersion = newReleaseVersion;
        this.newWindowsExeURL = newWindowsExeURL;
    }

    public Updater getUpdater() {
        return (Updater) source;
    }

    public String getNewReleaseVersion() {
        return newReleaseVersion;
    }

    public URL getNewWindowsExeURL() {
        return newWindowsExeURL;
    }

}
