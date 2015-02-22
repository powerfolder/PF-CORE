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
package de.dal33t.powerfolder.ui.action;

import javax.swing.Icon;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.event.SelectionChangeListener;
import de.dal33t.powerfolder.ui.event.SelectionModel;

/**
 * your inheriting class needs to implements those:<BR>
 * public void selectionChanged(SelectionChangeEvent e)<BR>
 * public void selectionsChanged(SelectionChangeEvent e)<BR>
 * public void actionPerformed(ActionEvent e)
 */
public abstract class SelectionBaseAction extends BaseAction implements
        SelectionChangeListener
{
    private SelectionModel selectionModel;

    protected SelectionBaseAction(String actionId, Controller controller,
        SelectionModel selectionModel)
    {
        super(actionId, controller);
        setSelectionModel(selectionModel);
    }

    protected SelectionBaseAction(String name, Icon icon,
        Controller controller, SelectionModel selectionModel)
    {
        super(name, icon, controller);
        setSelectionModel(selectionModel);
    }

    /** only set by constructor */
    private void setSelectionModel(SelectionModel selectionModel) {
        if (selectionModel == null) {
            throw new NullPointerException("Selectionmodel is null");
        }
        this.selectionModel = selectionModel;
        selectionModel.addSelectionChangeListener(this);
    }

    public SelectionModel getSelectionModel() {
        return selectionModel;
    }
}
