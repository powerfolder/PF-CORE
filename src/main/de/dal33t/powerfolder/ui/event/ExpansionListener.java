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
* $Id: ExpandableView.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.ui.event;

import de.dal33t.powerfolder.event.CoreListener;

/**
 * Listener for ExpansionEvents.
 * There should only ever be one expanded view,
 * so implementations are to ensure that all other views are collapsed.
 * Same with being focussed.
 */
public interface ExpansionListener extends CoreListener {
    void resetAllButSource(ExpansionEvent e);
}
