/* $Id: Invitation.java,v 1.5 2005/04/21 11:53:43 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;

import java.io.File;

/**
 * A Invitation to a folder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class Invitation extends FolderRelatedMessage {

    private static final long serialVersionUID = 101L;

    private MemberInfo invitor;

    /**
     * This field is only used to allow backward compatablility with old
     * invitations.
     */
    private SyncProfile neverUsed;
    private String invitationText;
    private File suggestedLocalBase;
    private String suggestedSyncProfileConfig;

    public Invitation(FolderInfo folder, MemberInfo invitor) {
        this.folder = folder;
        this.invitor = invitor;
    }

    public void setInvitor(MemberInfo invitor) {
        this.invitor = invitor;
    }

    public void setSuggestedSyncProfile(SyncProfile suggestedSyncProfile) {
        suggestedSyncProfileConfig = suggestedSyncProfile.getFieldList();
    }

    public void setSuggestedLocalBase(File suggestedLocalBase) {
        this.suggestedLocalBase = suggestedLocalBase;
    }

    public File getSuggestedLocalBase() {
        return suggestedLocalBase;
    }

    public MemberInfo getInvitor() {
        return invitor;
    }

    public String getInvitationText() {
        return invitationText;
    }

    public void setInvitationText(String invitationText) {
        this.invitationText = invitationText;
    }

    public SyncProfile getSuggestedSyncProfile() {
        if (suggestedSyncProfileConfig == null) {
            // For backward compatibility.
            return SyncProfile.SYNCHRONIZE_PCS;
        }
        return SyncProfile
            .getSyncProfileByFieldList(suggestedSyncProfileConfig);
    }

    public String toString() {
        return "Invitation to " + folder + " from " + invitor;
    }
}