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

import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TimerTask;

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
    private TimerTask task;

    /** The value model <String> of the searchfield we listen to */
    private final ValueModel searchFieldVM;

    /** The value model setting flat mode */
    private ValueModel flatMode;

    protected FilterModel(Controller controller, ValueModel searchFieldVM) {
        super(controller);
        this.searchFieldVM = searchFieldVM;
        searchFieldVM.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (task != null) {
                    // cancel if seach field changed before delay was reached
                    task.cancel();
                }
                task = new TimerTask() {
                    public void run() {
                        preScheduleFiltering();
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

    public void preScheduleFiltering() {
        // Override to do stuff just before scheduling filtering on searchFiledChange.
    }

    public abstract void scheduleFiltering();

    public ValueModel getSearchFieldVM() {
        return searchFieldVM;
    }

    public boolean isFlatMode() {
        Object value = flatMode.getValue();
        return value != null && (Boolean) flatMode.getValue();
    }

    public void setFlatMode(ValueModel flatMode) {
        this.flatMode = flatMode;
        flatMode.addValueChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                TimerTask flatTask = new TimerTask() {
                    public void run() {
                        scheduleFiltering();
                    }
                };
                getController().schedule(flatTask, DELAY);
            }
        });
    }
}
