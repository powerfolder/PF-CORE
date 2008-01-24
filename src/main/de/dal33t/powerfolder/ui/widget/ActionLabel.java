/* $Id: LinkLabel.java,v 1.4 2006/04/14 22:38:37 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.widget;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.wizard.PFWizard;

/**
 * A Label which executes the action when clicked.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class ActionLabel extends AntialiasedLabel {

    static final int HEADER_FONT_SIZE = 20;

    public ActionLabel(final Action action) {
        super("<html><font color=\"#00000\"><a href=\"#\">"
            + action.getValue(Action.NAME) + "</a></font></html>");
        Reject.ifNull(action, "Action listener is null");
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                action.actionPerformed(new ActionEvent(e.getSource(), 0,
                    "clicked"));
            }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void makeTitle() {
        SimpleComponentFactory.setFontSize(this, HEADER_FONT_SIZE);
    }

    @Override
    public void setText(String text) {
        super.setText("<html><font color=\"#00000\"><a href=\"#\">" + text
            + "</a></font></html>");
    }
}