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
package de.dal33t.powerfolder.ui.friends;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

/**
 * Class to render a selected user.
 */
public class NodesSelectTableCellRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column)
    {
        setHorizontalAlignment(SwingConstants.LEFT);
        setIcon(null);
        Object myValue = null;

        // Show user nick and icon
        if (value instanceof Member) {
            Member member = (Member) value;
            switch (column) {
                case 0 :
                    String text = member.getNick();
                    if (member.isOnLAN()) {
                        text += " ("
                            + Translation.getTranslation("general.localnet")
                            + ")";
                    }
                    myValue = text;
                    setIcon(Icons.getIconFor(member));
                    break;
            }
        } else if (value instanceof String) {
            // Just print text (no users)
            myValue = value;
        } else {
            throw new IllegalStateException(
                "Don't know how to render " + value == null ? "null" : value
                    .getClass().getName());
        }

        return super.getTableCellRendererComponent(table, myValue, isSelected,
            hasFocus, row, column);
    }
}
