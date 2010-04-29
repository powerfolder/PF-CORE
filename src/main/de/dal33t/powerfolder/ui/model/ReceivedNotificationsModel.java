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
 * $Id: ReceivedInvitationsModel.java 5975 2008-12-14 05:23:32Z harry $
 */
package de.dal33t.powerfolder.ui.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.clientserver.Notification;

/**
 * Class to manage received {@link Notification}. Notifications can be added and
 * removed. Also a value model is available to count notification.
 */
public class ReceivedNotificationsModel extends PFComponent {

    private final ValueModel countVM = new ValueHolder();

    private List<Notification> notifications = new CopyOnWriteArrayList<Notification>();

    /**
     * Constructor
     * 
     * @param controller
     */
    public ReceivedNotificationsModel(Controller controller) {
        super(controller);
        countVM.setValue(0);
    }

    /**
     * Adds received {@link Notification} to the model
     * 
     * @param notification
     */
    public void gotInvitation(final Notification notification) {

        if (alreadyHave(notification)) {
            // Skip dupes
            return;
        }

        if (!getController().isUIOpen()) {
            return;
        }

        // Normal - add to invitation model.
        if (!alreadyHave(notification)) {
            notifications.add(notification);
            countVM.setValue(notifications.size());
        }
    }

    private boolean alreadyHave(Notification notification) {
        return notifications.contains(notification);
    }

    /**
     * Remove an notification from the model for display, etc.
     * 
     * @return an notification from the model for display, etc.
     */
    public Notification pop() {
        if (!notifications.isEmpty()) {
            Notification invitation = notifications.remove(0);
            countVM.setValue(notifications.size());
            return invitation;
        }
        return null;
    }

    /**
     * @return Value model with integer count of received notifications.
     */
    public ValueModel getCountVM() {
        return countVM;
    }
}
