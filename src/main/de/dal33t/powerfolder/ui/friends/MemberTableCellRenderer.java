package de.dal33t.powerfolder.ui.friends;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Helper class which renders the search results
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
class MemberTableCellRenderer extends DefaultTableCellRenderer {
    public Component getTableCellRendererComponent(JTable table,
        Object value, boolean isSelected, boolean hasFocus, int row,
        int column)
    {
        setHorizontalAlignment(SwingConstants.LEFT);
        setIcon(null);
        if (value instanceof String) {// no user found
            if (column != 0) {
                value = "";
            }
        } else if (value instanceof Member) {
            Member member = (Member) value;
            switch (column) {
                case 0 : {
                    value = member.getNick();
                    setIcon(Icons.getIconFor(member));
                    break;
                }
                case 1 : {
                    if (member.isCompleteyConnected()) {
                        value = Translation
                            .getTranslation("friendspanel.connected");
                    } else if (member.isConnectedToNetwork()) {
                        value = Translation
                            .getTranslation("friendspanel.currently_online");
                    } else {
                        value = Format.formatDate(member.getLastConnectTime());
                    }
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                }
                case 2 : {
                    value = replaceNullWithNA(member.getHostName());
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                }
                case 3 : {
                    value = replaceNullWithNA(member.getIP());
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                }
                case 4 : {
                    JCheckBox box = new JCheckBox("", member.isOnLAN());
                    box.setBackground(Color.WHITE);
                    box.setHorizontalAlignment(SwingConstants.CENTER);
                    return box;
                }

            }
        } else {
            throw new IllegalStateException("don't know how to render this");
        }

        return super.getTableCellRendererComponent(table, value,
            isSelected, hasFocus, row, column);
    }
    
    private final static String replaceNullWithNA(String original) {
        return original == null ? Translation
            .getTranslation("friendspanel.n_a") : original;
    }
}