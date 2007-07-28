package de.dal33t.powerfolder.util.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

/**
 * Creates an animated view that slides out of the bottom-right corner of the
 * screen. The user of this class supplies the contents for sliding. Also, the
 * view automatically closes itself after predetermined amount of time, which is
 * currently set to 15 seconds.<br>
 * This class is based on code and ideas from <i>Swing Hacks</i> book by Joshua
 * Marinacci and Chris Adamson.<br>
 * 
 * @author <a href="mailto:magapov@gmail.com">Maxim Agapov</a>
 * @version $Revision: 1.2 $
 */
public class Slider implements ActionListener {

    /**
     * Default animation time
     */
    public static final int ANIMATION_TIME = 700;

    /**
     * Default delay between animation frames
     */
    public static final int ANIMATION_DELAY = 50;

    /**
     * Default delay before autodismissing the view is 15 seconds
     */
    public static final int DISMISS_DELAY = 15000;

    private JWindow window;

    private JComponent contents;

    private Timer dismissTimer;

    /**
     * Constructor
     * 
     * @param contents
     */
    public Slider(JComponent contents) {
        super();
        this.contents = contents;
    }

    public JComponent getContents() {
        return contents;
    }

    /*
     * Query graphics environment for maximum window bounds.
     */
    private Rectangle initDesktopBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getMaximumWindowBounds();
        // System.out.println("max window bounds = " + desktopBounds);
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
        window = new JWindow();
        final AnimatingSheet animatingSheet = new AnimatingSheet();
        animatingSheet.setSource(contents);
        window.getContentPane().add(animatingSheet);
        window.setAlwaysOnTop(true);

        final Dimension contentsSize = contents.getSize();
        Rectangle desktopBounds = initDesktopBounds();
        final int showX = desktopBounds.width - contentsSize.width;
        final int startY = desktopBounds.y + desktopBounds.height;

        final long animationStart = System.currentTimeMillis();

        // this is the main animation timer
        final Timer animationTimer = new Timer(ANIMATION_DELAY,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    long elapsed = System.currentTimeMillis() - animationStart;
                    if (elapsed < ANIMATION_TIME) {
                        log("elapsed=" + elapsed);
                        float progress = (float) elapsed / ANIMATION_TIME;
                        log(", progress=" + progress);
                        // get height to show
                        int animatingHeight = (int) (progress * contentsSize.height);
                        animatingHeight = Math.max(animatingHeight, 1);
                        animatingSheet.setAnimatingHeight(animatingHeight);
                        window.pack();
                        window.setLocation(showX, startY - window.getHeight());
                        log(", X=" + showX + ", Y="
                            + (startY - window.getHeight()));
                        window.setVisible(true);
                        // window.repaint();
                    } else {
                        // first thing to do is to stop the animation timer
                        Timer myTimer = (Timer) e.getSource();
                        myTimer.stop();
                        // put real contents in window and show
                        window.getContentPane().removeAll();
                        window.getContentPane().add(contents);
                        window.pack();
                        window.setLocation(showX, startY - window.getHeight());
                        window.setVisible(true);
                        // window.repaint();
                        // start the timer to autodismiss the view
                        initDismissTimer();
                    }
                }
            });
        animationTimer.setInitialDelay(0);
        animationTimer.setCoalesce(true);
        animationTimer.start();
    }

    private void log(String msg) {
        // System.out.print(msg);
    }

    /*
     * initializes the dismiss timer 
     */
    private void initDismissTimer() {
        dismissTimer = new Timer(DISMISS_DELAY, this);
        dismissTimer.setRepeats(false);
        dismissTimer.start();
    }

    /**
     * Event listener method - handles dismiss timer event
     * 
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == dismissTimer) {
            close();
        }
    }

    /**
     * Close and dispose the animated window.
     */
    public void close() {
        // stop the dismissal timer
        if (dismissTimer != null) {
            dismissTimer.stop();
            dismissTimer = null;
        }
        // close the window
        if (window != null) {
            window.dispose();
            window = null;
        }
    }

    // AnimatingSheet inner class
    private class AnimatingSheet extends JPanel {

        private static final long serialVersionUID = 1L;
        private Dimension animatingSize = new Dimension(0, 1);
        private JComponent source;
        private BufferedImage offscreenImage;

        /**
         * Constructor
         */
        public AnimatingSheet() {
            super();
            setOpaque(true);
        }

        /* (non-Javadoc)
         * @see javax.swing.JComponent#getMaximumSize()
         */
        public Dimension getMaximumSize() {
            return animatingSize;
        }

        /* (non-Javadoc)
         * @see javax.swing.JComponent#getMinimumSize()
         */
        public Dimension getMinimumSize() {
            return animatingSize;
        }

        /* (non-Javadoc)
         * @see javax.swing.JComponent#getPreferredSize()
         */
        public Dimension getPreferredSize() {
            return animatingSize;
        }

        private void makeOffscreenImage(JComponent source) {
            GraphicsConfiguration gfxConfig = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration();
            offscreenImage = gfxConfig.createCompatibleImage(source.getWidth(),
                source.getHeight());
            Graphics2D offscreenGraphics = (Graphics2D) offscreenImage
                .getGraphics();
            // windows workaround
            offscreenGraphics.setColor(source.getBackground());
            offscreenGraphics.fillRect(0, 0, source.getWidth(), source
                .getHeight());
            // paint from source to offscreen buffer
            source.paint(offscreenGraphics);
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
}
