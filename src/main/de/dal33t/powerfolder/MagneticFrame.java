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
* $Id: MagneticFrame.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder;

import javax.swing.*;
import java.awt.*;

/**
 * This abstract class attempts to track movements of the MainFrame
 */
public abstract class MagneticFrame extends PFUIComponent {

    /**
     * Constructor
     *
     * @param controller
     */
    public MagneticFrame(Controller controller) {
        super(controller);
    }

    /**
     * The UI component must be a JFrame.
     *
     * @return
     */
    public abstract JFrame getUIComponent();

    /**
     * This defines the amount to move the frame by. If the edge is already out
     * of bounds, do not move any farther in that direction.
     *
     * @param changeX
     *                  mainFrame x movement
     * @param changeY
     *                  mainFrame y movement
     */
    public void nudge(int changeX, int changeY) {

        JFrame frame = getUIComponent();

        if (frame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
            return;
        }
        
        Point location = frame.getLocation();
        boolean underX = location.getX() < 0;
        boolean underY = location.getY() < 0;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        boolean overX = location.getX() + frameSize.getWidth()
                > screenSize.getWidth();
        boolean overY = location.getY() + frameSize.getHeight()
                > screenSize.getHeight();

        int newX;
        if (changeX < 0) {
            newX = underX ? (int) location.getX()
                    : (int) location.getX() + changeX;
        } else {
            newX = overX ? (int) location.getX()
                    : (int) location.getX() + changeX;
        }

        int newY;
        if (changeY < 0) {
            newY = underY ? (int) location.getY()
                    : (int) location.getY() + changeY;
        } else {
            newY = overY ? (int) location.getY()
                    : (int) location.getY() + changeY;
        }

        Point newLocation = new Point(newX, newY);
        frame.setLocation(newLocation);
    }
}
