/* $Id$
 */
package de.dal33t.powerfolder.ui.friends;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.factories.Borders;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.model.FriendsNodeTableModel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DoubleClickAction;
import de.dal33t.powerfolder.util.ui.HasUIPanel;
import de.dal33t.powerfolder.util.ui.PopupMenuOpener;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Displays all friends in a list.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FriendsPanel extends PFUIComponent implements HasUIPanel {

    private FriendsQuickInfoPanel quickinfo;

    /** this panel */
    private JComponent panel;
    /** bottom toolbar */
    private JComponent toolbar;

    /** the ui of the list of friends. */
    private JTable friendsTable;
    private JScrollPane friendsPane;
    
    /**
     * the toggle button that indicates if the offline friends should be hidden
     * or not
     */
    private JCheckBox hideOffline;

    // Actions
    private ChatAction chatAction;
    private FindFriendAction findFriendsAction;
    private RemoveFriendAction removeFriendAction;

    /** create a FriendsPanel */
    public FriendsPanel(Controller controller) {
        super(controller);
    }

    public String getTitle() {
        return Translation.getTranslation("general.friendlist");
    }

    /** returns this ui component, creates it if not available * */
    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickinfo.getUIComponent());
            builder.setToolbar(toolbar);
            builder.setContent(friendsPane);
            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {
        // Actions
        chatAction = new ChatAction();
        findFriendsAction = new FindFriendAction();
        removeFriendAction = new RemoveFriendAction();

        quickinfo = new FriendsQuickInfoPanel(getController(), Translation
            .getTranslation("general.friendlist"));

        friendsTable = new JTable(getUIController().getNodeManagerModel()
            .getFriendsTableModel());
        friendsTable
            .setRowHeight(Icons.NODE_FRIEND_CONNECTED.getIconHeight() + 3);
        friendsTable.setShowGrid(false);
        friendsTable.setDefaultRenderer(Member.class,
            new MemberTableCellRenderer());
        // TODO Support multi selection. not possible atm
        friendsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        friendsTable.getTableHeader().setReorderingAllowed(true);
        // add sorting
        friendsTable.getTableHeader().addMouseListener(
            new TableHeaderMouseListener());

        friendsTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    updateActions();
                }
            });
        // Connect table to core
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    
        friendsPane = new JScrollPane(friendsTable);
        UIUtil.whiteStripTable(friendsTable);
        UIUtil.removeBorder(friendsPane);
        UIUtil.setZeroHeight(friendsPane);
        setupColumns();

        toolbar = createToolBar();

        friendsTable.addMouseListener(new PopupMenuOpener(createPopupMenu()));
        friendsTable.addMouseListener(new DoubleClickAction(chatAction));

        updateActions();
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = friendsTable.getWidth();
        // otherwise the table header may not be visible:
        friendsTable.getTableHeader().setPreferredSize(
            new Dimension(totalWidth, 20));

        TableColumn column = friendsTable.getColumn(friendsTable
            .getColumnName(0));
        column.setPreferredWidth(140);
        column = friendsTable.getColumn(friendsTable.getColumnName(1));
        column.setPreferredWidth(100);
