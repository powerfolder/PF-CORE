package de.dal33t.powerfolder.event;


public interface InvitationReceivedHandler {
    /**
     * Processes a invitation to a folder TODO: Autojoin invitation, make this
     * configurable in pref screen.
     * <P>
     * 
     * @param invitationReceivedEvent containing: 
     * invitation,
     * processSilently: 
     *            if the invitation should be processed silently if already on
     *            folder (no error)
     * forcePopup: 
     *            popup application (even when minimized)
     */
    public void invitationReceived(InvitationReceivedEvent invitationRecievedEvent);
    
}
