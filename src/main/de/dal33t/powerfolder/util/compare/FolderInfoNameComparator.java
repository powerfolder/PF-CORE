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
 */
package de.dal33t.powerfolder.util.compare;

import java.util.Comparator;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.logging.Loggable;

public final class FolderInfoNameComparator extends Loggable implements
    Comparator<FolderInfo> {
    public static final FolderInfoNameComparator INSTANCE = new FolderInfoNameComparator();

    private FolderInfoNameComparator() {
    }

    public int compare(final FolderInfo o1, final FolderInfo o2) {
        if (o1.getName() == null) {
            return -1;
        }
        if (o2.getName() == null) {
            return 1;
        }

        final int comparison = o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        if (comparison == 0) {
            return o1.getId().compareTo(o2.getId());
        }
        return comparison;
    }
}
