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
package de.dal33t.powerfolder.util.os.Win32;

import de.dal33t.powerfolder.util.Reject;

/**
 * Representation of a windows shell link file.
 *
 * @author <A HREF="mailto:bytekeeper@powerfolder.com">Dennis Waldherr</A>
 * @version $Revision$
 *
 */
public class ShellLink {
    /*
     * WARNING: Don't change the names of these variables, they're accessed directly by native code!
     */
	private final String arguments;
	private final String description;
	private final String path;
	private final String workdir;

	/**
	 * Creates a wrapper for IShellLink in windows.
	 * @param arguments contains the arguments, may be null
	 * @param description contains a description, may be null
	 * @param path contains the target path
	 * @param workdir contains the working directory, may be null
	 */
	public ShellLink(String arguments, String description, String path,
        String workdir)
    {
        Reject.ifNull(path, "Path (target) not set!");
        this.arguments = arguments;
        this.description = description;
        this.path = path;
        this.workdir = workdir;
    }

	public ShellLink(String arguments, String path, String workdir) {
	    this(arguments, null, path, workdir);
	}

    public String getArguments() {
        return arguments;
    }

    public String getDescription() {
        return description;
    }

    public String getPath() {
        return path;
    }

    public String getWorkdir() {
        return workdir;
    }
}
