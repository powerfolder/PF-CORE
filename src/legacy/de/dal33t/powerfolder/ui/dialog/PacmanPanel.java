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
package de.dal33t.powerfolder.ui.dialog;

import de.dal33t.powerfolder.ui.util.Icons;

import javax.swing.*;
import java.awt.*;

/**
 * Pacman "Easter egg" class.
 * Animates an 'eater' on the AboutDialog.
 */
public class PacmanPanel extends JPanel {

    private volatile Image image = Icons.getImageById(Icons.PACMAN_00);
    private volatile int offset;
    private volatile boolean active;

    /**
     * Limit to 19px high.
     *
     * @return
     */
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        return new Dimension((int) preferredSize.getWidth(), 19);
    }

    /**
     * Draw the animation.
     *
     * @param g
     */
    public void paint(Graphics g) {
        if (active) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.drawImage(image, offset, 1, 33, 17, this);

            int x = offset + 27;
            while (x < getWidth()) {
                x += 17;
                g.drawImage(Icons.getImageById(Icons.PACMAN_DOT), x, 1, 2, 17, this);
            }
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Run an animation thread.
     */
    public void activate() {
        if (active) {
            return;
        }
        
        Thread t = new Thread() {
            public void run() {
                active = true;
                offset = 0;
                repaint();
                int i = 0;
                while(true) {
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                        // Ignore.
                    }

                    if (i > 16) {
                        i = 0;
                        offset += 17;
                    }

                    switch (i) {
                        case 0:
                            image = Icons.getImageById(Icons.PACMAN_00);
                            break;
                        case 1:
                            image = Icons.getImageById(Icons.PACMAN_01);
                            break;
                        case 2:
                            image = Icons.getImageById(Icons.PACMAN_02);
                            break;
                        case 3:
                            image = Icons.getImageById(Icons.PACMAN_03);
                            break;
                        case 4:
                            image = Icons.getImageById(Icons.PACMAN_04);
                            break;
                        case 5:
                            image = Icons.getImageById(Icons.PACMAN_05);
                            break;
                        case 6:
                            image = Icons.getImageById(Icons.PACMAN_06);
                            break;
                        case 7:
                            image = Icons.getImageById(Icons.PACMAN_07);
                            break;
                        case 8:
                            image = Icons.getImageById(Icons.PACMAN_08);
                            break;
                        case 9:
                            image = Icons.getImageById(Icons.PACMAN_09);
                            break;
                        case 10:
                            image = Icons.getImageById(Icons.PACMAN_10);
                            break;
                        case 11:
                            image = Icons.getImageById(Icons.PACMAN_11);
                            break;
                        case 12:
                            image = Icons.getImageById(Icons.PACMAN_12);
                            break;
                        case 13:
                            image = Icons.getImageById(Icons.PACMAN_13);
                            break;
                        case 14:
                            image = Icons.getImageById(Icons.PACMAN_14);
                            break;
                        case 15:
                            image = Icons.getImageById(Icons.PACMAN_15);
                            break;
                        case 16:
                        default:
                            image = Icons.getImageById(Icons.PACMAN_16);
                            break;
                    }

                    repaint();

                    i++;

                    if (offset > getWidth()) {
                        active = false;
                        repaint();
                        break;
                    }
                }
            }
        };

        t.start();
    }
}
