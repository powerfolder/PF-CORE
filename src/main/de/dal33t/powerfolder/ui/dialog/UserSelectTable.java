package de.dal33t.powerfolder.ui.dialog;

import de.dal33t.powerfolder.ui.friends.UserSelectTableCellRenderer;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.Member;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import java.awt.Container;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Table showing selected users in invitation wizard dialog
 */
public class UserSelectTable extends JTable {

    public UserSelectTable(TableModel dm) {
        super(dm);
        setShowGrid(false);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setRowHeight(Icons.NODE_NON_FRIEND_CONNECTED.getIconHeight() + 3);
        setDefaultRenderer(Member.class, new UserSelectTableCellRenderer());
    }

    /**
     * Make the height same as viewport (JScrollPane) if bigger than this.
     *
     * @return
     */
    public boolean getScrollableTracksViewportHeight() {
    Container viewport = getParent();
        return viewport instanceof JViewport &&
                getPreferredSize().height < viewport.getHeight();
    }

    /**
     * Returns all the members that the user has selected.
     * @return
     */
    public Collection<Member> getSelectedMembers() {
        Collection<Member> selectedMembers = new ArrayList<Member>();
        for (int i = 0; i < getRowCount(); i++) {
            if (isRowSelected(i)) {
                Object o = getValueAt(i, 0);
                System.out.println(o);
                if (o instanceof Member) {
                    Member member = (Member) o;
                    selectedMembers.add(member);
                }
            }
        }
        return selectedMembers;
    }
}
