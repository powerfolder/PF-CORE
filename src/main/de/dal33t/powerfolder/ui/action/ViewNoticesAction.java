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
 * $Id: ViewNoticesAction.java 5419 2008-09-29 12:18:20Z harry $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.model.NoticesModel;
import de.dal33t.powerfolder.ui.notices.Notice;

/**
 * This action displays system notices.
 */
public class ViewNoticesAction extends BaseAction {

    public ViewNoticesAction(Controller controller) {
        super("action_view_notices", controller);
    }

    /**
     * Display the notices handler dialog.
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        NoticesModel model = getUIController().getApplicationModel()
            .getNoticesModel();
        boolean single = (Integer) model.getUnreadNoticesCountVM().getValue() == 1;
        Notice unread;
        if (single && (unread = model.getFirstUnread()) != null) {
            model.activateNotice(unread);
        } else {
            getController().getUIController().openNoticesCard();
        }
    }
}