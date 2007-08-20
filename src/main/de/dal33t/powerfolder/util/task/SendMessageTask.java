package de.dal33t.powerfolder.util.task;

import java.util.Calendar;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.message.Message;

/**
 * This task tries to send a message to another user.
 * The task remains active even when PF is closed and will try to
 * perform it's work on the next PF session.
 * 
 * @author Dennis "Bytekeeper" Waldherr </a>
 * @version $Revision:$
 */
public class SendMessageTask extends PersistentTask {
	private static final int EXPIRATION_DAYS = 14;
	private static final long serialVersionUID = 1L;
	private Message message;
	private String targetID;
	private Calendar expires;
	
	private transient NodeManagerListener listener; 
	
	public SendMessageTask(Message message, String targetID) {
		this.message = message;
		this.targetID = targetID;
		expires = Calendar.getInstance();
		expires.add(Calendar.DAY_OF_MONTH, EXPIRATION_DAYS);
	}
	
	@Override
	public void init(PersistentTaskManager handler) {
		super.init(handler);
		if (message == null || targetID == null || expires == null || isExpired()) {
			remove();
		} else {
			listener = new MessageTrigger();
			// Try to execute the task immediately
			if (!execute()) {
				getController().getNodeManager().addNodeManagerListener(listener);
				Member node = getController().getNodeManager().getNode(targetID); 
				if (node != null) {
					node.markForImmediateConnect();
				}
			}
		}
	}

	private boolean execute() {
		Member node = getController().getNodeManager().getNode(targetID);
		if (node != null && node.isCompleteyConnected()) {
			node.sendMessageAsynchron(
					message, "Failed to send " + message);
			remove();
			return true;
		}
		return false;
	}

	@Override
	public void shutdown() {
		try {
			if (getController() != null && getController().getNodeManager() != null && listener != null) {
				getController().getNodeManager().removeNodeManagerListener(listener);
			}
		} finally {
			super.shutdown();
		}
	}

	/**
	 * @return this task's message.
	 */
	public Message getMessage() {
		return message;
	}

	/**
	 * @return the target user who should receive the message.
	 */
	public String getTargetID() {
		return targetID;
	}
	
	private boolean isExpired() {
		return expires != null && Calendar.getInstance().compareTo(expires) >= 0;
	}
	
	@Override
	public String toString() {
		return "SendMessageTask trying to send " + message + " to " + targetID + " until " + expires;
	}

	private class MessageTrigger implements NodeManagerListener {
		public void friendAdded(NodeManagerEvent e) { }
		public void friendRemoved(NodeManagerEvent e) { }

		public void nodeAdded(NodeManagerEvent e) { }

		public void nodeConnected(NodeManagerEvent e) {
			if (e.getNode().getId().equals(targetID)) {
				execute();
			}
		}
		
		public void nodeDisconnected(NodeManagerEvent e) { }

		public void nodeRemoved(NodeManagerEvent e) { }

		public void settingsChanged(NodeManagerEvent e) { }

        public void startStop(NodeManagerEvent e) {
        }

		public boolean fireInEventDispathThread() { 
			return false;
		}
	}
}
