/* $Id: SplashScreen.java,v 1.16 2006/04/10 19:03:38 schaatser Exp $
 */
package de.dal33t.powerfolder.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;

/**
 * Splash screen
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.16 $
 */
public class SplashScreen extends JWindow {
    private static final Logger LOG = Logger.getLogger(SplashScreen.class);

    private Controller controller;
    private JProgressBar bar;
    private Thread splashThread;
    private JLabel image;
    private Timer timer;
    private int nPercentageGuessed;
    private Date startTime;
    private long lastStartTookMS;

    /**
     * New splashscreen
     * 
     * @param controller
     *            the controller.
     * @param waitTime
     */
    public SplashScreen(Controller controller, int waitTime) {
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
        bar.setForeground(new Color(254, 229, 140));
        bar.setBackground(new Color(253, 210, 61));
        bar.setBorder(Borders.EMPTY_BORDER);

        // l.setBorder(new SplashBorder());
        // bar.setBorder(new SplashBorder());
        getContentPane().add(image, BorderLayout.NORTH);
        getContentPane().add(bar, BorderLayout.SOUTH);

        getRootPane().setOpaque(true);
        pack();

        getRootPane().setBorder(new SplashBorder());

        timer = new Timer("Splash barupdater", true);
        timer.schedule(new BarUpdater(), 0, (int) lastStartTookMS / 200);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension labelSize = this.getPreferredSize();
        setLocation(screenSize.width / 2 - (labelSize.width / 2),
            (int) (screenSize.height / 2.5) - (labelSize.height / 2));
        final int pause = waitTime;
        final Runnable closerRunner = new Runnable() {
            public void run() {
                timer.cancel();
                setVisible(false);
                dispose();
            }
        };
        Runnable waitRunner = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(pause);
                } catch (InterruptedException e) {
                    LOG.verbose(e);
                } finally {
                    try {
                        SwingUtilities.invokeAndWait(closerRunner);
                    } catch (Exception e) {
                        LOG.error(e);
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
        public void run()
        {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    int v = bar.getValue();
                    if (v < 100 && nPercentageGuessed < 30) {
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
    public void setCompletionPercentage(final int absPerc) {
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
                    g.setColor(Color.RED);
                    String version = Translation.getTranslation(
                        "splash.version", Controller.PROGRAM_VERSION);
                    g.drawString(version, 500, 180);
                }
            });
        } catch (InterruptedException e) {
            LOG.error(e);
        } catch (InvocationTargetException e) {
            LOG.error(e);
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
            // Robot r = null;
            // try {
            // r = new Robot();
            // // TODO Auto-generated method stub
            // } catch (AWTException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // e = null;
            // }

            g.setColor(PlasticLookAndFeel.getControlDarkShadow());
            g.drawRect(x, y, width - 1, height - 1);
            // g.drawRect(x + 1, y + 1, width - 2, height - 2);

            // // Upper line
            // g.drawLine(x + 3, y + 1, x + width - 4, y + 1);
            // // Lower line
            // g.drawLine(x + 3, y + height - 2, x + width - 4, y + height - 2);
            // // Left line
            // g.drawLine(x + 1, y + 3, x + 1, y + height - 4);
            // // Right line
            // g.drawLine(x + width - 2, y + 3, x + width - 2, y + height - 4);

        }

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }
}