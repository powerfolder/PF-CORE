package de.dal33t.powerfolder.ui.friends;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.message.SearchNodeRequest;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.DoubleClickAction;
import de.dal33t.powerfolder.util.ui.PopupMenuOpener;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Search for members, use to "make friends".
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.7 $
 */
public class FriendsPanel extends PFUIComponent {
        
    /** for convienience a reference to the member representing myself */
    private Member myself;
    /** input field for search text */
    private JTextField searchInput;
    /** the ui of the list of users that matches the search. */
    private JTable searchResult;

    /** The quick info panle for friends */
    private FriendsQuickInfoPanel quickinfo;
    
    private JScrollPane searchResultScroller;
    /** the table model holding the search results */
    private NodeTableModel nodeTableModel;
    /** this panel */
    private JPanel panel;
    /** search */
    private Action searchAction;
    /** add friend */
    private Action addFriendAction;
    /** chat with this user */
    private Action chatAction;
    /** bottom toolbar */
    private JComponent toolbar;
    /** The Thread performing the search */
    private FriendSearcher searcher;
    
    /** create a FriendsPanel */
    public FriendsPanel(Controller controller) {
        super(controller);
        myself = controller.getMySelf();    
    }

    /** returns this ui component, creates it if not available * */
    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            // layout:
            FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, pref, pref, 3dlu, pref, fill:pref:grow, pref, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(quickinfo.getUIComponent(), cc.xy(1, 1));
            builder.addSeparator(null, cc.xy(1, 2));

            builder.add(createSearchPanel(), cc.xy(1, 3));
            JLabel searchTitle = builder.addTitle(Translation
                .getTranslation("friendspanel.search_results"), cc.xy(1, 5));
            searchTitle
                .setBorder(Borders.createEmptyBorder("0, 4dlu, 4dlu, 0"));

