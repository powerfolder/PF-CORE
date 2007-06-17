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
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Creates an animated view that slides out of the bottom-right corner of the screen.
 * The user of this class supplies the contents for sliding. Also, the view automatically closes
 * itself after predetermined amount of time, which is currently set to 15 seconds.<br>
 * This class is based on code and ideas from <i>Swing Hacks</i> book by Joshua Marinacci and Chris Adamson.<br>
 *  
 * @author <a href="mailto:magapov@gmail.com">Maxim Agapov</a>
 * @version $Revision: 1.1 $
 *
 */
public class Slider implements ActionListener {

    /**
     * Default animation time
     */
    public static final int ANIMATION_TIME = 500;

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

    /**
     * Close and dispose the animated window.
     */
    public void close() {
        //stop the dismissal timer
        if (dismissTimer != null) {
            dismissTimer.stop();
            dismissTimer =null;
        }
        //close the window
        if (window != null) {
            window.dispose();
            window = null;
        }
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

    public void show() {
        if (window != null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                showInUI();
            }
        });
    }

    /*
     * create a window with an animating sheet copy over its contents from the
     * temp window animate it when done, remove animating sheet and add real
     * contents.
     */
    private void showInUI() {
        window = new JWindow();
        AnimatingSheet animatingSheet = new AnimatingSheet();
        animatingSheet.setSource(contents);
        window.add(animatingSheet);

        Dimension contentsSize = contents.getSize();
        Rectangle desktopBounds = initDesktopBounds();
        int showX = desktopBounds.width - contentsSize.width;
        int startY = desktopBounds.y + desktopBounds.height;

        long animationStart = System.currentTimeMillis();
        long elapsed = 0;

        while (elapsed < ANIMATION_TIME) {
            log("elapsed=" + elapsed);
            float progress = (float) elapsed / ANIMATION_TIME;
            log(", progress=" + progress);

            // get height to show
            int animatingHeight = (int) (progress * contentsSize.getHeight());

            animatingHeight = Math.max(animatingHeight, 1);
            animatingSheet.setAnimatingHeight(animatingHeight);
            window.pack();
            window.setLocation(showX, startY - window.getHeight());
            log(", X=" + showX + ", Y=" + (startY - window.getHeight()));
            window.setVisible(true);
            window.repaint();
            try {
                //this is executed in the Swing's event dispatcher thread
                Thread.sleep(ANIMATION_DELAY);
            } catch (InterruptedException e1) {
            }
            elapsed = System.currentTimeMillis() - animationStart;
        }

        // put real contents in window and show
        window.getContentPane().removeAll();
        window.getContentPane().add(contents);
        window.pack();
        window.setLocation(showX, startY - window.getHeight());
        window.setVisible(true);
        // window.repaint();
        
        //start the timer to autodismiss the view
        dismissTimer = new Timer(DISMISS_DELAY, this);
        dismissTimer.setRepeats(false);
        dismissTimer.start();
    }

    private void log(String msg) {
        // System.out.print(msg);
    }

    /**
     * Event listener method - handles dismiss timer event
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == dismissTimer) {
            close();
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

        public Dimension getMaximumSize() {
            return animatingSize;
        }

        public Dimension getMinimumSize() {
            return animatingSize;
        }

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

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.JComponent#paint(java.awt.Graphics)
         */
        public void paint(Graphics g) {
            // get the top-most n pixels of source and
            // paint them into g, where n is height
            // (different from sheet example, which used bottom-most)
            BufferedImage fragment = offscreenImage.getSubimage(0, 0, source
                .getWidth(), animatingSize.height);
            g.drawImage(fragment, 0, 0, this);
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

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.JComponent#update(java.awt.Graphics)
         */
        public void update(Graphics g) {
            // override to eliminate flicker from
            // unnecessary clear
            paint(g);
        }
    }
}
