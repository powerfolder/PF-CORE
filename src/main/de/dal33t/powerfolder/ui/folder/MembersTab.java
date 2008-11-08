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
package de.dal33t.powerfolder.ui.folder;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.actionold.ChangeFriendStatusAction;
import de.dal33t.powerfolder.ui.actionold.OpenChatAction;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.compare.MemberComparator;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Displays the members of a Folder in a list, when a member is selected some
 * stats are displayed about the sync status of that member.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class MembersTab extends PFUIComponent implements FolderTab,
    FolderMembershipListener
{

    private JPanel panel;
    private MemberSyncStatusPanel syncStatusPanel;
    private JList memberList;
    private JScrollPane memberListScroller;
    private Folder folder;
    private MemberListModel memberListModel;
    private JPopupMenu memberMenu;
    /** The currently selected item */
    private SelectionModel selectionModel;

    // Actions
    private OpenChatAction openChatAction;
    private ChangeFriendStatusAction changeFriendStatusAction;

    public MembersTab(Controller controller) {
        super(controller);
        memberListModel = new MemberListModel();
        selectionModel = new SelectionModel();
    }

    public String getTitle() {
        return Translation.getTranslation("my_folders_table.members");
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout(
                "fill:pref, 7dlu, fill:pref:grow", "pref, fill:pref:grow");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(memberListScroller, cc.xywh(1, 1, 1, 2));
            builder.add(syncStatusPanel.getUIComponent(), cc.xy(3, 1));
            builder.setBorder(Borders.DLU7_BORDER);
            panel = builder.getPanel();

            updateActions();
        }
        return panel;
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        // Popupmenus

        // create member menu for folder
        memberMenu = new JPopupMenu();
        openChatAction = new OpenChatAction(getController(),
            getSelectionModel());
        changeFriendStatusAction = new ChangeFriendStatusAction(
            getController(), getSelectionModel());
        memberMenu.add(openChatAction);
        memberMenu.add(changeFriendStatusAction);
    }

    /**
     * Returns the selection model, contains the model for the selected item on
     * the member list. If you need information about the parent of the current
     * selection see <code>getSelectionParentModel</code>
     * 
     * @see #getSelectionParentModel()
     * @return
     */
    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    private void initComponents() {
        memberList = new JList(memberListModel);
        memberList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        memberList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Member member = (Member) memberList.getSelectedValue();
                if (!e.getValueIsAdjusting()) {
                    syncStatusPanel.setMember(member);
                }
            }
        });
        // Selection listener to update selection model
        memberList.getSelectionModel().addListSelectionListener(
            new MemberSelectionAdapater());

        // build popup menus
        buildPopupMenus();
        memberList.setCellRenderer(new MemberListCellRenderer());
        memberList.addMouseListener(new MemberListener());
        memberListScroller = new JScrollPane(memberList);
        UIUtil.setZeroHeight(memberListScroller);

        syncStatusPanel = new MemberSyncStatusPanel(getController());
    }

    public void setFolder(Folder folder) {
        Reject.ifNull(folder, "Folder is null");
        Folder oldFolder = this.folder;
        this.folder = folder;
        syncStatusPanel.setFolder(folder);
        List<Member> members = MemberComparator.IN_GUI.sortedCopy(folder
            .getMembersAsCollection());
        memberListModel.setMembers(members);
        if (members.size() > -1) {
            memberList.setSelectedIndex(0);
            syncStatusPanel.setMember(members.get(0));
        }
        if (oldFolder != null) {
            oldFolder.removeMembershipListener(this);
        }

        folder.addMembershipListener(this);
    }

    /**
     * Updates the state of all actions upon the current selection
     */
    private void updateActions() {
        openChatAction.setEnabled(false);
        int selectedIndex = memberList.getSelectedIndex();

        // only single selection for chat and with connected members
        if (selectedIndex != -1) {
            Object item = memberListModel.getElementAt(selectedIndex);
            if (item instanceof Member) {
                Member member = (Member) item;
                openChatAction.setEnabled(member.isCompleteyConnected());
            }
        }
    }

    private final class MemberSelectionAdapater implements
        ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e) {
            if (memberList.getSelectedIndex() < 0) {
                return;
            }
            Member member = (Member) memberListModel.getElementAt(memberList
                .getSelectedIndex());
            selectionModel.setSelection(member);
            if (isFiner()) {
                logFiner("Selection: " + selectionModel.getSelection());
            }
        }
    }

    /**
     * member listener, cares for selection and popup menus
     */
    private class MemberListener extends MouseAdapter {

        public void mouseReleased(MouseEvent e) {
            updateActions();
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        public void mousePressed(MouseEvent e) {
            updateActions();
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            Object selection = memberList.getSelectedValue();
            if (selection == null) {
                return;
            }
            if (selection instanceof Member) {
                Member member = (Member) selection;
                // show menu
                if (!member.isMySelf())
                    memberMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }

    private class MemberListCellRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {

            Member member = (Member) value;
            String newValue = member.getNick();
            if (member.isMySelf()) {
                newValue += " (" + Translation.getTranslation("nav_tree.me")
                    + ")";
            }
            super.getListCellRendererComponent(list, newValue, index,
                isSelected, cellHasFocus);
            setIcon(Icons.getIconFor(member));
            String tooltipText = "";

            if (member.isMySelf()) {
                tooltipText += Translation.getTranslation("nav_tree.me");
                // } else if (member.isMutalFriend()) {
                // tooltipText += Translation.getTranslation("member.friend");
            } else if (member.isFriend()) {
                tooltipText += Translation
                    .getTranslation("member.non_mutual_friend");
            }

            if (member.isSupernode()) {
                tooltipText += " ("
                    + Translation.getTranslation("member.supernode") + ")";
            }
            if (member.isOnLAN()) {
                tooltipText += " ("
                    + Translation.getTranslation("member.on_lan") + ")";
            }
            setToolTipText(tooltipText);
            return this;
        }

    }

    private class MemberListModel extends AbstractListModel {
        List<Member> members = Collections
            .synchronizedList(new LinkedList<Member>());

        public void setMembers(List<Member> membersArr) {
            synchronized (members) {
                members.clear();
                members.addAll(membersArr);
            }
            fireContentsChanged(MembersTab.this, 0, membersArr.size() - 1);
        }

        public Object getElementAt(int index) {
            return members.get(index);
        }

        public int getSize() {
            return members.size();
        }

        void add(Member member) {
            if (members.indexOf(member) >= 0) {
                // Not add duplicate member
                return;
            }
            members.add(member);
            int index = members.indexOf(member);
            fireIntervalAdded(MembersTab.this, index, index);
        }

        void remove(Member member) {
            int index = members.indexOf(member);
            members.remove(member);
            fireIntervalRemoved(MembersTab.this, index, index);
        }
    }

    public void memberJoined(FolderMembershipEvent folderEvent) {
        memberListModel.add(folderEvent.getMember());
    }

    public void memberLeft(FolderMembershipEvent folderEvent) {
        memberListModel.remove(folderEvent.getMember());
    }

    public boolean fireInEventDispatchThread() {
        return true;
    }

}
