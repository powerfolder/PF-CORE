/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BaseMultiResolutionImage;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.UIThemeState;
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

        image = new JLabel(splashIcon());
        image.setBorder(BorderFactory.createEmptyBorder());
        // PFC-3643: smooth continuous loading bar filled in the brand accent colour
        // on a light track (FlatLaf rounds it), matching the new wallpaper splash.
        // Same on all platforms.
        bar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 100);
        bar.setBorder(BorderFactory.createEmptyBorder());
        bar.setBorderPainted(false);
        bar.setStringPainted(false);
        bar.setOpaque(true);
        bar.setPreferredSize(new java.awt.Dimension(574, 10));
        // Light mode: white fills over the brand-accent track; dark mode: the accent fills over a light track.
        java.awt.Color accent = LookAndFeelSupport.getAccentColor();
        java.awt.Color light = new java.awt.Color(0xEDF1F5);
        boolean dark = LookAndFeelSupport.isDarkMode();
        bar.setForeground(dark ? accent : light);
        bar.setBackground(dark ? light : accent);
        bar.putClientProperty("JProgressBar.largeHeight", Boolean.TRUE);

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
    /**
     * The splash of the active skin plus the {@code @1.5x} / {@code @2x} files next to it, so it is sharp on scaled
     * displays. Without such files (branded clients) the plain splash icon.
     */
    private static Icon splashIcon() {
        Icon icon = Icons.getIconById(Icons.SPLASH);
        String path = Icons.getIconProperties().getProperty(Icons.SPLASH);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        // In dark mode prefer the branding's dark splash (and its @1.5x / @2x
        // siblings) when it ships one; light-only brandings are unaffected.
        if (UIThemeState.isDark() && path != null && path.endsWith(".png")) {
            String darkPath = path.substring(0, path.length() - ".png".length()) + "_dark.png";
            if (cl.getResource(darkPath) != null) {
                path = darkPath;
            }
        }
        URL baseURL = path != null && path.endsWith(".png") ? cl.getResource(path) : null;
        if (baseURL == null) {
            return icon;
        }
        String base = path.substring(0, path.length() - ".png".length());
        List<Image> variants = new ArrayList<>();
        variants.add(new ImageIcon(baseURL).getImage());
        for (String suffix : new String[]{"@1.5x.png", "@2x.png"}) {
            URL url = cl.getResource(base + suffix);
            if (url != null) {
                variants.add(new ImageIcon(url).getImage());
            }
        }
        if (variants.size() == 1) {
            return icon;
        }
        return new ImageIcon(new BaseMultiResolutionImage(variants.toArray(new Image[0])));
    }

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
