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
public class ReceivedAskedForFriendshipModel {

    private final ValueModel receivedAskForFriendshipCountVM = new ValueHolder();
    private List<AskForFriendshipReceivedListener> listeners;

    private List<AskForFriendshipEvent> addAskForFriendshipEvents =
            new CopyOnWriteArrayList<AskForFriendshipEvent>();

    public ReceivedAskedForFriendshipModel() {
        receivedAskForFriendshipCountVM.setValue(0);
        listeners = new CopyOnWriteArrayList<AskForFriendshipReceivedListener>();
    }

    public void addListener(AskForFriendshipReceivedListener l) {
        listeners.add(l);
    }

    public void removeListener(AskForFriendshipReceivedListener l) {
        listeners.remove(l);
    }

    /**
     * Add an notification to the model.
     *
     * @param addFriendNotification
     */
    public void addAskForFriendshipEvent(AskForFriendshipEvent askForFriendshipEvent) {
        addAskForFriendshipEvents.add(askForFriendshipEvent);
        receivedAskForFriendshipCountVM.setValue(addAskForFriendshipEvents.size());
        for (AskForFriendshipReceivedListener listener : listeners) {
            listener.notificationReceived(new AskForFriendshipReceivedEvent(this));
        }
    }

    /**
     * Remove an notification from the model for display, etc.
     *
     * @return
     */
    public AskForFriendshipEvent popNotification() {
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

    public ValueModel getReceivedAskForFriendshipCountVM() {
        return receivedAskForFriendshipCountVM;
    }
}