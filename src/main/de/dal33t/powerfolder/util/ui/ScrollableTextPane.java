package de.dal33t.powerfolder.util.ui;

import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.*;

import de.dal33t.powerfolder.util.Logger;

/**
 * A text pane that automatic scrolls the text, starts after a delay.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.5 $
 */
public class ScrollableTextPane extends JScrollPane {
    private static final Logger LOG = Logger
        .getLogger(ScrollableTextPane.class);

    /** Timer to control the scrolling */
    protected Timer timer;
    /** The text pane that holds our text */
    private JEditorPane textPanel;

    /** delay to wait after start or scrollbar change */
    private int delay;

    /** text in HTML format to scroll... */
    public ScrollableTextPane(String text, int delay) {
        this.delay = delay;
        if (text == null) {
            throw new NullPointerException("null text: " + text);
        }
        // set mimetype and text
        textPanel = new JEditorPane("text/html", text);
        // smooth ;-)
        textPanel.setDoubleBuffered(true);
        // no editing
        textPanel.setEditable(false);
        // marge
        textPanel.setMargin(new Insets(4, 4, 4, 4));
        // add to the scrollpane
        JViewport viewPort = getViewport();
        viewPort.add(textPanel);

        // create timer
        Action scrollText = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                try {                     
                    timer.setDelay(30);
                    scroll();
                } catch (Throwable t) {
                    LOG.error(t);
                }
            }
        };
        // timer will call the above action every 30 milliseconds
        timer = new Timer(delay, scrollText);
    }

    /** start scrolling */
    public void start() {
        //try detect if scroll bar changed by hand
        //then wait again
        getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            int lastValue = -10;

            public void adjustmentValueChanged(AdjustmentEvent adjustmentEvent) {
                if (adjustmentEvent.getValueIsAdjusting()) {
                    // The user is dragging the knob
                    return;
                }
                
                //if its one more than last its our scroll code...
                //else its by hand
                if (lastValue + 1 != adjustmentEvent.getValue()) {                    
                    timer.stop();
                    timer.setDelay(delay);
                    timer.start();
                }
                lastValue = adjustmentEvent.getValue();
            }

        });
        Rectangle rect = new Rectangle(0, 0, 0, 0);
        // scroll to start position
        textPanel.scrollRectToVisible(rect);
        timer.start();
    }

    /** stop scolling */
    public void stop() {        
        timer.stop();
    }

    /**
     * Scroll the contents of the textPanel
     */
    private void scroll() {
        // get visible rectangle
        Rectangle rect = textPanel.getVisibleRect();

        // get x / y values
        int x = rect.x;
        int y = this.getVerticalScrollBar().getValue();

        // no scroll if at end
        if ((y + rect.height) >= textPanel.getHeight()) {
            return;
        }

        // one pixel lower:
        y += 1;

        Rectangle rectNew = new Rectangle(x, y, (x + rect.width),
            (y + rect.height));

        // scroll to next pixel
        textPanel.scrollRectToVisible(rectNew);
    }
}
