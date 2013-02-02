/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder.ui.friends;

import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.InetSocketAddress;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.net.NodeSearcher;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.action.ConnectAction;
import de.dal33t.powerfolder.ui.model.SearchNodeTableModel;
import de.dal33t.powerfolder.ui.widget.FilterTextField;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.util.PopupMenuOpener;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;

/**
 * Search for members, use to "make friends".
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.7 $
 */
public class FindComputersPanel extends PFUIComponent {

    /** input field for search text */
    private FilterTextField searchInput;
    private ValueModel searchInputVM;
    /** the ui of the list of users that matches the search. */
    private JTable searchResultTable;

    private JScrollPane searchResultScroller;
    /** the table model holding the search results */
    private SearchNodeTableModel searchNodeTableModel;
    /** this panel */
    private JComponent panel;
    /** add friend */
    private Action addFriendAction;
    /** to connect */
    private Action connectAction;
    /** The Thread performing the search */
    private NodeSearcher searcher;
    /** Label to show when there are no results */
    private JLabel noResultsLabel;
    /**
     * the toggle button that indicates if the offline users should be hidden or
     * not
     */
    private JCheckBox hideOffline;

    /**
     * create a FriendsPanel
     * 
     * @param controller
     */
    public FindComputersPanel(Controller controller) {
        super(controller);
        addFriendAction = new AddFriendAction();
        connectAction = new MyConnectAction(controller);
        noResultsLabel = new JLabel(Translation
            .getTranslation("friend_search.no_computers_found"),
            SwingConstants.CENTER);
        noResultsLabel.setEnabled(false);
    }

