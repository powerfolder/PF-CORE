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
package de.dal33t.powerfolder.ui.notification;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.Timer;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.ui.util.UIUtil;

/**
 * Creates an animated view that slides out of the bottom-right corner of the
 * screen. The user of this class supplies the contents for sliding. Also, the
 * view automatically closes itself after predetermined amount of time, which is
 * currently set to 10 seconds.<br>
 * This class is based on code and ideas from <i>Swing Hacks</i> book by Joshua
 * Marinacci and Chris Adamson.<br>
 * If the Java version is high enough, it fades the window instead of sliding
 * it.
 *
 * @author <a href="mailto:magapov@gmail.com">Maxim Agapov</a>
 * @version $Revision: 1.2 $
 */
public class Slider {

    /**
     * Default delay between animation frames, 5ms
     */
    public static final int ANIMATION_DELAY = 5;

    private int displaySeconds;
    private Window owner;
    private JWindow window;
    private JComponent contents;
    private Timer animateUpTimer;
    private Timer dismissTimer;
    private Timer animateDownTimer;
    private int translucencyPercentage;
    private boolean displayLeft;

    /**
     * Constructor
     *
     * @param contents
     * @param displaySeconds
     * @param translucencyPercentage
     * @param displayLeft
     */
    public Slider(JComponent contents, int displaySeconds,
        int translucencyPercentage, boolean displayLeft)
    {
        this(contents, null, displaySeconds, translucencyPercentage,
            displayLeft);
    }

    /**
     * Constructor
     *
     * @param contents
     * @param displaySeconds
     * @param translucencyPercentage
     * @param displayLeft
     */
    public Slider(JComponent contents, Window owner, int displaySeconds,
        int translucencyPercentage, boolean displayLeft)
    {
        this.contents = contents;
        this.displaySeconds = displaySeconds;
        this.translucencyPercentage = translucencyPercentage;
        this.displayLeft = displayLeft;
        this.owner = owner;
    }

    public JComponent getContents() {
        return contents;
    }

    /*
     * Query graphics environment for maximum window bounds.
     */
    private static Rectangle initDesktopBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getMaximumWindowBounds();
    }

    /**
     * Create a window with an animating sheet copy over its contents from the
     * temp window animate it when done, remove animating sheet and add real
     * contents.
     */
    public void show() {
        if (window != null) {
            return;
        }
        window = new JWindow(owner);
        window.setAlwaysOnTop(true);
        if (Constants.OPACITY_SUPPORTED) {
            UIUtil.applyTranslucency(window, 0.0f);
        }

        // Initial boundaries.
        Dimension contentsSize = contents.getPreferredSize();
        Rectangle desktopBounds = initDesktopBounds();
        int showX = displayLeft ? 10 : desktopBounds.width - contentsSize.width
            - 10;
        int startY = desktopBounds.y + desktopBounds.height - 10;

        window.getContentPane().add(contents);
        window.pack();
        window.setLocation(showX, startY - window.getHeight());
        window.setVisible(true);

        // Timer to animate the sheet down.
        animateDownTimer = new Timer(ANIMATION_DELAY, new ActionListener() {
            private int percentage = 99;

            public void actionPerformed(ActionEvent e) {
                animate(percentage);
                if (percentage-- <= 0) {
                    animateDownTimer.stop();
                }
            }
        });

        // Timer to pause for the user to read.
        dismissTimer = new Timer(1000 * displaySeconds, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                animateDownTimer.start();
            }
        });
        dismissTimer.setRepeats(false);

        // Timer to animate the sheet up.
        animateUpTimer = new Timer(ANIMATION_DELAY, new ActionListener() {
            private int percentage = 1;

            public void actionPerformed(ActionEvent e) {
                animate(percentage);
                if (percentage++ >= 100) {
                    animateUpTimer.stop();
                    dismissTimer.start();
                }
            }
        });

        animateUpTimer.start();
    }

    /**
     * Close and dispose the animated window.
     */
    public void close() {

        // stop the timers
        if (animateUpTimer != null && animateUpTimer.isRunning()) {
            animateUpTimer.stop();
        }
        if (animateDownTimer != null && animateDownTimer.isRunning()) {
            animateDownTimer.stop();
        }
        if (dismissTimer != null && dismissTimer.isRunning()) {
            dismissTimer.stop();
        }

        // close the window
        if (window != null) {
            window.dispose();
            window = null;
        }
    }

    /**
     * Show the correct percentage of the size.
     *
     * @param percentage
     */
    public void animate(long percentage) {
        if (window == null) {
            // Huh?
            return;
        }
        if (Constants.OPACITY_SUPPORTED) {
            // Opacity = 1 - translucency.
            float opacity = 1.0f - translucencyPercentage / 100.0f;
            UIUtil.applyTranslucency(window, opacity * percentage / 100.0f);
        }
        if (percentage <= 0) {
            close();
        }
    }
}
