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

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.net.NodeSearcher;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.model.SearchNodeTableModel;
import de.dal33t.powerfolder.ui.widget.FilterTextField;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Search for members, use to "make friends".
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.7 $
 */
public class FindComputersPanel extends PFUIPanel {

    /** input field for search text */
    private FilterTextField searchInput;
    private ValueModel searchInputVM;
    /** the ui of the list of users that matches the search. */
    private JTable searchResult;

    private JScrollPane searchResultScroller;
    /** the table model holding the search results */
    private SearchNodeTableModel searchNodeTableModel;
    /** this panel */
    private JComponent panel;
    /** add friend */
    private Action addFriendAction;
    /** The Thread performing the search */
    private NodeSearcher searcher;
    /**
     * the toggle button that indicates if the offline users should be hidden or
     * not
     */
    private JCheckBox hideOffline;

    /** create a FriendsPanel */
    public FindComputersPanel(Controller controller) {
        super(controller);
        addFriendAction = new AddFriendAction();
    }

    /**
     * returns this ui component, creates it if not available
     */
    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            panel = createContentPanel();
        }
        return panel;
    }

    private void initComponents() {
        searchInput = new FilterTextField(12, Translation
                .getTranslation("find_computers_panel.search_for_computer.hint"),
                Translation
                .getTranslation("find_computers_panel.search_for_computer.tooltip"));
        searchInputVM = searchInput.getValueModel();
        searchInputVM.addValueChangeListener(new MySearchInputVMListener());

        searchNodeTableModel = new SearchNodeTableModel(getController());

        addFriendAction.setEnabled(false);

        searchResult = new FindComputersTable(searchNodeTableModel);
        searchResult.getSelectionModel().addListSelectionListener(
            new SearchResultSelectionListener());
        searchResult.addMouseListener(new PopupMenuOpener(createPopupMenu()));
        searchResult.addMouseListener(new DoubleClickAction(addFriendAction));

        searchResultScroller = new JScrollPane(searchResult);
        UIUtil.whiteStripTable(searchResult);
        UIUtil.removeBorder(searchResultScroller);
    }

    private JComponent createContentPanel() {
        FormLayout layout = new FormLayout("500",
            "pref, 3dlu, pref, min(pref;300)");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createSearchPanel(), cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 3));
        builder.add(searchResultScroller, cc.xy(1, 4));

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

        JButton connectButton = new JButton(getApplicationModel()
                .getActionModel().getConnectAction());

        FormLayout layout = new FormLayout("pref, 4dlu, pref, fill:pref:grow, 105dlu",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(hideOffline, cc.xy(1, 1));
        builder.add(connectButton, cc.xy(3, 1));
        builder.add(searchInput.getUIComponent(), cc.xy(5, 1));
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

        // Block until the search has been killed if there's one running
        if (searcher != null && searcher.isSearching()) {
            searcher.cancelSearch();
        }

        searcher = new NodeSearcher(getController(),
                searchText, searchNodeTableModel.getListModel(), true,
            hideOffline.isSelected());
        searcher.start();
    }

    /** called if button addFriend clicked or if selected in popupmenu */
    private void addFriend() {
        synchronized (searchNodeTableModel) {
            int[] selectedIndexes = searchResult.getSelectedRows();
            if (selectedIndexes != null && selectedIndexes.length > 0) {

                boolean askForFriendshipMessage = PreferencesEntry.
                        ASK_FOR_FRIENDSHIP_MESSAGE
                    .getValueBoolean(getController());
                if (askForFriendshipMessage) {

                    // Prompt for personal message.
                    String[] options = {
                            Translation
                                    .getTranslation("general.ok"),
                            Translation
                                    .getTranslation("general.cancel")};

                    FormLayout layout = new FormLayout("pref", "pref, 5dlu, pref, pref");
                    PanelBuilder builder = new PanelBuilder(layout);
                    CellConstraints cc = new CellConstraints();
                    String text;
                    if (selectedIndexes.length == 1) {
                        Object o = searchNodeTableModel.getDataAt(selectedIndexes[0]);
                        if (o instanceof Member) {
                            Member member = (Member) o;
                            String nick = member.getNick();
                            text = Translation.
                                getTranslation("friend.search.personal.message.text2",
                                        nick);
                        } else {
                            text = Translation.
                                getTranslation("friend.search.personal.message.text");
                        }
                    } else {
                        text = Translation.
                            getTranslation("friend.search.personal.message.text");
                    }
                    builder.add(new JLabel(text),
                            cc.xy(1, 1));
                    JTextArea textArea = new JTextArea();
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(400, 200));
                    builder.add(scrollPane, cc.xy(1, 3));
                    JPanel innerPanel = builder.getPanel();

                    NeverAskAgainResponse response = DialogFactory.genericDialog(
                            getController().getUIController().
                            getMainFrame().getUIComponent(),
                            Translation.getTranslation("friend.search.personal.message.title"),
                            innerPanel, options, 0, GenericDialogType.INFO,
                            Translation.getTranslation("general.neverAskAgain"));
                    if (response.getButtonIndex() == 0) { // == OK
                        String personalMessage = textArea.getText();
                        for (int index : selectedIndexes) {
                            Object item = searchNodeTableModel.getDataAt(index);
                            if (item instanceof Member) {
                                Member newFriend = (Member) item;
                                newFriend.setFriend(true, personalMessage);
                            }
                        }
                    }
                    if (response.isNeverAskAgain()) {
                        // dont ask me again
                        PreferencesEntry.ASK_FOR_FRIENDSHIP_MESSAGE
                            .setValue(getController(), false);
                    }
                } else {
                    // Send with no personal messages
                    for (int index : selectedIndexes) {
                        Object item = searchNodeTableModel.getDataAt(index);
                        if (item instanceof Member) {
                            Member newFriend = (Member) item;
                            newFriend.setFriend(true, null);
                        }
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
        int[] selectedIndexes = searchResult.getSelectedRows();

        // if at least one member selected
        for (int index : selectedIndexes) {
            Object item = searchNodeTableModel.getDataAt(index);
            if (item instanceof Member) {
                Member user = (Member) item;
                addFriendAction.setEnabled(!user.isFriend());
                break;
            }
        }
    }

    /** The hide offline user to perform on click on checkbox */
    private class HideOfflineAction extends BaseAction {
        private HideOfflineAction() {
            super("action_hide_offline", FindComputersPanel.this.getController());
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
}