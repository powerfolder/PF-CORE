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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.Waiter;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Splash screen
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.16 $
 */
public class SplashScreen extends JWindow {

    private static final Logger log = Logger.getLogger(SplashScreen.class.getName());

    private static final Color FREE_BAR_COLOR1 = new Color(100, 10, 15);
    private static final Color FREE_BAR_COLOR2 = new Color(235, 235, 235);
    private static final Color FREE_TEXT_COLOR = new Color(100, 10, 15);

    private static final Color PRO_BAR_COLOR1 = new Color(66, 99, 128);
    private static final Color PRO_BAR_COLOR2 = new Color(235, 235, 235);
    private static final Color PRO_TEXT_COLOR = Color.BLACK;

    private Controller controller;
    private JProgressBar bar;
    private Thread splashThread;
    private JLabel image;
    private Timer timer;
    private int nPercentageGuessed;
    private int nextPercentage;
    private Date startTime;
    private long lastStartTookMS;

    /**
     * New splashscreen
     * 
     * @param controller
     *            the controller.
     * @param waitTime
     */
    public SplashScreen(final Controller controller, int waitTime) {
        super();
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        this.controller = controller;

        // Get last start time
        lastStartTookMS = controller.getPreferences().getLong(
            "lastStartTookMS", 1000);

        image = new JLabel(Icons.SPLASH);
        bar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 100);
        bar.setOpaque(false);
        if (Util.isRunningProVersion()) {
            bar.setForeground(PRO_BAR_COLOR1);
            bar.setBackground(PRO_BAR_COLOR2);
            getContentPane().setBackground(PRO_BAR_COLOR2);
        } else {
            bar.setForeground(FREE_BAR_COLOR1);
            bar.setBackground(FREE_BAR_COLOR2);
            getContentPane().setBackground(FREE_BAR_COLOR2);
        }
        bar.setBorder(BorderFactory.createEmptyBorder());

        // l.setBorder(new SplashBorder());
        // bar.setBorder(new SplashBorder());
        getContentPane().add(image, BorderLayout.NORTH);
        getContentPane().add(bar, BorderLayout.SOUTH);
     //   getContentPane().setBackground(Color.WHITE);
      //  getRootPane().setOpaque(true);
        

      
        pack();

        getRootPane().setBorder(new SplashBorder());

        timer = new Timer("Splash barupdater", true);
        timer.schedule(new BarUpdater(), 0, Math.max(
            (int) lastStartTookMS / 200, 10));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension labelSize = this.getPreferredSize();
        setLocation(screenSize.width / 2 - (labelSize.width / 2),
            (int) (screenSize.height / 2.5) - (labelSize.height / 2));
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
                    try {
                        SwingUtilities.invokeAndWait(closerRunner);
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "Exception", e);
                    }
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
                    if (Util.isRunningProVersion()) {
                        g.setColor(PRO_TEXT_COLOR);
                    } else {
                        g.setColor(FREE_TEXT_COLOR);
                    }
                    String version = Translation.getTranslation(
                        "splash.version", Controller.PROGRAM_VERSION);
                    g.drawString(version, 460, 145);
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

    /**
     * Adds a custom border to the splashscreen
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.16 $
     */
    private class SplashBorder extends AbstractBorder {

        public void paintBorder(Component c, Graphics g, int x, int y,
            int width, int height)
        {
            g.setColor(SystemColor.controlDkShadow);
            g.drawRect(x, y, width - 1, height - 1);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }
}