package de.dal33t.powerfolder.ui.folder;

import java.awt.Component;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Displays the members of a Folder in a list, when a member is selected some
 * stats are displayed about the sync status of that member.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class MembersTab extends PFUIComponent implements FolderTab, FolderMembershipListener {
    private JPanel panel;
    private MemberSyncStatusPanel syncStatusPanel;
    private JList memberList;
    private JScrollPane memberListScroller;
    private Folder folder;
    private MemberListModel memberListModel;


    public MembersTab(Controller controller) {
        super(controller);
        memberListModel = new MemberListModel();
    }

    public String getTitle() {
        return Translation.getTranslation("myfolderstable.members");
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
        }
        return panel;
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
        memberList.setCellRenderer(new MemberListCellRenderer());
        memberListScroller = new JScrollPane(memberList);
        UIUtil.setZeroHeight(memberListScroller);

        syncStatusPanel = new MemberSyncStatusPanel(getController());
    }

    public void setFolder(Folder folder) {
        Folder oldFolder = this.folder;
        this.folder = folder;
        syncStatusPanel.setFolder(folder);
        Member[] members = folder.getMembers();
        memberListModel.setMembers(members);
        if (members.length > -1) {
            memberList.setSelectedIndex(0);
            syncStatusPanel.setMember(members[0]);
        }
        if (oldFolder != null) {
            oldFolder.removeMembershipListener(this);
        }

        folder.addMembershipListener(this);
    }

    private class MemberListCellRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {

            Member member = (Member) value;
            String newValue = member.getNick();
            if (member.isMySelf()) {
                newValue += " (" + Translation.getTranslation("navtree.me")
                    + ")";
            }
            super.getListCellRendererComponent(list, newValue, index,
                isSelected, cellHasFocus);
            setIcon(Icons.getIconFor(member));
            String tooltipText = "";

            if (member.isMySelf()) {
                tooltipText += Translation.getTranslation("navtree.me");
            } else if (member.isMutalFriend()) {
                tooltipText += Translation.getTranslation("member.friend");
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
                    + Translation.getTranslation("member.onlan") + ")";
            }
            setToolTipText(tooltipText);
            return this;
        }

    }

    private class MemberListModel extends AbstractListModel {
        List<Member> members = Collections.synchronizedList(new LinkedList<Member>());

        public void setMembers(Member[] membersArr) {
            synchronized (members) {
                this.members.clear();
                // java 1.5:
                // Collections.addAll(this.members, membersArr);
                for (int i = 0; i < membersArr.length; i++) {
                    members.add(membersArr[i]);
                }
            }
            fireContentsChanged(MembersTab.this, 0,
                membersArr.length - 1);
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
    
    public boolean fireInEventDispathThread() {
        return true;
    }
    
}
