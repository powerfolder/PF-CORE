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
import de.dal33t.powerfolder.ui.model.NodeManagerModel;
import de.dal33t.powerfolder.event.NodeManagerModelListener;
import de.dal33t.powerfolder.event.NodeManagerModelEvent;

import javax.swing.*;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.awt.*;

public class ComputersList extends PFUIComponent {

    private JPanel uiComponent;
    private JPanel computerListPanel;
    private final NodeManagerModel nodeManagerModel;
    private final Set<ExpandableComputerView> viewList;

    /**
     * Constructor
     *
     * @param controller
     */
    public ComputersList(Controller controller) {
        super(controller);
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
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref:grow");
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
        Member[] nodes = getController().getNodeManager().getFriends();
        for (Member node : nodes) {
            addViewForNode(node);
        }
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
        ExpandableComputerView view = new ExpandableComputerView(getController(),
                node);
        synchronized (viewList) {
            computerListPanel.add(view.getUIComponent());
            viewList.add(view);
        }
    }

    /**
     * Remove a view from the list.
     *
     * @param node
     */
    private void removeViewForNode(Member node) {
        synchronized (viewList) {
            ExpandableComputerView viewToRemove = null;
            outerLoop:
            for (ExpandableComputerView existingView : viewList) {
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
            }
        }
    }

    /**
     * Rebuild the whole list.
     */
    private void rebuild() {
        synchronized (viewList) {
            viewList.clear();
            computerListPanel.removeAll();
            for (Member node : nodeManagerModel.getNodes()) {
                ExpandableComputerView view = new ExpandableComputerView(
                        getController(), node);
                computerListPanel.add(view.getUIComponent());
                viewList.add(view);
            }
        }
    }

    //////////////////
    // Inner Classes//
    //////////////////

    /**
     * Node Manager Model listener.
     */
    private class MyNodeManagerModelListener implements
            NodeManagerModelListener {

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
}