/* $Id: PFListCellRenderer.java,v 1.4 2005/04/14 14:38:42 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.render;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;

/**
 * A Default list cell renderer for several powerfolder elements
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class PFListCellRenderer extends DefaultListCellRenderer {

    public PFListCellRenderer() {
        super();
    }

    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus)
    {
        super.getListCellRendererComponent(list, value, index, isSelected,
            cellHasFocus);

        if (value instanceof Member) {
            Member node = (Member) value;

            // Get icon
            setIcon(Icons.getIconFor(node));

            // General stuff (text)
            String text = node.getNick();
           
            if (node.isOnLAN()) {
                text += " (" + Translation.getTranslation("general.localnet") + ")";
            }
            setText(text);
        } else if (value instanceof SyncProfile) {
            // Sync profile
            SyncProfile syncProfile = (SyncProfile) value;
            String text = Translation.getTranslation(syncProfile
                .getTranslationId());
            setText(text);
        }

        return this;
    }
}