//        column = friendsTable.getColumn(friendsTable.getColumnName(2));
//        column.setPreferredWidth(220);
        column = friendsTable.getColumn(friendsTable.getColumnName(2));
        column.setPreferredWidth(100);
        column = friendsTable.getColumn(friendsTable.getColumnName(3));
        column.setPreferredWidth(50);
        column.setMaxWidth(50);
    }

    /**
     * Creates the bottom toolbar
     * 
     * @return The bottom tool bar
     */
    private JComponent createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(chatAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(findFriendsAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(removeFriendAction));
        bar.addRelatedGap();
        
        hideOffline = new JCheckBox(new HideOfflineAction());
        hideOffline.setSelected(getUIController().getNodeManagerModel().hideOfflineFriends());
        
        bar.addFixed(hideOffline);
        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);
        return barPanel;
    }

    /**
     * Creates the popup menu
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu popupMenu = SimpleComponentFactory.createPopupMenu();
        popupMenu.add(chatAction);
        popupMenu.add(removeFriendAction);
        return popupMenu;
    }

    // App logic **************************************************************

    /** called if button chat clicked or if in popupmenu selected */
    private void chatWithSelected() {
        synchronized (getUIController().getNodeManagerModel()
            .getFriendsTableModel())
        {
            int[] selectedIndexes = friendsTable.getSelectedRows();
            // only single selection for chat
            if (selectedIndexes.length != 1) {
                return;
            }
            int index = selectedIndexes[0];
            Member member = (Member) getUIController().getNodeManagerModel()
                .getFriendsTableModel().getDataAt(index);
            if (!getUIController().getNodeManagerModel().hasMemberNode(member))
            {
                getUIController().getNodeManagerModel().addChatMember(member);
            }
            if (member.isCompleteyConnected()) {
                getController().getUIController().getControlQuarter()
                    .setSelected(member);
            }
        }

        updateActions();
    }

    /** called if button removeFriend clicked or if selected in popupmenu */
    private void removeFriend() {
        synchronized (getUIController().getNodeManagerModel()
            .getFriendsTableModel())
        {
            int[] selectedIndexes = friendsTable.getSelectedRows();
            for (int i = 0; i < selectedIndexes.length; i++) {
                int index = selectedIndexes[i];
                Object item = getUIController().getNodeManagerModel()
                    .getFriendsTableModel().getDataAt(index);
                if (item instanceof Member) {
                    Member newFriend = (Member) item;
                    newFriend.setFriend(false);
                }
            }
        }

        // Update actions
        updateActions();
    }

    /** called if button removeFriend clicked or if selected in popupmenu */
    private void findFriends() {
        // TODO Uarg, this is ugly (tm)
        getUIController().getControlQuarter().setSelected(
            getUIController().getNodeManagerModel().getNotInFriendsTreeNodes());
    }

    // Actions/Inner classes **************************************************

    /** The hideOfflineUserAction to perform, on click, on checkbox */
    private class HideOfflineAction extends BaseAction {
        public HideOfflineAction() {
            super("hideoffline", FriendsPanel.this.getController());            
            
        }

        public void actionPerformed(ActionEvent e) {
            // hides offline friends from the tree:
            getController().getUIController().getNodeManagerModel()
                .setHideOfflineFriends(hideOffline.isSelected());
        }
    }

    /** The Chat action to preform for button and popup menu item */
    private class ChatAction extends BaseAction {
        public ChatAction() {
            super("openchat", FriendsPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            chatWithSelected();
        }
    }

    /** Switches to the find friends panel */
    private class FindFriendAction extends BaseAction {
        public FindFriendAction() {
            super("findfriends", FriendsPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            findFriends();
        }
    }

    /** The removes friends action for button and popup menu Item */
    private class RemoveFriendAction extends BaseAction {
        public RemoveFriendAction() {
            super("removefriend", FriendsPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            removeFriend();
        }
    }

    /**
     * Listens for changes in the friendlist
     */
    private class MyNodeManagerListener implements NodeManagerListener {
        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            updateActions();
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            updateActions();
        }

        public void friendAdded(NodeManagerEvent e) {
            updateActions();
        }

        public void friendRemoved(NodeManagerEvent e) {
            updateActions();
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }

    }

    // UI Helper code *********************************************************

    /**
     * Updates the state of all actions upon the current selection
     */
    private void updateActions() {
        chatAction.setEnabled(false);
        removeFriendAction.setEnabled(false);
        int[] selectedIndexes = friendsTable.getSelectedRows();

        // only single selection for chat and with connected members
        if (selectedIndexes.length == 1) {
            int index = selectedIndexes[0];
            Object item = getUIController().getNodeManagerModel()
                .getFriendsTableModel().getDataAt(index);
            if (item instanceof Member) {
                Member member = (Member) item;
                chatAction.setEnabled(member.isCompleteyConnected());
            }
        }

        // if at least one member selected
        for (int i = 0; i < selectedIndexes.length; i++) {
            int index = selectedIndexes[i];
            Object item = getUIController().getNodeManagerModel()
                .getFriendsTableModel().getDataAt(index);
            if (item instanceof Member) {
                Member user = (Member) item;
                removeFriendAction.setEnabled(user.isFriend());
                break;
            }
        }
    }

    /**
     * Listner on table header, takes care about the sorting of table
     */
    private class TableHeaderMouseListener extends MouseAdapter {
        public final void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof FriendsNodeTableModel) {
                    FriendsNodeTableModel nodeTableModel = (FriendsNodeTableModel) model;
                    boolean freshSorted = nodeTableModel.sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        nodeTableModel.reverseList();
                    }
                }
            }
        }
    }

}
