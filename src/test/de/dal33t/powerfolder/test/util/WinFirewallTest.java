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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.test.util;

import java.io.IOException;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.FirewallUtil;

public class WinFirewallTest extends TestCase {
	public void testFirewallAPI() throws IOException {
		// Always passes on non Windows systems
		if (!OSUtil.isWindowsSystem() && !OSUtil.isWindowsMEorOlder())
			return;

		FirewallUtil.openport(1337);
		FirewallUtil.changeport(1337, 1338);
		FirewallUtil.closeport(1338);
	}

	public void testMultiopen() throws IOException {
		// Always passes on non Windows systems
		if (!OSUtil.isWindowsSystem() && !OSUtil.isWindowsMEorOlder())
			return;
		FirewallUtil.openport(1337);
		FirewallUtil.openport(1337);
	}

	public void testMulticlose() throws IOException {
		// Always passes on non Windows systems
		if (!OSUtil.isWindowsSystem() && !OSUtil.isWindowsMEorOlder())
			return;
		FirewallUtil.closeport(1337);
		FirewallUtil.closeport(1337);
	}

}
