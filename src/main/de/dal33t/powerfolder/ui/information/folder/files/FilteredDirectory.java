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
package de.dal33t.powerfolder.ui.information.folder.files;

public class FilteredDirectory implements Comparable<FilteredDirectory> {

    private final String displayName;
    private final String relativeName;

    public FilteredDirectory(String displayName, String relativeName) {
        this.displayName = displayName;
        this.relativeName = relativeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRelativeName() {
        return relativeName;
    }

    public int compareTo(FilteredDirectory o) {
        return getDisplayName().compareToIgnoreCase(o.getDisplayName());
    }
}
