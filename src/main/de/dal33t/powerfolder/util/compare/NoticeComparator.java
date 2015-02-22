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
 * $Id$
 */
package de.dal33t.powerfolder.util.compare;

import java.util.Comparator;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ui.notices.Notice;

/**
 * Comparator for members
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class NoticeComparator implements Comparator<Notice> {

    private static final Logger LOG = Logger.getLogger(NoticeComparator.class
        .getName());
    public static final NoticeComparator BY_DATE = new NoticeComparator(0);
    public static final NoticeComparator BY_SEVERITY = new NoticeComparator(1);
    public static final NoticeComparator BY_TITLE = new NoticeComparator(2);
    public static final NoticeComparator BY_SUMMARY = new NoticeComparator(3);

    private int type;

    private NoticeComparator(int type) {
        this.type = type;
    }

    public int compare(Notice o1, Notice o2) {
        if (type == 0) {
            // Sort by date
            return o1.getDate().compareTo(o2.getDate());
        } else if (type == 1) {
            // Sort by severity
            return o1.getNoticeSeverity().compareTo(o2.getNoticeSeverity());
        } else if (type == 2) {
            // Sort by title
            return o1.getTitle().compareTo(o2.getTitle());
        } else if (type == 3) {
            return o1.getSummary().compareTo(o2.getSummary());
        } else {
            LOG.severe("Unknow comparing type: " + type);
            return 0;
        }
    }
}