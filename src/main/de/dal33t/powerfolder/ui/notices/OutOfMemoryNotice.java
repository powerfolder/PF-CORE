/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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
 * $Id: OutOfMemoryNotice.java 12401 2012-01-08 00:52:17Z harry $
 */
package de.dal33t.powerfolder.ui.notices;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;

/**
 * Notice of an out of memory problem.
 */
public class OutOfMemoryNotice extends NoticeBase {

    private static final long serialVersionUID = 100L;

    private OutOfMemoryError error;

    public OutOfMemoryNotice(OutOfMemoryError error) {
        super(Translation.getTranslation("out_of_memory_notice.title"),
                Translation.getTranslation("out_of_memory_notice.summary"));
        this.error = error;
    }

    public OutOfMemoryError getPayload(Controller controller) {
        return error;
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
        return false;
    }
}