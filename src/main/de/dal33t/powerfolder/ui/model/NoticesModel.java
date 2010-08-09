/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: NoticesModel.java 12401 2010-05-20 00:52:17Z harry $
 */
package de.dal33t.powerfolder.ui.model;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.notices.InvitationNotice;
import de.dal33t.powerfolder.ui.notices.Notice;
import de.dal33t.powerfolder.ui.notification.NotificationHandler;
import de.dal33t.powerfolder.util.Visitor;

import java.util.List;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Model of the notices awaiting action by the user.
 */
public class NoticesModel extends PFUIComponent {

    private final ValueModel receivedNoticesCountVM = new ValueHolder();

    private List<Notice> notices = new CopyOnWriteArrayList<Notice>();

    /**
     * Constructor
     * 
     * @param controller
     */
    public NoticesModel(Controller controller) {
        super(controller);
        receivedNoticesCountVM.setValue(0);
    }

    /**
     * Value model with integer count of received invitations.
     * 
     * @return
     */
    public ValueModel getReceivedNoticesCountVM() {
        return receivedNoticesCountVM;
    }

    public boolean addNotice(Notice notice) {
        if (notices.contains(notice)) {
            logFine("Ignoring existing notice: " + notice);
            return false;
        }
        notices.add(notice);
        receivedNoticesCountVM.setValue(notices.size());
        return true;
    }

    /**
     * Remove a notice from the model for display, etc.
     * 
     * @return
     */
    public Notice popNotice() {
        if (!notices.isEmpty()) {
            Notice notice = notices.remove(0);
            receivedNoticesCountVM.setValue(notices.size());
            return notice;
        }
        return null;
    }

    /**
     * Return a reference list of the notices in the model.
     * 
     * @return
     */
    public List<Notice> getAllNotices() {
        return Collections.unmodifiableList(notices);
    }

    /**
     * This handles a notice object. If it is a notification, show in a
     * notification handler. If it is actionable, add to the app model notices.
     * 
     * @param notice
     *            the Notice to handle
     */
    public void handleNotice(Notice notice) {
        if (!getUIController().isStarted() || getController().isShuttingDown())
        {
            return;
        }

        if ((Boolean) getApplicationModel().getSystemNotificationsValueModel()
            .getValue()
            && notice.isNotification())
        {
            NotificationHandler notificationHandler = new NotificationHandler(
                getController(), notice.getTitle(), notice.getSummary(), true);
            notificationHandler.show();
        }

        if (notice.isActionable()) {
            // Invitations are a special case. We do not care about
            // invitations to folders that we have already joined.
            if (notice instanceof InvitationNotice) {
                InvitationNotice in = (InvitationNotice) notice;
                Invitation i = in.getPayload();
                FolderInfo fi = i.folder;
                if (!getController().getFolderRepository().hasJoinedFolder(fi))
                {
                    addNotice(notice);
                }
            } else {
                addNotice(notice);
            }
        }
    }

    public void clearAll() {
        while (!notices.isEmpty()) {
            popNotice();
        }
    }

    public void clearNotice(Notice notice) {
        for (Notice n : notices) {
            if (notice.equals(n)) {
                notices.remove(notice);
                receivedNoticesCountVM.setValue(notices.size());
                return;
            }
        }
    }
}
