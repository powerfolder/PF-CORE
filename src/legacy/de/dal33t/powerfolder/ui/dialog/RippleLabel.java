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
 * $Id: RippleLabel.java 9547 2009-09-14 14:32:18Z tot $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;

/**
 * This class displays an Image in a JLabel and animates a ripple effect when
 * the mouse is moved over it. Based on some voodoo code by Neil Wallis.
 * http://www.neilwallis.com/java/water.html
 */
public class RippleLabel extends JLabel {

    private final int width;
    private final int height;
    private int[] imageTexture;
    private ImageProducer source;
    private short[] rippleMap;
    private int[] rippleTexture;
    private int oldInd;
    private int newInd;

    private volatile boolean active = true;
    private ScheduledFuture<?> rippler;

    /**
     * Constructor
     * 
     * @param controller
     * @param image
     */
    public RippleLabel(Controller controller, Image image) {

        MediaTracker mediaTracker = new MediaTracker(this);
        mediaTracker.addImage(image, 1);
        try {
            mediaTracker.waitForAll();
        } catch (Exception e) {
        }

        width = image.getWidth(this);
        height = image.getHeight(this);
        int size = width * (height + 2) * 2;

        PixelGrabber pg = new PixelGrabber(image, 0, 0, -1, -1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }

        imageTexture = (int[]) pg.getPixels();

        rippleMap = new short[size];
        rippleTexture = new int[width * height];

        source = new MemoryImageSource(width, height, rippleTexture, 0, width);

        oldInd = width;
        newInd = width * (height + 3);

        addMouseMotionListener(new MyMouseListener());

        rippler = controller.getThreadPool().scheduleAtFixedRate(new Runnable()
        {
            public void run() {
                if (active) {
                    doRipple();
                }
            }
        }, 10, 10, TimeUnit.MILLISECONDS);
    }

    public void deactivate() {
        active = false;
        rippler.cancel(false);
    }

    private void doRipple() {

        // Do some random drops occasionally.
        if (Math.random() > 0.999) {
            int x = (int) (5 + (width - 10) * Math.random());
            int y = (int) (5 + (height - 10) * Math.random());
            drop(x, y);
        }

        // Toggle maps each frame
        int i = oldInd;
        oldInd = newInd;
        newInd = i;

        i = 0;
        int mapind = oldInd;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                short data = (short) (rippleMap[mapind - width]
                    + rippleMap[mapind + width] + rippleMap[mapind - 1]
                    + rippleMap[mapind + 1] >> 1);
                data -= rippleMap[newInd + i];
                data -= data >> 5;
                rippleMap[newInd + i] = data;

                // Where data = 0 then still, where data > 0 then wave
                data = (short) (1024 - data);

                // Offsets
                int a = (x - width / 2) * data / 1024 + width / 2;
                int b = (y - height / 2) * data / 1024 + height / 2;

                // Bounds check
                if (a >= width) {
                    a = width - 1;
                }
                if (a < 0) {
                    a = 0;
                }
                if (b >= height) {
                    b = height - 1;
                }
                if (b < 0) {
                    b = 0;
                }

                rippleTexture[i] = imageTexture[a + b * width];
                mapind++;
                i++;
            }
        }
        repaint();
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(createImage(source), 0, 0, width, height, this);
    }

    private void drop(int x, int y) {
        for (int j = y - 3; j < y + 3; j++) {
            for (int k = x - 3; k < x + 3; k++) {
                if (j >= 0 && j < height && k >= 0 && k < width) {
                    rippleMap[oldInd + j * width + k] += 512;
                }
            }
        }
    }

    private class MyMouseListener extends MouseAdapter {
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            drop(x, y);
        }
    }
}
