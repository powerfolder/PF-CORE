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
package de.dal33t.powerfolder.ui.model;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.navigation.NavTreeModel;

/**
 * Contains all models for the application such as the (additional) top level
 * items or the core models (TODO #278).
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ApplicationModel extends PFUIComponent {
    // TODO #278 Throw away?
    private NavTreeModel navTreeModel;
    private RootTableModel rootTabelModel;

    /**
     * Constructs a non-initialized application model. Before the model can used
     * call {@link #initialize()}
     * 
     * @param controller
     * @see #initialize()
     */
    public ApplicationModel(Controller controller) {
        super(controller);
    }

    /**
     * Initializes this and all submodels
     */
    public void initialize() {
        navTreeModel = new NavTreeModel(getController());
        rootTabelModel = new RootTableModel(getController(), navTreeModel);
    }

    // Exposing ***************************************************************

    public NavTreeModel getNavTreeModel() {
        return navTreeModel;
    }

    public RootTableModel getRootTabelModel() {
        return rootTabelModel;
    }

}
