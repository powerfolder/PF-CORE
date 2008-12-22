/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id: ReceivedAskedForFriendshipModel.java 5975 2008-12-14 05:23:32Z harry $
 */
package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.event.AskForFriendshipReceivedListener;
import de.dal33t.powerfolder.event.AskForFriendshipReceivedEvent;
import de.dal33t.powerfolder.event.AskForFriendshipEvent;
import de.dal33t.powerfolder.event.AskForFriendshipListener;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;

/**
 * Class to manage received notifications. Notifications can be added and removed.
 * Also a value model is available to count notifications.
 * Listeners can be added to be notified of changes to the number of notifications
 * in the model.
 */
public class ReceivedAskedForFriendshipModel extends PFComponent implements AskForFriendshipListener {

    private final ValueModel receivedAskForFriendshipCountVM = new ValueHolder();
    private List<AskForFriendshipReceivedListener> listeners;

    private List<AskForFriendshipEvent> addAskForFriendshipEvents =
            new CopyOnWriteArrayList<AskForFriendshipEvent>();

    /**
     * Constructor
     *
     * @param controller
     */
    public ReceivedAskedForFriendshipModel(Controller controller) {
        super(controller);
        receivedAskForFriendshipCountVM.setValue(0);
        listeners = new CopyOnWriteArrayList<AskForFriendshipReceivedListener>();
        controller.addAskForFriendshipListener(this);
    }

    /**
     * Add a listener
     *
     * @param l
     */
    public void addListener(AskForFriendshipReceivedListener l) {
        listeners.add(l);
    }

    /**
     * Remove a listener
     *
     * @param l
     */
    public void removeListener(AskForFriendshipReceivedListener l) {
        listeners.remove(l);
    }

    /**
     * Ask for friendship event.
     *
     * @param event
     */
    public void askForFriendship(AskForFriendshipEvent event) {

        Member node = getController().getNodeManager()
                .getNode(event.getMemberInfo());
        if (node == null) {
            // Ignore friendship request from unknown node.
            return;
        }

        // FIXME Does not work with temporary server nodes.
        if (node.isServer() || getController().getOSClient().isServer(node))
        {
            // TRAC #1190
            node.setFriend(true, null);
            return;
        }

        boolean askForFriendship =
                PreferencesEntry.ASK_FOR_FRIENDSHIP_ON_PRIVATE_FOLDER_JOIN
            .getValueBoolean(getController());

        if (getController().isUIOpen() && !node.isFriend()
            && askForFriendship && !node.askedForFriendship()) {

            // Okay node is asking for friendship now.
            node.setAskedForFriendship(true);

            addAskForFriendshipEvents.add(event);
            receivedAskForFriendshipCountVM.setValue(addAskForFriendshipEvents.size());
            for (AskForFriendshipReceivedListener listener : listeners) {
                listener.notificationReceived(new AskForFriendshipReceivedEvent(this));
            }
        }
    }

    /**
     * Remove an notification from the model for display, etc.
     *
     * @return
     */
    public AskForFriendshipEvent popEvent() {
        if (!addAskForFriendshipEvents.isEmpty()) {
            AskForFriendshipEvent askForFriendshipEvent =
                    addAskForFriendshipEvents.remove(0);
            receivedAskForFriendshipCountVM.setValue(
                    addAskForFriendshipEvents.size());
            for (AskForFriendshipReceivedListener listener : listeners) {
                listener.notificationReceived(
                        new AskForFriendshipReceivedEvent(this));
            }
            return askForFriendshipEvent;
        }
        return null;
    }

    /**
     * Value model with integer count of received friendship requests.
     *
     * @return
     */
    public ValueModel getReceivedAskForFriendshipCountVM() {
        return receivedAskForFriendshipCountVM;
    }
}