/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder.task;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.message.Message;

/**
 * This task tries to send a message to another user. The task remains active
 * even when PF is closed and will try to perform it's work on the next PF
 * session.
 *
 * @author Dennis "Bytekeeper" Waldherr </a>
 * @version $Revision:$
 */
public class SendMessageTask extends PersistentTask {
    private static final long serialVersionUID = 1L;
    private Message message;
    private String targetID;

    private transient NodeManagerListener listener;

    public SendMessageTask(Message message, String targetID) {
        super(DEFAULT_DAYS_TO_EXIPRE);
        this.message = message;
        this.targetID = targetID;
    }

    @Override
    public void initialize() {
        if (message == null || targetID == null || isExpired()) {
            remove();
            return;
        }
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

    private boolean execute() {
        Member node = getController().getNodeManager().getNode(targetID);
        if (node != null && node.isCompletelyConnected()) {
            node.sendMessageAsynchron(message);
            remove();
            return true;
        }
        return false;
    }

    @Override
    public void shutdown() {
        try {
            if (getController() != null
                && getController().getNodeManager() != null && listener != null)
            {
                getController().getNodeManager().removeNodeManagerListener(
                    listener);
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

    @Override
    public String toString() {
        return "SendMessageTask trying to send " + message + " to " + targetID
            + " until " + getExpires().getTime();
    }

    private class MessageTrigger extends NodeManagerAdapter {

        public void nodeConnected(NodeManagerEvent e) {
            if (e.getNode().getId().equals(targetID)) {
                execute();
            }
        }

        public boolean fireInEventDispatchThread() {
            return false;
        }
    }
}
