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
* $Id: InformationCard.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;

import javax.swing.*;
import java.awt.*;

/**
 * Base class for all 'cards' that can be displayed in the InformationFrame.
 */
public abstract class InformationCard extends PFUIComponent {

    protected InformationCard(Controller controller) {
        super(controller);
    }

    /**
     * Returns the Image that is applied to the InformationFrame.
     * @return
     */
    public abstract Image getCardImage();

    /**
     * Returns the title that is applied to the InformationFrame.
     * @return
     */
    public abstract String getCardTitle();

    /**
     * This is the JComponent that is displayed in the InformationFrame.
     * @return
     */
    public abstract JComponent getUIComponent();

    /**
     * The type of the information card.
     * Used to set the individual location of each panel type.
     *
     * @return
     */
    public abstract InformationCardType getInformationCardType();

}
