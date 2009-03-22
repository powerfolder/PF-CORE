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
 * $Id: WarningModel.java 5975 2008-12-14 05:23:32Z harry $
 */
package de.dal33t.powerfolder.ui.model;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This model holds warnings that are displayed to the user in the HomeTab.
 */
public class WarningModel extends PFComponent implements WarningHandler {

    private final ValueModel warningsCountVM = new ValueHolder();

    private List<WarningEvent> warnings =
            new CopyOnWriteArrayList<WarningEvent>();

    /**
     * Constructor
     *
     * @param controller
     */
    public WarningModel(Controller controller) {
        super(controller);
        warningsCountVM.setValue(0);
        getController().addWarningHandler(this);
    }

    /**
     * Adds a warning to the list.
     *
     * @param warning
     */
    public void pushWarning(WarningEvent warning) {
        warnings.add(warning);
        warningsCountVM.setValue(warnings.size());
    }

    /**
     * VM that holds the number of warnings available.
     * @return
     */
    public ValueModel getWarningsCountVM() {
        return warningsCountVM;
    }

    /**
     * Pops the next warning, if available.
     *
     * @return
     */
    public WarningEvent getNextWarning() {
        if (warnings.isEmpty()) {
            return null;
        }
        WarningEvent event = warnings.remove(0);
        warningsCountVM.setValue(warnings.size());
        return event;
    }
}