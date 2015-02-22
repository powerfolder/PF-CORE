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
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Container;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.friends.NodesSelectTableCellRenderer;
import de.dal33t.powerfolder.ui.util.Icons;

/**
 * Table showing selected users in invitation wizard dialog
 */
public class NodesSelectTable extends JTable {

    public NodesSelectTable(TableModel dm) {
        super(dm);
        setShowGrid(false);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setRowHeight(Icons.getIconById(Icons.NODE_CONNECTED)
            .getIconHeight() + 3);
        setDefaultRenderer(Member.class, new NodesSelectTableCellRenderer());
    }

    /**
     * Make the height same as viewport (JScrollPane) if bigger than this.
     *
     * @return
     */
    public boolean getScrollableTracksViewportHeight() {
    Container viewport = getParent();
        return viewport instanceof JViewport &&
                getPreferredSize().height < viewport.getHeight();
    }

    /**
     * Returns all the members that the user has selected.
     * @return
     */
    public Collection<Member> getSelectedMembers() {
        Collection<Member> selectedMembers = new ArrayList<Member>();
        for (int i = 0; i < getRowCount(); i++) {
            if (isRowSelected(i)) {
                Object o = getValueAt(i, 0);
                if (o instanceof Member) {
                    Member member = (Member) o;
                    selectedMembers.add(member);
                }
            }
        }
        return selectedMembers;
    }
}
