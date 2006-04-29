/* $Id: SettingsChange.java,v 1.1 2004/09/29 01:34:11 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Tells that nick has changed
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.1 $
 */
public class SettingsChange extends Message {
	private static final long serialVersionUID = 100L;
	
	public MemberInfo newInfo;
	
	public SettingsChange(Member member)  {
		if (member == null) {
			throw new NullPointerException("Member is null");
		}
		newInfo = member.getInfo();
	}
	
	public String toString() {
		return "Settings changed to '" + newInfo.nick + "'";
	}
}
