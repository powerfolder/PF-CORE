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
 * $Id: ReceivedAddFriendNotificationsModel.java 5975 2008-12-14 05:23:32Z harry $
 */
package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.message.AddFriendNotification;
import de.dal33t.powerfolder.event.AddFriendNotificationReceivedListener;
import de.dal33t.powerfolder.event.AddFriendNotificationReceivedEvent;

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
public class ReceivedAddFriendNotificationsModel {

    private final ValueModel receivedAddFriendNotificationsCountVM = new ValueHolder();
    private List<AddFriendNotificationReceivedListener> listeners;

    private List<AddFriendNotification> addFriendNotifications =
            new CopyOnWriteArrayList<AddFriendNotification>();

    public ReceivedAddFriendNotificationsModel() {
        receivedAddFriendNotificationsCountVM.setValue(0);
        listeners =
                new CopyOnWriteArrayList<AddFriendNotificationReceivedListener>();
    }

    public void addNotificationReceivedListener(AddFriendNotificationReceivedListener l) {
        listeners.add(l);
    }

    public void removeNotificationReceivedListener(AddFriendNotificationReceivedListener l) {
        listeners.remove(l);
    }

    /**
     * Add an notification to the model.
     *
     * @param addFriendNotification
     */
    public void addNotification(AddFriendNotification addFriendNotification) {
        addFriendNotifications.add(addFriendNotification);
        receivedAddFriendNotificationsCountVM.setValue(addFriendNotifications.size());
        for (AddFriendNotificationReceivedListener listener
                : listeners) {
            listener.notificationReceived(
                    new AddFriendNotificationReceivedEvent(this));
        }
    }

    /**
     * Remove an notification from the model for display, etc.
     *
     * @return
     */
    public AddFriendNotification popNotification() {
        if (!addFriendNotifications.isEmpty()) {
            AddFriendNotification addFriendNotification =
                    addFriendNotifications.remove(0);
            receivedAddFriendNotificationsCountVM.setValue(
                    addFriendNotifications.size());
            for (AddFriendNotificationReceivedListener
                    addFriendNotificationReceivedListener
                    : listeners) {
                addFriendNotificationReceivedListener.notificationReceived(
                        new AddFriendNotificationReceivedEvent(this));
            }
            return addFriendNotification;
        }
        return null;
    }

    public ValueModel getReceivedAddFriendNotificationsCountVM() {
        return receivedAddFriendNotificationsCountVM;
    }
}