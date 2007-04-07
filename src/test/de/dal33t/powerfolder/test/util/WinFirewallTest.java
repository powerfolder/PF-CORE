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
