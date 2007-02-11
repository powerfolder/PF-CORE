package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * Commando to trigger the file scan on a specified folder.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ScanCommand extends FolderRelatedMessage {
    private static final long serialVersionUID = 101L;
    
    public ScanCommand(FolderInfo foInfo) {
        Reject.ifNull(foInfo, "Folder info is null");
        folder = foInfo;
    }
}
