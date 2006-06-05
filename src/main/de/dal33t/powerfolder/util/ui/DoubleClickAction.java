package de.dal33t.powerfolder.util.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.SwingUtilities;

/**
 * Mouselistener, which perfoms the action, when clicked
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class DoubleClickAction extends MouseAdapter {
    private Action action;

    public DoubleClickAction(Action action) {
        if (action == null) {
            throw new NullPointerException("Action is null");
        }
        this.action = action;
    }

    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2
            && action.isEnabled())
        {
            action.actionPerformed(null);
        }
    }
}

