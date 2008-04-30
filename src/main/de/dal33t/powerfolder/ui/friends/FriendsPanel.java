/* $Id$
 */
package de.dal33t.powerfolder.ui.friends;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.factories.Borders;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.ChangeFriendStatusAction;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Displays all friends in a list.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FriendsPanel extends PFUIPanel {

    private FriendsQuickInfoPanel quickinfo;

    /** this panel */
    private JComponent panel;
    /** bottom toolbar */
    private JComponent toolbar;

    /** the ui of the list of friends. */
    private FriendsTable friendsTable;
    private JScrollPane friendsPane;

    /**
     * the toggle button that indicates if the offline friends should be hidden
     * or not
     */
    private JCheckBox hideOfflineCB;

    /**
     * the toggle button that indicates if all lan users should be displayed
     * or not
     */
    private JCheckBox includeLanCB;

    // Actions
    private ChatAction chatAction;
    private Action findFriendsAction;
    private ChangeFriendStatusAction changeFriendStatusAction;
    private SelectionModel memberSelectionModel;


    public FriendsPanel(Controller controller) {
        super(controller);

        memberSelectionModel = new SelectionModel();
        changeFriendStatusAction = new ChangeFriendStatusAction(controller,
            memberSelectionModel);

    }

    public String getTitle() {
        return Translation.getTranslation("general.friendlist");
    }

    /** @return this ui component, creates it if not available * */
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
        findFriendsAction = getUIController().getFindFriendAction();

        quickinfo = new FriendsQuickInfoPanel(getController(), Translation
            .getTranslation("general.friendlist"));

        friendsTable = new FriendsTable(getUIController().getNodeManagerModel()
            .getFriendsTableModel());
        

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
        friendsTable.setupColumns();

        toolbar = createToolBar();

        friendsTable.addMouseListener(new PopupMenuOpener(createPopupMenu()));
        friendsTable.addMouseListener(new DoubleClickAction(chatAction));

        updateActions();
    }

    /**
     * Creates the bottom toolbar
     * 
     * @return The bottom tool bar
     */
    private JComponent createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(findFriendsAction));
        bar.addUnrelatedGap();
        bar.addGridded(new JButton(chatAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(changeFriendStatusAction));
        bar.addRelatedGap();

        NodeManagerModel nmModel = getUIController().getNodeManagerModel();

        hideOfflineCB = BasicComponentFactory.createCheckBox(nmModel
            .getHideOfflineUsersModel(), Translation
            .getTranslation("hideoffline.name"));

        bar.addFixed(hideOfflineCB);
        bar.addRelatedGap();

        includeLanCB = BasicComponentFactory.createCheckBox(nmModel
            .getIncludeLanUsersModel(), Translation
            .getTranslation("include_lan.name"));

        bar.addFixed(includeLanCB);
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
        popupMenu.add(changeFriendStatusAction);
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
            for (int index : selectedIndexes) {
                Object item = getUIController().getNodeManagerModel()
                        .getFriendsTableModel().getDataAt(index);
                if (item instanceof Member) {
                    Member newFriend = (Member) item;
                    newFriend.setFriend(false, null);
                }
            }
        }

        // Update actions
        updateActions();
    }

    // Actions/Inner classes **************************************************

    /** The Chat action to preform for button and popup menu item */
    private class ChatAction extends BaseAction {
        public ChatAction() {
            super("openchat", FriendsPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            chatWithSelected();
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

        public void startStop(NodeManagerEvent e) {
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
        changeFriendStatusAction.setEnabled(false);
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
        for (int index : selectedIndexes) {
            Object item = getUIController().getNodeManagerModel()
                    .getFriendsTableModel().getDataAt(index);
            if (item instanceof Member) {
                Member user = (Member) item;
                changeFriendStatusAction.setEnabled(true);
                memberSelectionModel.setSelection(user);
                break;
            }
        }
    }

}
