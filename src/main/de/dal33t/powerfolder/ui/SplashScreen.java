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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.Waiter;

/**
 * Splash screen
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.16 $
 */
@SuppressWarnings("serial")
public class SplashScreen extends JWindow {

    private static final Logger log = Logger.getLogger(SplashScreen.class
        .getName());

    private Controller controller;
    private JProgressBar bar;
    private Thread splashThread;
    private JLabel image;
    private Timer timer;
    private int nPercentageGuessed;
    private int nextPercentage;
    private Date startTime;

    /**
     * New splashscreen
     * 
     * @param controller
     *            the controller.
     * @param waitTime
     */
    public SplashScreen(final Controller controller, int waitTime) {
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        this.controller = controller;

        // Get last start time
        long lastStartTookMS = controller.getPreferences().getLong(
            "lastStartTookMS", 1000);

        image = new JLabel(Icons.getIconById(Icons.SPLASH));
        image.setBorder(BorderFactory.createEmptyBorder());
        bar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 100);
        bar.setBorder(BorderFactory.createEmptyBorder());

        getContentPane().add(image, BorderLayout.NORTH);
        getContentPane().add(bar, BorderLayout.SOUTH);

        pack();

        timer = new Timer("Splash barupdater", true);
        timer.schedule(new BarUpdater(), 0,
            Math.max((int) lastStartTookMS / 200, 10));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension labelSize = getPreferredSize();
        setLocation(screenSize.width / 2 - labelSize.width / 2,
            (int) (screenSize.height / 2.5) - labelSize.height / 2);
        final int pause = waitTime;
        final Runnable closerRunner = new Runnable() {
            public void run() {
                timer.purge();
                timer.cancel();
                setVisible(false);
                dispose();
            }
        };
        Runnable waitRunner = new Runnable() {
            public void run() {
                try {
                    Waiter waiter = new Waiter(pause);
                    while (!waiter.isTimeout()) {
                        waiter.waitABit();
                        if (controller.isShuttingDown()) {
                            break;
                        }
                    }
                } catch (RuntimeException e) {
                    // Ignore
                } finally {
                    SwingUtilities.invokeLater(closerRunner);
                }
            }
        };
        setVisible(true);

        splashThread = new Thread(waitRunner, "SplashScreenThread");
        splashThread.start();
    }

    /**
     * Updates the bar and intercalculate completion percentage
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
     */
    private class BarUpdater extends TimerTask {
        @Override
        public void run() {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    int v = bar.getValue();
                    if (v < nextPercentage && nPercentageGuessed < 30) {
                        bar.setValue(v + 1);
                        nPercentageGuessed++;
                    }
                }
            });
        }
    }

    /**
     * Sets the completion percentage of loading process
     * 
     * @param absPerc
     */
    public void setCompletionPercentage(final int absPerc, int nextPerc) {
        if (startTime == null) {
            // Started
            startTime = new Date();
        }

        if (absPerc >= 100) {
            long startTook = System.currentTimeMillis() - startTime.getTime();
            // completed
            controller.getPreferences().putLong("lastStartTookMS", startTook);
        }

        // Not longer guessed
        nPercentageGuessed = 0;
        nextPercentage = nextPerc;
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    bar.setValue(absPerc);
                    // draw version number only once (cannot do in init, no yet
                    // Graphics there)

                    Graphics g = image.getGraphics();
                    if (g == null) {
                        return;
                    }
                    String version = Controller.PROGRAM_VERSION;
                    g.drawString(version, 20, getHeight() - 25);
                }
            });
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "InterruptedException", e);
        } catch (InvocationTargetException e) {
            log.log(Level.SEVERE, "InvocationTargetException", e);
        }
    }

    /**
     * Shutsdown/hides the splashscreen
     */
    public void shutdown() {

        setVisible(false);
        dispose();
        if (splashThread != null) {
            splashThread.interrupt();
        }
        timer.purge();
        timer.cancel();
    }
}