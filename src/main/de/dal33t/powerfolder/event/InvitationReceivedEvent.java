package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.Reject;

/**
 * Event which gets fired to <code>InvitationReceivedHandler</code> that is
 * listening to the <code>FolderRepository</code>.
 * 
 * @see de.dal33t.powerfolder.event.InvitationReceivedHandler
 * @see de.dal33t.powerfolder.disk.FolderRepository
 * @see de.dal33t.powerfolder.ui.InvitationReceivedHandlerDefaultImpl
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class InvitationReceivedEvent extends EventObject {
    private Invitation invitation;
    private boolean processSilently;
    private boolean forcePopup;

    /**
     * @param source
     *            the source folder repo
     * @param invitation
     * @param processSilently
     *            if the invitation should be processed silently if already on
     *            folder (no error)
     * @param forcePopup
     *            popup application (even when minimized)
     */
    public InvitationReceivedEvent(FolderRepository source,
        Invitation invitation, boolean processSilently, boolean forcePopup)
    {
        super(source);
        Reject.ifNull(source, "Folder Repository is null");
        this.invitation = invitation;
        this.processSilently = processSilently;
        this.forcePopup = forcePopup;
    }

    public FolderRepository getFolderRepository() {
        return (FolderRepository) getSource();
    }

    public boolean isForcePopup() {
        return forcePopup;
    }

    public Invitation getInvitation() {
        return invitation;
    }

    public boolean isProcessSilently() {
        return processSilently;
    }
}
