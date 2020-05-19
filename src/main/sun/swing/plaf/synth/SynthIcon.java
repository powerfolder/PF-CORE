package sun.swing.plaf.synth;

import javax.swing.*;
import javax.swing.plaf.synth.SynthContext;
import java.awt.*;

public abstract class SynthIcon implements Icon {
    public static int getIconWidth(Icon icon, SynthContext context) {
        if (icon == null) {
            return 0;
        }
        if (icon instanceof SynthIcon) {
            return ((SynthIcon)icon).getIconWidth(context);
        }
        return icon.getIconWidth();
    }

    public static int getIconHeight(Icon icon, SynthContext context) {
        if (icon == null) {
            return 0;
        }
        if (icon instanceof SynthIcon) {
            return ((SynthIcon)icon).getIconHeight(context);
        }
        return icon.getIconHeight();
    }

    public static void paintIcon(Icon icon, SynthContext context, Graphics g,
                                 int x, int y, int w, int h) {
        if (icon instanceof SynthIcon) {
            ((SynthIcon)icon).paintIcon(context, g, x, y, w, h);
        }
        else if (icon != null) {
            icon.paintIcon(context.getComponent(), g, x, y);
        }
    }

    public abstract void paintIcon(SynthContext context, Graphics g, int x,
                                   int y, int w, int h);

    public abstract int getIconWidth(SynthContext context);

    public abstract int getIconHeight(SynthContext context);

    public void paintIcon(Component c, Graphics g, int x, int y) {
        paintIcon(null, g, x, y, 0, 0);
    }

    public int getIconWidth() {
        return getIconWidth(null);
    }

    public int getIconHeight() {
        return getIconHeight(null);
    }
}