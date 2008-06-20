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
package de.dal33t.powerfolder.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TimerTask;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

/**
 * abstract class with commons for filtering, holds a searchField (ValueModel)
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public abstract class FilterModel extends PFComponent {
    /** the delay to use to give fast typers to complete their words */
    private static final long DELAY = 500;
    /** The task that performs the filtering */
    private TimerTask task = null;

    /** The value model of the searchfield we listen to */
    private ValueModel searchField;

    public FilterModel(Controller controller) {
        super(controller);
        setSearchField(new ValueHolder());
    }

    public abstract void reset();

    public abstract void scheduleFiltering();

    public ValueModel getSearchField() {
        return searchField;
    }

    public void setSearchField(ValueModel searchField) {
        this.searchField = searchField;
        searchField.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (task != null) {
                    task.cancel(); // cancel if seachfield changed before delay
                    // was reached
                }
                task = new TimerTask() {
                    public void run() {
                        scheduleFiltering();
                        task = null;
                    }
                };
                // schedule to filter after DELAY to make sure that fast typer
                // can complete their words before filtering
                getController().schedule(task, DELAY);
            }
        });
    }
}