            builder.add(searchResultScroller, cc.xy(1, 6));
            builder.addSeparator(null, cc.xy(1, 7));
            builder.add(toolbar, cc.xy(1, 8));
            panel = builder.getPanel();
        }
        return panel;
    }

    public String getTitle() {
        return Translation.getTranslation("title.search.users");
    }
    
    private void initComponents() {
        quickinfo = new FriendsQuickInfoPanel(getController());
        
        searchInput = new JTextField(15);
        searchInput.setEditable(true);
        nodeTableModel = new NodeTableModel(getController());
        searchResult = new JTable(nodeTableModel);
        searchResult.setRowHeight(Icons.NODE.getIconHeight() + 3);
        searchResult.setDefaultRenderer(Member.class,
            new MemberTableCellRenderer());
        searchResult.getSelectionModel().addListSelectionListener(
            new SearchResultSelectionListener());
        searchResult.getTableHeader().setReorderingAllowed(false);

        addFriendAction = new AddFriendAction();
        addFriendAction.setEnabled(false);
        chatAction = new ChatAction();
        chatAction.setEnabled(false);
        toolbar = createToolBar();

        searchInput.addKeyListener(new SearchInputKeyListener());
        searchResult.addMouseListener(new PopupMenuOpener(createPopupMenu()));
        searchResult.addMouseListener(new DoubleClickAction(addFriendAction));

        searchResultScroller = new JScrollPane(searchResult);
        Util.whiteStripTable(searchResult);
        Util.removeBorder(searchResultScroller);
        Util.setZeroHeight(searchResultScroller);

        setupColumns();
    }

    /**
     * Creates the popup menu
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu popupMenu = SimpleComponentFactory.createPopupMenu();
        popupMenu.add(addFriendAction);
        popupMenu.add(chatAction);
        return popupMenu;
    }

    private JPanel createSearchPanel() {
        searchAction = new SearchAction();
        searchAction.setEnabled(false);

        JButton searchButton = new JButton(searchAction);
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGlue();
        bar.addGridded(searchButton);

        FormLayout layout = new FormLayout("pref, 3dlu, pref, 7dlu, pref",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(new JLabel(Translation
            .getTranslation("friendspanel.search_for_user")), cc.xy(1, 1));
        builder.add(searchInput, cc.xy(3, 1));
        builder.add(bar.getPanel(), cc.xy(5, 1));

        builder.setBorder(Borders.DLU4_BORDER);
        return builder.getPanel();

    }

    /**
     * Creates the bottom toolbar
     * 
     * @return The bottom tool bar
     */
    private JComponent createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(addFriendAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(chatAction));

        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);
        return barPanel;
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = searchResult.getWidth();
        // otherwise the table header may not be visible:
        searchResult.getTableHeader().setPreferredSize(
            new Dimension(totalWidth, 20));

        TableColumn column = searchResult.getColumn(searchResult
            .getColumnName(0));
        column.setPreferredWidth(140);
        column = searchResult.getColumn(searchResult.getColumnName(1));
        column.setPreferredWidth(100);
        column = searchResult.getColumn(searchResult.getColumnName(2));
        column.setPreferredWidth(220);
        column = searchResult.getColumn(searchResult.getColumnName(3));
        column.setPreferredWidth(100);
        column = searchResult.getColumn(searchResult.getColumnName(4));
        column.setPreferredWidth(50);
        column.setMaxWidth(50);
    }

    /** preform a search, interrupts a search if still running */
    private void search() {
        // Block until the search has been killed if there's one running
        if (searcher != null && searcher.isSearching())
            searcher.cancelSearch();
        
        searcher = new FriendSearcher(searchInput.getText().trim());
        searcher.start();
    }

    /** called if button addFriend clicked or if selected in popupmenu */
    private void addFriend() {
        synchronized (nodeTableModel) {
            int[] selectedIndexes = searchResult.getSelectedRows();
            List toRemove = new LinkedList();
            for (int i = 0; i < selectedIndexes.length; i++) {
                int index = selectedIndexes[i];
                Object item = nodeTableModel.getDataAt(index);
                if (item instanceof Member) {
                    Member newFriend = (Member) item;
                    newFriend.setFriend(true);
                    toRemove.add(newFriend);
                }
            }
//            for (int i = 0; i < toRemove.size(); i++) {
//                nodeTableModel.remove((Member) toRemove.get(i));
//            }

        }
//      Update actions
        updateActions();
    }

    /** called if button chat clicked or if in popupmenu selected */
    private void chat() {
        synchronized (nodeTableModel) {
            int[] selectedIndexes = searchResult.getSelectedRows();
            // only single selection for chat
            if (selectedIndexes.length != 1) {
                return;
            }
            int index = selectedIndexes[0];
            Member member = (Member) nodeTableModel.getDataAt(index);
            if (!getController().getNodeManager().hasMemberNode(member))
                getController().getNodeManager().addChatMember(member);
            if (member.isCompleteyConnected()) {
                getController().getUIController().getControlQuarter().setSelected(
                    member);
                getController().getUIController().getInformationQuarter().displayChat(member);
            }
        }
    }
    
    // UI Helper code *********************************************************
    
    /**
     * Updates the state of all actions upon the current selection
     */
    private void updateActions() {
        chatAction.setEnabled(false);
        addFriendAction.setEnabled(false);
        int[] selectedIndexes = searchResult.getSelectedRows();

        // only single selection for chat and with connected members
        if (selectedIndexes.length == 1) {
            int index = selectedIndexes[0];
            Object item = nodeTableModel.getDataAt(index);
            if (item instanceof Member) {
                Member member = (Member) item;
                chatAction.setEnabled(member.isCompleteyConnected());
            }
        }

        // if at least one member selected
        for (int i = 0; i < selectedIndexes.length; i++) {
            int index = selectedIndexes[i];
            Object item = nodeTableModel.getDataAt(index);
            if (item instanceof Member) {
                Member user = (Member) item;
                addFriendAction.setEnabled(!user.isFriend());
                break;
            }
        }
    }

    /** The Chat action to preform for button and popup menu item */
    private class ChatAction extends BaseAction {
        public ChatAction() {
            super("openchat", FriendsPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            chat();
        }
    }

    /** The Add friends action for button and popup menu Item */
    private class AddFriendAction extends BaseAction {
        public AddFriendAction() {
            super("addfriend", FriendsPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            addFriend();
        }
    }

    /** The action to preform if the search button is clicked */
    private class SearchAction extends BaseAction {
        public SearchAction() {
            super("searchfriends", FriendsPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            search();
        }
    }

    /**
     * listens to keys in the search input updates the searchAction state if
     * enough chars are available and preforms a search on enter key
     * 
     * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
     */
    private class SearchInputKeyListener implements KeyListener {
        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
            // minimal 3 chars for enabled search button
            searchAction.setEnabled(searchInput.getText().trim().length() >= 3);
        }

        public void keyTyped(KeyEvent e) {
            char keyTyped = e.getKeyChar();
            if (keyTyped == '\n') { // enter key = search
                search();
            }
        }
    }

    /**
     * Helper class which tracks the selections int the search Results and
     * updates the actions to the correct state
     * 
     * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
     */
    private class SearchResultSelectionListener implements
        ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e) {
            updateActions();
        }
    }

    /**
     * Helper class which renders the search results
     * 
     * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
     */
    private class MemberTableCellRenderer extends DefaultTableCellRenderer {
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
                        if (member.isCompleteyConnected()
                            || member.isConnectedToNetwork())
                        {
                            value = Translation
                                .getTranslation("friendspanel.currently_online");
                        } else {
                            value = Format.formatDate(member
                                .getLastConnectTime());
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
    }

    private final static String replaceNullWithNA(String original) {
        return original == null ? Translation
            .getTranslation("friendspanel.n_a") : original;
    }
    
    /**
     * This class searches friends matching a given pattern.
     * @author Dennis "Dante" Waldherr
     */
    private final class FriendSearcher extends Thread {
        private String pattern;
        /** indicates that we want to interrupt a search */
        private boolean stopSearching = false;
        
        private final String noUser = Translation.getTranslation("friendsearch.no_user_found"); 
        
        private NodeManagerListener nodeListener;
        private Queue<Member> checkList;
        
        public FriendSearcher(String pattern) {
            super("FriendSearcher searching for " + pattern);
            this.pattern = pattern;
        }

        /**
         * Cancels this search.
         * This method will block until the searching has stopped.
         */
        public void cancelSearch() {
            try {
                stopSearching = true;
                synchronized (this) {
                    // Wake the searcher Thread
                    notifyAll();
                }
                // give some time to shutdown the running search
                join(500);
                if (isSearching()) { // Searching didn't stop within 500ms
                    // Interrupt it
                    interrupt();
                }
            } catch (InterruptedException ie) {
                // This might mean that 2 Threads called cancelSearch()
                log().error(ie);
            }
        }
        
        /**
         * @return true if this Thread is still searching new members
         */
        public boolean isSearching() {
            return !getState().equals(State.TERMINATED);
        }
        
        private void init() {
            checkList = new LinkedList<Member>();

            nodeListener = new NodeManagerListener() {

                public void nodeRemoved(NodeManagerEvent e) {
                }

                public void nodeAdded(NodeManagerEvent e) {
                    synchronized (FriendSearcher.this) {
                        checkList.add(e.getNode());
                        FriendSearcher.this.notifyAll();
                    }
                }

                public void nodeConnected(NodeManagerEvent e) {
                }

                public void nodeDisconnected(NodeManagerEvent e) {
                }

                public void friendAdded(NodeManagerEvent e) {
                }

                public void friendRemoved(NodeManagerEvent e) {
                }

                public void settingsChanged(NodeManagerEvent e) {
                }
                
            };
            getController().getNodeManager().addNodeManagerListener(nodeListener);
        }

        private void done() {
            getController().getNodeManager().removeNodeManagerListener(nodeListener);
            synchronized (this) {
                notifyAll();
            }
        }
        
        private void checkAndAddMember(Member member) {
            if (member.equals(myself) || !member.matches(pattern))
                return;
            nodeTableModel.remove(noUser);
            nodeTableModel.add(member);
        }
        
        public void run() {
            init();
            try {
                if (pattern.length() >= 3) {
                    // this part is generaly very fast doesn't need interruption
                    searchAction.setEnabled(false);
                    searchInput.setEnabled(false);
                    nodeTableModel.clear();
                    Member[] members = getController().getNodeManager().getNodes();
                    for (int i = 0; i < members.length; i++) {
                        Member member = members[i];
                        if (!member.equals(myself) && !member.isFriend()) {
                            if (member.matchesFast(pattern)) {
                                nodeTableModel.add(member);
                            }
                        }
                    }
                    searchAction.setEnabled(true);
                    searchInput.setEnabled(true);
                    quickinfo.setUsersFound(nodeTableModel.getRowCount());

                    for (int i = 0; i < members.length; i++) {
                        // This part maybe slow (the first time) needs
                        // interuption
                        if (stopSearching) {
                            // Hej a new search!, stop this one
                            break;
                        }
                        Member member = members[i];
                        if (!member.equals(myself)) {
                            if (member.matches(pattern)) {
                                nodeTableModel.add(member);
                            }
                        }
                    }
                    
                    quickinfo.setUsersFound(nodeTableModel.getRowCount());
                    if (nodeTableModel.getRowCount() == 0) {
                        //add "No user found text"
                        nodeTableModel.add(noUser);
                    }
                }
                searchInput.requestFocusInWindow();

                // Ask connected SuperNodes for search results
                Message msg = new SearchNodeRequest(pattern);
                for (Member m: getController().getNodeManager().getValidNodes())
                    if (m.isCompleteyConnected())
                        m.sendMessageAsynchron(msg, null);
                
                while (!stopSearching) {
                    synchronized (this) {
                        while (!checkList.isEmpty())
                            checkAndAddMember(checkList.remove());
                        quickinfo.setUsersFound(nodeTableModel.getRowCount());
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            log().error(e);
                        }
                    }
                }
            } finally {
                done();
            }
        }
    }

}