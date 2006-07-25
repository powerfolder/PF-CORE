/* $Id$
 */
package de.dal33t.powerfolder.ui.widget;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JLabel;

/**
 * A Label which supported antialiasing.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class AntialiasedLabel extends JLabel {
    private static final int AA_TRIGGER_FONT_SIZE = 14;

    private boolean antialiased;

    public AntialiasedLabel() {
        super();
    }

    public AntialiasedLabel(Icon image, int horizontalAlignment) {
        super(image, horizontalAlignment);
    }

    public AntialiasedLabel(Icon image) {
        super(image);
    }

    public AntialiasedLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
    }

    public AntialiasedLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
    }

    public AntialiasedLabel(String text) {
        super(text);
    }

    public void paintComponent(Graphics g) {
        if (antialiased) {
            ((Graphics2D) g).addRenderingHints(new RenderingHints(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
        }
        super.paintComponent(g);
    }

    private void setAntialiased(boolean newValue) {
        boolean oldValue = antialiased;
        antialiased = newValue;
        if (newValue != oldValue) {
            repaint();
        }
    }

    @Override
    public void setFont(Font font)
    {
        if (font.getSize() > AA_TRIGGER_FONT_SIZE) {
            setAntialiased(true);
        }
        super.setFont(font);
    }
}
