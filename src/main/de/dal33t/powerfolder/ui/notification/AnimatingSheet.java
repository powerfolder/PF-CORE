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
* $Id: AnimatingSheet.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.ui.notification;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Class to animate the notification window
 */
public class AnimatingSheet extends JPanel {

    private Dimension animatingSize = new Dimension(0, 1);
    private JComponent source;
    private BufferedImage offscreenImage;

    /**
     * Constructor
     */
    public AnimatingSheet() {
        setOpaque(true);
    }

    /**
     * @see javax.swing.JComponent#getMaximumSize()
     */
    public Dimension getMaximumSize() {
        return animatingSize;
    }

    /**
     * @see javax.swing.JComponent#getMinimumSize()
     */
    public Dimension getMinimumSize() {
        return animatingSize;
    }

    /**
     * @see javax.swing.JComponent#getPreferredSize()
     */
    public Dimension getPreferredSize() {
        return animatingSize;
    }

    private void makeOffscreenImage(JComponent sourceArg) {
        GraphicsConfiguration gfxConfig = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getDefaultScreenDevice()
            .getDefaultConfiguration();
        offscreenImage = gfxConfig.createCompatibleImage(sourceArg.getWidth(),
            sourceArg.getHeight());
        Graphics2D offscreenGraphics = (Graphics2D) offscreenImage
            .getGraphics();
        // windows workaround
        offscreenGraphics.setColor(sourceArg.getBackground());
        offscreenGraphics.fillRect(0, 0, sourceArg.getWidth(), sourceArg
            .getHeight());
        // paint from source to offscreen buffer
        sourceArg.paint(offscreenGraphics);
    }

    /**
     * Paints the component by drawing image on top of it
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    protected void paintComponent(Graphics g) {
        // We'll do our own painting, so leave out
        // a call to the superclass behavior
        // We're telling Swing that we're opaque, and
        // we'll honor this by always filling our
        // our entire bounds with image.
        g.drawImage(offscreenImage, 0, 0, source.getWidth(),
            animatingSize.height, 0, 0, source.getWidth(),
            animatingSize.height, this);
    }

    public void setAnimatingHeight(int height) {
        animatingSize.height = height;
        setSize(animatingSize);
    }

    public void setSource(JComponent source) {
        this.source = source;
        animatingSize.width = source.getWidth();
        makeOffscreenImage(source);
    }
}
