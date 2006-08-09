package de.dal33t.powerfolder.event;

import java.util.EventObject;

import de.dal33t.powerfolder.message.Invitation;

public class InvitationReceivedEvent extends EventObject {
    private Invitation invitation;
    private boolean processSilently;
    private boolean forcePopup;

    /**
     * @param invitation
     * @param processSilently
     *            if the invitation should be processed silently if already on
     *            folder (no error)
     * @param forcePopup
     *            popup application (even when minimized)
     */
    public InvitationReceivedEvent(Object source, Invitation invitation,
        boolean processSilently, boolean forcePopup)
    {
        super(source);
        this.invitation = invitation;
        this.processSilently = processSilently;
        this.forcePopup = forcePopup;
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
