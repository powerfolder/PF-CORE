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

import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.model.ApplicationModel;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.Controller;

/**
 * A element which is owned by a ui controller
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public abstract class PFUIComponent extends PFComponent {
    /**
     * @param controller
     */
    protected PFUIComponent(Controller controller) {
        super(controller);
    }

    /**
     * Answers the ui controller gives acces to all user interface componenets
     *
     * @return The UIController.
     */
    protected UIController getUIController() {
        return getController().getUIController();
    }

    /**
     * @return the central application model.
     */
    protected ApplicationModel getApplicationModel() {
        return getUIController().getApplicationModel();
    }

}