    private void showResults(final Boolean valuesDisplayed) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                searchResultScroller.setVisible(valuesDisplayed);
                noResultsLabel.setVisible(!valuesDisplayed);
            }
        });
    }

    /**
     * returns this ui component, creates it if not available
     */
    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            panel = createContentPanel();
            searchInput.requestFocus();
            showResults(false);
        }
        return panel;
    }

    public void cancelSearch() {
        // Block until the search has been killed if there's one running
        if (searcher != null && searcher.isSearching()) {
            searcher.cancelSearch();
        }
    }

    private void initComponents() {
        searchInput = new FilterTextField(
            15,
            Translation
                .getTranslation("find_computers_panel.search_for_computer.hint"),
            Translation
                .getTranslation("find_computers_panel.search_for_computer.tooltip"));
        searchInputVM = searchInput.getValueModel();
        searchInputVM.addValueChangeListener(new MySearchInputVMListener());

        searchNodeTableModel = new SearchNodeTableModel(getController());

        addFriendAction.setEnabled(false);

        searchResultTable = new FindComputersTable(searchNodeTableModel);
        searchResultTable.getSelectionModel().addListSelectionListener(
            new SearchResultSelectionListener());
        searchResultTable.addMouseListener(new PopupMenuOpener(
            createPopupMenu()));
        searchResultTable.addMouseListener(new DoubleClickAction(
            addFriendAction));

        searchResultTable.registerKeyboardAction(new SelectAllAction(),
            KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK),
            JComponent.WHEN_FOCUSED);

        searchResultScroller = new JScrollPane(searchResultTable);
        UIUtil.whiteStripTable(searchResultTable);
        UIUtil.removeBorder(searchResultScroller);

        searchNodeTableModel.getListModel().addListDataListener(
            new ListDataListener() {
                public void contentsChanged(ListDataEvent e) {
                    showResults(!searchNodeTableModel.getListModel().isEmpty());
                }

                public void intervalAdded(ListDataEvent e) {
                    showResults(!searchNodeTableModel.getListModel().isEmpty());
                }

                public void intervalRemoved(ListDataEvent e) {
                    showResults(!searchNodeTableModel.getListModel().isEmpty());

                }
            });
    }

    private JComponent createContentPanel() {
        FormLayout layout = new FormLayout("fill:600:grow",
            "pref, 3dlu, pref, 3dlu, fill:300:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createSearchPanel(), cc.xy(1, 1));

        builder.addSeparator(null, cc.xy(1, 3));

        // searchResultScroller and noResultsLabel share the same slot.
        builder.add(searchResultScroller, cc.xy(1, 5));
        builder.add(noResultsLabel, cc.xy(1, 5));

        updateActions();

        return builder.getPanel();
    }

    /**
     * Creates the popup menu
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu popupMenu = SimpleComponentFactory.createPopupMenu();
        popupMenu.add(addFriendAction);
        return popupMenu;
    }

    private JPanel createSearchPanel() {
        hideOffline = new JCheckBox(new HideOfflineAction());
        hideOffline.setSelected(PreferencesEntry.FRIEND_SEARCH_HIDE_OFFLINE
            .getValueBoolean(getController()));
        hideOffline.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                // save to pref if hide off line checkbox clicked
                PreferencesEntry.FRIEND_SEARCH_HIDE_OFFLINE.setValue(
                    getController(), hideOffline.isSelected());
            }
        });

        FormLayout layout = new FormLayout("0:grow, pref, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(hideOffline, cc.xy(2, 1));
        builder.add(searchInput.getUIComponent(), cc.xy(4, 1));
        return builder.getPanel();

    }

    /** perform a search, interrupts a search if still running */
    private void search() {

        Object o = searchInputVM.getValue();
        if (o == null) {
            return;
        }
        String searchText = ((String) o).trim().toLowerCase();
        if (searchText.length() < 3) {
            return;
        }

        cancelSearch();

        searcher = new NodeSearcher(getController(), searchText,
            searchNodeTableModel.getListModel(), false, hideOffline
                .isSelected());
        searcher.start();
    }

    /** called if button addFriend clicked or if selected in popupmenu */
    private void addFriend() {
        synchronized (searchNodeTableModel) {
            int[] selectedIndexes = searchResultTable.getSelectedRows();
            if (selectedIndexes != null && selectedIndexes.length > 0) {

                for (int index : selectedIndexes) {
                    Object item = searchNodeTableModel.getDataAt(index);
                    if (item instanceof Member) {
                        Member newFriend = (Member) item;
                        newFriend.setFriend(true, null);
                    }
                }
            }
        }
        // Update actions
        updateActions();
        // refresh search (removes the new friend)
        search();
    }

    // UI Helper code *********************************************************

    /**
     * Updates the state of all actions upon the current selection
     */
    private void updateActions() {
        addFriendAction.setEnabled(false);
        int[] selectedIndexes = searchResultTable.getSelectedRows();

        // if at least one member selected
        boolean enabled = false;
        for (int index : selectedIndexes) {
            Object item = searchNodeTableModel.getDataAt(index);
            if (item instanceof Member) {
                Member user = (Member) item;
                if (!user.isFriend()) {
                    enabled = true;
                    break;
                }
            }
        }
        addFriendAction.setEnabled(enabled);
    }

    private class MyConnectAction extends ConnectAction {
        private MyConnectAction(Controller controller) {
            super(controller);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = searchResultTable.getSelectedRow();
            if (row >= 0) {
                Member node = (Member) searchNodeTableModel.getValueAt(row, 0);
                if (node != null) {
                    InetSocketAddress addr = node.getReconnectAddress();
                    String conStr = addr.getAddress().getHostAddress();
                    if (addr.getPort() != ConnectionListener.DEFAULT_PORT) {
                        conStr += ':';
                        conStr += addr.getPort();
                    }
                    setInputConnect(conStr);
                }
            }
            super.actionPerformed(e);
        }
    }

    /** The hide offline user to perform on click on checkbox */
    private class HideOfflineAction extends BaseAction {
        private HideOfflineAction() {
            super("action_hide_offline", FindComputersPanel.this
                .getController());
        }

        public void actionPerformed(ActionEvent e) {
            search();
        }
    }

    /** The Add friends action for button and popup menu Item */
    private class AddFriendAction extends BaseAction {
        private AddFriendAction() {
            super("action_add_friend", FindComputersPanel.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            addFriend();
        }
    }

    /**
     * listens to keys in the search input updates the searchAction state if
     * enough chars are available and preforms a search on enter key
     * 
     * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
     */
    private class MySearchInputVMListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            search();
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

    public Action getAddFriendAction() {
        return addFriendAction;
    }

    public Action getConnectAction() {
        return connectAction;
    }

    private class SelectAllAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            searchResultTable.selectAll();
        }
    }

    private static class DoubleClickAction extends MouseAdapter {
        private Action action;

        DoubleClickAction(Action action) {
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


}