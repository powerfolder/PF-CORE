/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.notices;

import de.dal33t.powerfolder.Controller;

/**
 * Notice to wrap a WarningEvent. Show in notification and add to app model.
 */
public class RunnableNotice extends NoticeBase {

    private static final long serialVersionUID = 100L;
    private Runnable runnable;
    private NoticeSeverity severity;

    public RunnableNotice(String title, String summary, Runnable runnable, NoticeSeverity severity) {
        super(title, summary);
        this.runnable = runnable;
        this.severity = severity;
    }

    public Runnable getPayload(Controller controller) {
        return runnable;
    }

    public boolean isNotification() {
        return true;
    }

    public boolean isActionable() {
        return true;
    }

    public NoticeSeverity getNoticeSeverity() {
        return severity;
    }

    /**
     * Can't persist because of runnable.
     *
     * @return
     */
    public boolean isPersistable() {
        return false;
    }
}
