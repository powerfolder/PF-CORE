package de.dal33t.powerfolder.util.task;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;

/**
 * This task tries to send an invitation to another user.
 * The task remains active even when PF is closed and will try to
 * perform it's work on the next PF session.
 * 
 * @author Dennis "Bytekeeper" Waldherr </a>
 * @version $Revision$
 */
public class SendInvitationTask extends PersistentTask {
	private static final long serialVersionUID = 1L;
	private Invitation invitation;
	private MemberInfo target;
	
	private transient NodeManagerListener listener = 
		new NodeManagerListener() {
			public void friendAdded(NodeManagerEvent e) { }
			public void friendRemoved(NodeManagerEvent e) { }

			public void nodeAdded(NodeManagerEvent e) { }

			public void nodeConnected(NodeManagerEvent e) {
				if (target.matches(e.getNode())) {
					execute();
				}
			}
			
			public void nodeDisconnected(NodeManagerEvent e) { }

			public void nodeRemoved(NodeManagerEvent e) { }

			public void settingsChanged(NodeManagerEvent e) { }

			public boolean fireInEventDispathThread() { 
				return false;
			}
		
	};
	

	public SendInvitationTask(Invitation invitation, MemberInfo target) {
		this.invitation = invitation;
		this.target = target;
	}
	
	@Override
	public void init(PersistentTaskManager handler) {
		super.init(handler);
		// Try to execute the task immediatly
		if (!execute()) {
			getController().getNodeManager().addNodeManagerListener(listener);
			target.getNode(getController()).markForImmediateConnect();
		}
	}

	private boolean execute() {
		Member node = target.getNode(getController());
		if (node.isCompleteyConnected()) {
			node.sendMessageAsynchron(
					invitation, "Failed to send invitation!");
			remove();
			return true;
		}
		return false;
	}

	@Override
	public void shutdown() {
		if (getController() != null && getController().getNodeManager() != null) {
			getController().getNodeManager().removeNodeManagerListener(listener);
		}
		super.shutdown();
	}

	/**
	 * Returns this task's invititation message.
	 * @return
	 */
	public Invitation getInvitation() {
		return invitation;
	}

	/**
	 * Gets the the target user who should receive the invitation.
	 * @return
	 */
	public MemberInfo getTarget() {
		return target;
	}
}
