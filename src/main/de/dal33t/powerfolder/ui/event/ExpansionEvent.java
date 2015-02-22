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
* $Id: ExpansionEvent.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.ui.event;

import java.util.EventObject;

import de.dal33t.powerfolder.ui.ExpandableView;

/**
 * Interface for ExpansionEvents.
 * Only one view sould ever be expanded, so this interface is used to collapse
 * any other expanded views.
 */
public class ExpansionEvent extends EventObject {

    public ExpansionEvent(ExpandableView expandableView) {
        super(expandableView);
    }
}
