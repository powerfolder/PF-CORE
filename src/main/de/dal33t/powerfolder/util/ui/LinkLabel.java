/* $Id: LinkLabel.java,v 1.4 2006/04/14 22:38:37 totmacherr Exp $
 */
package de.dal33t.powerfolder.util.ui;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.JLabel;

import de.dal33t.powerfolder.util.BrowserLauncher;

/**
 * A Label which opens a given link by click it
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class LinkLabel extends JLabel {
    private final String url;

    public LinkLabel(String text, String aUrl) {
        super("<html><font color=\"#00000\"><a href=\"" + aUrl + "\">" + text
            + "</a></font></html>");
        url = aUrl;
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    BrowserLauncher.openURL(url);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}