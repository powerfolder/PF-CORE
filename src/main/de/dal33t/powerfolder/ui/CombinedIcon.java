package de.dal33t.powerfolder.ui;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/* called when have two or more icons in a label or a table cell */
public class CombinedIcon implements Icon {
    enum Orientation {
        HORIZONTAL, VERTICAL
    }

    public CombinedIcon(Icon first, Icon second) {
        this(first, second, Orientation.HORIZONTAL);
    }

    public CombinedIcon(Icon first, Icon second, Orientation orientation) {
        assert first != null : "The 'first' Icon cannot be null";
        assert second != null : "The 'second' Icon cannot be null";

        first_ = first;
        second_ = second;
        orientation_ = orientation == null
            ? Orientation.HORIZONTAL
            : orientation;
    }

    public int getIconHeight() {
        if (orientation_ == Orientation.VERTICAL) {
            return first_.getIconHeight() + second_.getIconHeight();
        }
        return Math.max(first_.getIconHeight(), second_.getIconHeight());
    }

    public int getIconWidth() {
        if (orientation_ == Orientation.VERTICAL) {
            return Math.max(first_.getIconWidth(), second_.getIconWidth());
        }
        return first_.getIconWidth() + second_.getIconWidth();
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (orientation_ == Orientation.VERTICAL) {
            int heightOfFirst = first_.getIconHeight();
            int maxWidth = getIconWidth();
            first_.paintIcon(c, g, x + (maxWidth - first_.getIconWidth())
                / 2, y);
            second_.paintIcon(c, g, x + (maxWidth - second_.getIconWidth())
                / 2, y + heightOfFirst);
        } else {
            int widthOfFirst = first_.getIconWidth();
            int maxHeight = getIconHeight();
            first_.paintIcon(c, g, x, y
                + (maxHeight - first_.getIconHeight()) / 2);
            second_.paintIcon(c, g, x + widthOfFirst, y
                + (maxHeight - second_.getIconHeight()) / 2);
        }
    }

    private Icon first_;
    private Icon second_;
    private Orientation orientation_;
}