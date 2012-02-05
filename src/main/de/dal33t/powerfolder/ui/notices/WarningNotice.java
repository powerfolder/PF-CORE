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
 * $Id: WarningEventNotice.java 12401 2010-05-20 00:52:17Z harry $
 */
package de.dal33t.powerfolder.ui.notices;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;

/**
 * Notice to wrap a WarningEvent. Show in notification and add to app model.
 */
public class WarningNotice extends NoticeBase {

    private static final long serialVersionUID = 100L;
    private String message;

    public WarningNotice(String title, String summary, String message) {
        super(title, summary);
        this.message = message;
    }

    public Runnable getPayload(final Controller controller) {
        return new Runnable() {
            public void run() {
                DialogFactory.genericDialog(controller, getTitle(), message,
                    GenericDialogType.WARN);
            }
        };
    }

    public boolean isNotification() {
        return true;
    }

    public boolean isActionable() {
        return true;
    }

    public NoticeSeverity getNoticeSeverity() {
        return NoticeSeverity.WARINING;
    }

    public boolean isPersistable() {
        return true;
    }
}