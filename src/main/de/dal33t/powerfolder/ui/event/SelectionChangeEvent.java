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
package de.dal33t.powerfolder.ui.event;

import java.util.EventObject;

/**
 * Event fired if a selection has changed in the SelectionModel
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.4 $
 */
public class SelectionChangeEvent extends EventObject {

    private Object[] selections;

    /** package protected. Only created by SelectionModel */
    SelectionChangeEvent(Object source, Object[] selections) {
        super(source);
        this.selections = selections;
    }

    /** returns the new selection (maybe null) */
    public Object getSelection() {
        if (selections == null || selections.length == 0) {
            return null;
        }
        return selections[0];
    }

    /** Returns the new list of selections (maybe null, or items maybe null) */
    public Object[] getSelections() {
        return selections;
    }
}
