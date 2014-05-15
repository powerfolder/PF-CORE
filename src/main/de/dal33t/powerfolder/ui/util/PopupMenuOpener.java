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
package de.dal33t.powerfolder.ui.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * Helper class which opens a popmenu when requested (right-mouseclick)
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class PopupMenuOpener extends MouseAdapter {
    private JPopupMenu popupMenu;

    public PopupMenuOpener(JPopupMenu popupMenu) {
        if (popupMenu == null) {
            throw new NullPointerException("Popupmenu is null");
        }
        this.popupMenu = popupMenu;
    }

    public void mousePressed(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            showContextMenu(evt);
        }
    }

    public void mouseReleased(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            showContextMenu(evt);
        }
    }

    private void showContextMenu(MouseEvent evt) {
        popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }
}