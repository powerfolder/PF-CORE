package de.dal33t.powerfolder.message;


/**
 * This message represents a small event that another peer might be interested in.
 * 
 * @author Dennis "Bytekeeper" Waldherr </a>
 * @version $Revision:$
 */
public class Notification extends Message {
	private static final long serialVersionUID = 100L;

	public static enum Event {
		ADDED_TO_FRIENDS;
	};
	
	private Event event = null;

	public Notification(Event event) {
		super();
		this.event = event;
	}
	
	/**
	 * Returns the event that occured on the remote client.
	 * @return an Notification.Event
	 */
	public Event getEvent() {
		return event;
	}
}
