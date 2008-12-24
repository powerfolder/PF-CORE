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
package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.disk.DiskItemFilter;

import javax.swing.*;

/**
 * maps the current FileInfoFilter to a ListModel.
 * <p>
 * May contain <code>null</code> as filter = empty listmodel
 *
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class DiskItemFilterPatternsListModel extends AbstractListModel {
    private DiskItemFilter filter;
    private int oldSize;

    public DiskItemFilterPatternsListModel(DiskItemFilter filter) {
        this.filter = filter;
    }

    public void setDiskItemFilter(DiskItemFilter filter) {
        this.filter = filter;
    }

    public Object getElementAt(int index) {
        return filter.getPatterns().get(index);
    }

    public int getSize() {
        if (filter == null) {
            return 0;
        }
        return filter.getPatterns().size();

    }

    /** why can't I fire a complete change? This is a hack. */
    public void fireUpdate() {
        fireContentsChanged(this, 0, oldSize + 1);
        fireContentsChanged(this, 0, filter.getPatterns().size() + 1);
        oldSize = filter.getPatterns().size();
    }
}