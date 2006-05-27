/* $Id: FolderRelatedMessage.java,v 1.2 2004/09/24 03:37:46 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FolderInfo;

/**
 * Basic message which is related to a folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public abstract class FolderRelatedMessage extends Message {
	private static final long serialVersionUID = 100L;
	
	public FolderInfo folder;
}
