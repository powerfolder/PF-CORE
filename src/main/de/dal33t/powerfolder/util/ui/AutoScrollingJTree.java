package de.dal33t.powerfolder.util.ui;

import java.awt.*;
import java.awt.dnd.Autoscroll;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;

/**
 * Autoscrolling JTree for drag and drop.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class AutoScrollingJTree extends JTree implements Autoscroll {

    /**
     * the size of the borders of the visible part of the tree in which the
     * autoscroll start
     */
    private int margin = 12;
    /** number of pix to scroll */
    private int scrollPix = 25;

    public AutoScrollingJTree(TreeModel model) {
        super(model);
    }

    /**
     * Ok, we have been told to scroll because the mouse cursor is in our scroll
     * zone.
     */
    public void autoscroll(Point cursorLocation) {
        // this code does not determine the direction of the scroll but seams to
        // work ... don't ask me why...
        int x = cursorLocation.x - scrollPix;
        int y = cursorLocation.y - scrollPix;
        int width = scrollPix + scrollPix;
        int height = scrollPix + scrollPix;
        scrollRectToVisible(new Rectangle(x, y, width, height));
    }

    /**
     * the borders of the tree with a margin in which the tree will start
     * scrolling.
     * 
     * @see #margin
     */
    public Insets getAutoscrollInsets() {
        Rectangle outer = getBounds();
        Rectangle inner = getParent().getBounds();
        int top = inner.y - outer.y + margin;
        int left = inner.x - outer.x + margin;
        int bottom = outer.height - inner.height - inner.y + outer.y + margin;
        int right = outer.width - inner.width - inner.x + outer.x + margin;
        return new Insets(top, left, bottom, right);
    }
}
