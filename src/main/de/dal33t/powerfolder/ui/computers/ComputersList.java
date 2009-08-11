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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.event.NodeManagerModelEvent;
import de.dal33t.powerfolder.event.NodeManagerModelListener;
import de.dal33t.powerfolder.event.ExpansionListener;
import de.dal33t.powerfolder.event.ExpansionEvent;
import de.dal33t.powerfolder.ui.model.NodeManagerModel;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

public class ComputersList extends PFUIComponent {

    private JPanel uiComponent;
    private JPanel computerListPanel;
    private final NodeManagerModel nodeManagerModel;
    private final Set<ExpandableComputerView> viewList;
    private ExpansionListener expansionListener;
    private ComputersTab computersTab;
    private volatile boolean populated;

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
     * Add a view to the list
     * 
     * @param node
     */
    private void addViewForNode(Member node) {

        // Do nothing until populate command is called.
        if (!populated) {
            return;
        }

        ExpandableComputerView view = new ExpandableComputerView(
            getController(), node);
        synchronized (viewList) {
            computerListPanel.add(view.getUIComponent());
            viewList.add(view);
            view.addExpansionListener(expansionListener);
            computersTab.updateEmptyLabel();
        }
    }

    /**
     * Remove a view from the list.
     * 
     * @param node
     */
    private void removeViewForNode(Member node) {

        // Do nothing until populate command is called.
        if (!populated) {
            return;
        }

        synchronized (viewList) {
            ExpandableComputerView viewToRemove = null;
            outerLoop : for (ExpandableComputerView existingView : viewList) {
                if (existingView.getNode().equals(node)) {
                    viewToRemove = existingView;
                    int count = computerListPanel.getComponentCount();
                    JPanel existingComponent = existingView.getUIComponent();
                    for (int i = 0; i < count; i++) {
                        Component component = computerListPanel.getComponent(i);
                        if (component.equals(existingComponent)) {
                            computerListPanel.remove(component);
                            break outerLoop;
                        }
                    }
                }
            }
            if (viewToRemove != null) {
                viewList.remove(viewToRemove);
                viewToRemove.removeExpansionListener(expansionListener);
                computersTab.updateEmptyLabel();
            }
        }
    }

    /**
     * Rebuild the whole list.
     */
    private void rebuild() {

        // Do nothing until populate command is called.
        if (!populated) {
            return;
        }

        synchronized (viewList) {
            for (ExpandableComputerView view : viewList) {
                view.removeExpansionListener(expansionListener);
                view.removeCoreListeners();
            }
            viewList.clear();
            computerListPanel.removeAll();

            /////////////////////////////
            // Split the members up by //
            // 1) My Computers,        //
            // 2) Friends and          //
            // 3) LAN                  //
            /////////////////////////////

            // Create a working copy of the node manager's nodes.
            Set<Member> nodes = new TreeSet<Member>();
            nodes.addAll(nodeManagerModel.getNodes());

            // First find my computers.
            boolean firstMyComputer = true;
            for (Iterator<Member> iter = nodes.iterator(); iter.hasNext();) {
                Member node = iter.next();
                if (node.isMyComputer()) {
                    if (firstMyComputer) {
                        firstMyComputer = false;
                        addSeparator(Translation.getTranslation("computer_list.my_computers"));
                    }
                    addView(node);
                    iter.remove();
                }
            }

            // Then find friends.
            boolean firstFriend = true;
            for (Iterator<Member> iter = nodes.iterator(); iter.hasNext();) {
                Member node = iter.next();
                if (node.isFriend()) {
                    if (firstFriend) {
                        firstFriend = false;
                        addSeparator(Translation.getTranslation("computer_list.friends"));
                    }
                    addView(node);
                    iter.remove();
                }
            }

            // Then LAN.
            boolean firstOther = true;
            for (Iterator<Member> iter = nodes.iterator(); iter.hasNext();) {
                Member node = iter.next();
                if (node.isOnLAN()) {
                    if (firstOther) {
                        firstOther = false;
                        addSeparator(Translation.getTranslation("computer_list.lan"));
                    }
                    addView(node);
                    iter.remove();
                }
            }

            computersTab.updateEmptyLabel();
        }
    }

    private void addSeparator(String label) {
        FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
            "pref");
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

        public void nodeRemoved(NodeManagerModelEvent e) {
            Member node = e.getNode();
            removeViewForNode(node);
        }

        public void nodeAdded(NodeManagerModelEvent e) {
            Member node = e.getNode();
            addViewForNode(node);
        }

        public void rebuilt(NodeManagerModelEvent e) {
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