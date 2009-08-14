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
 * $Id: ComputersList.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.computers;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.ExpansionEvent;
import de.dal33t.powerfolder.event.ExpansionListener;
import de.dal33t.powerfolder.event.NodeManagerModelListener;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.util.Translation;

public class ComputersList extends PFUIComponent {

    private JPanel uiComponent;
    private JPanel computerListPanel;
    private final NodeManagerModel nodeManagerModel;
    private final Set<ExpandableComputerView> viewList;
    private ExpansionListener expansionListener;
    private ComputersTab computersTab;
    private volatile boolean populated;

    // Only access these when synchronized on viewList.
    private final Set<Member> previousMyComputers;
    private final Set<Member> previousFriends;
    private final Set<Member> previousConnectedLans;

    /**
     * Constructor
     * 
     * @param controller
     */
    public ComputersList(Controller controller, ComputersTab computersTab) {
        super(controller);
        this.computersTab = computersTab;
        expansionListener = new MyExpansionListener();
        nodeManagerModel = getUIController().getApplicationModel()
            .getNodeManagerModel();
        viewList = new CopyOnWriteArraySet<ExpandableComputerView>();

        previousConnectedLans = new TreeSet<Member>();
        previousFriends = new TreeSet<Member>();
        previousMyComputers = new TreeSet<Member>();
    }

    /**
     * Get the UI component
     * 
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    /**
     * Build the UI
     */
    private void buildUI() {

        initComponents();

        // Build ui
        FormLayout layout = new FormLayout("pref:grow", "pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(computerListPanel, cc.xy(1, 1));
        uiComponent = builder.getPanel();
    }

    /**
     * Initialize the componets
     */
    private void initComponents() {
        computerListPanel = new JPanel();
        computerListPanel.setLayout(new BoxLayout(computerListPanel,
            BoxLayout.PAGE_AXIS));
        getUIController().getApplicationModel().getNodeManagerModel()
            .addNodeManagerModelListener(new MyNodeManagerModelListener());
        rebuild();
    }

    /**
     * Rebuild the whole list, if there is a significant change. This detects
     * things like Ln nodes becoming friends, etc.
     */
    private void rebuild() {

        // Do nothing until populate command is called.
        if (!populated) {
            return;
        }

        // Create a working copy of the node manager's nodes.
        Set<Member> nodes = new TreeSet<Member>();
        nodes.addAll(nodeManagerModel.getNodes());

        // Split nodes into three groups:
        // 1) My Computers,
        // 2) Friends and
        // 3) Connected LAN
        Set<Member> myComputers = new TreeSet<Member>();
        Set<Member> friends = new TreeSet<Member>();
        Set<Member> connectedLans = new TreeSet<Member>();

        for (Iterator<Member> iterator = nodes.iterator(); iterator.hasNext();)
        {
            Member member = iterator.next();
            if (member.isMyComputer()) {
                myComputers.add(member);
                iterator.remove();
            }
        }

        for (Iterator<Member> iterator = nodes.iterator(); iterator.hasNext();)
        {
            Member member = iterator.next();
            if (member.isOnLAN() && member.isCompletelyConnected()) {
                connectedLans.add(member);
                iterator.remove();
            }
        }

        for (Member member : nodes) {
            if (member.isFriend()) {
                friends.add(member);
            }
        }

        synchronized (viewList) {

            // Are the nodes same as current views?
            boolean different = false;
            if (previousConnectedLans.size() == connectedLans.size()
                && previousFriends.size() == connectedLans.size()
                && previousMyComputers.size() == myComputers.size())
            {
                for (Member member : myComputers) {
                    if (!previousMyComputers.contains(member)) {
                        different = true;
                        break;
                    }
                }
                if (!different) {
                    for (Member member : connectedLans) {
                        if (!previousConnectedLans.contains(member)) {
                            different = true;
                            break;
                        }
                    }
                    if (!different) {
                        for (Member member : friends) {
                            if (!previousFriends.contains(member)) {
                                different = true;
                                break;
                            }
                        }
                    }
                }
            } else {
                different = true;
            }

            if (!different) {
                return;
            }

            // Update for next time.
            previousConnectedLans.clear();
            previousFriends.clear();
            previousMyComputers.clear();
            previousConnectedLans.addAll(connectedLans);
            previousMyComputers.addAll(myComputers);
            previousFriends.addAll(friends);

            // Clear view listeners
            for (ExpandableComputerView view : viewList) {
                view.removeExpansionListener(expansionListener);
                view.removeCoreListeners();
            }
            viewList.clear();
            computerListPanel.removeAll();

            // If there is only one group, do not bother with separators
            boolean multiGroup = (myComputers.isEmpty() ? 0 : 1)
                + (connectedLans.isEmpty() ? 0 : 1)
                + (friends.isEmpty() ? 0 : 1) > 1;

            // First show my computers.
            boolean firstMyComputer = true;
            for (Member node : myComputers) {
                if (firstMyComputer && multiGroup) {
                    firstMyComputer = false;
                    addSeparator(Translation
                        .getTranslation("computer_list.my_computers"));
                }
                addView(node);
            }

            // Then friends.
            boolean firstFriend = true;
            for (Member node : friends) {
                if (firstFriend && multiGroup) {
                    firstFriend = false;
                    addSeparator(Translation
                        .getTranslation("computer_list.friends"));
                }
                addView(node);
            }

            // Then others (connected on LAN).
            boolean firstLan = true;
            for (Member node : connectedLans) {
                if (firstLan && multiGroup) {
                    firstLan = false;
                    addSeparator(Translation
                        .getTranslation("computer_list.lan"));
                }
                addView(node);
            }

            computersTab.updateEmptyLabel();
            getUIComponent().revalidate();
        }
    }

    private void addSeparator(String label) {
        FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
            "pref, 4dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(label, cc.xy(2, 1));
        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        computerListPanel.add(panel);
    }

    private void addView(Member node) {

        ExpandableComputerView view = new ExpandableComputerView(
            getController(), node);
        computerListPanel.add(view.getUIComponent());
        viewList.add(view);
        view.addExpansionListener(expansionListener);
    }

    public boolean isEmpty() {
        return viewList.isEmpty();
    }

    /**
     * Enable the view processing methods so that views get processed. This is
     * done so views do not get added before Synthetica has set all the colors,
     * else views look different before and after.
     */
    public void populate() {
        populated = true;
        rebuild();
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    /**
     * Node Manager Model listener.
     */
    private class MyNodeManagerModelListener implements
        NodeManagerModelListener
    {

        public void changed() {
            rebuild();
        }
    }

    /**
     * Expansion listener to collapse views.
     */
    private class MyExpansionListener implements ExpansionListener {

        public void collapseAllButSource(ExpansionEvent e) {
            synchronized (viewList) {
                for (ExpandableComputerView view : viewList) {
                    if (!view.equals(e.getSource())) {
                        // No source, so collapse.
                        view.collapse();
                    }
                }
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}