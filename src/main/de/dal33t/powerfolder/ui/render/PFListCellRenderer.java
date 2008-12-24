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
package de.dal33t.powerfolder.ui.render;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;

/**
 * A Default list cell renderer for several powerfolder elements
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class PFListCellRenderer extends DefaultListCellRenderer {

    public PFListCellRenderer() {
        super();
    }

    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus)
    {
        super.getListCellRendererComponent(list, value, index, isSelected,
            cellHasFocus);

        if (value instanceof Member) {
            Member node = (Member) value;

            // Get icon
            setIcon(Icons.getIconFor(node));

            // General stuff (text)
            String text = node.getNick();
           
            if (node.isOnLAN()) {
                text += " (" + Translation.getTranslation("general.localnet") + ')';
            }
            setText(text);
        } else if (value instanceof SyncProfile) {
            // Sync profile
            SyncProfile syncProfile = (SyncProfile) value;
            String text = syncProfile.getProfileName();
            setText(text);
        }

        return this;
    }
}