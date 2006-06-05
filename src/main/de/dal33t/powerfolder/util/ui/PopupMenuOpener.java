package de.dal33t.powerfolder.util.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * Helper class which opens a popmenu when requested (right-mouseclick)
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class PopupMenuOpener extends MouseAdapter {
    private JPopupMenu popupMenu;

    public PopupMenuOpener(JPopupMenu popupMenu) {
        if (popupMenu == null) {
            throw new NullPointerException("Popupmenu is null");
        }
        this.popupMenu = popupMenu;
    }

    public void mousePressed(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            showContextMenu(evt);
        }
    }

    public void mouseReleased(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            showContextMenu(evt);
        }
    }

    private void showContextMenu(MouseEvent evt) {
        popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
    }
}