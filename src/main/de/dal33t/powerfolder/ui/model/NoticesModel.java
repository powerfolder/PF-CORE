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
import de.dal33t.powerfolder.ui.notices.Notice;

import java.util.List;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Model of the notices awaiting action by the user.
 */
public class NoticesModel extends PFComponent {

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

    public void addNotice(Notice notice) {
        notices.add(notice);
        receivedNoticesCountVM.setValue(notices.size());
    }

    /**
     * Remove a notice from the model for display, etc.
     *
     * @return
     */
    public Notice popNotice() {
        if (!notices.isEmpty()) {
            Notice notice = notices.remove(0);
            receivedNoticesCountVM.setValue(
                    notices.size());
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
