package de.dal33t.powerfolder.event;

/**
 * The interface that should be implemented when writing a handler for
 * NodeManager called when a member joins a folder. The handler will generely
 * ask if that member should become a friend
 * 
 * @version $Revision: 1.5 $
 */
public interface AskForFriendshipHandler {
    public void askForFriendship(
        AskForFriendshipHandlerEvent askForFriendshipHandlerEvent);
}
