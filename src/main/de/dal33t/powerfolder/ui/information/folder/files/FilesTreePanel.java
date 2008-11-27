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
* $Id: FilesTreePanel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;

public class FilesTreePanel extends PFUIComponent implements DirectoryFilterListener {

    private JPanel uiComponent;
    private DirectoryTreeModel directoryTreeModel;
    private DirectoryFilter directoryFilter;

    public FilesTreePanel(Controller controller, DirectoryFilter directoryFilter) {
        super(controller);
        directoryTreeModel = new DirectoryTreeModel(controller);
        this.directoryFilter = directoryFilter;
        directoryFilter.addListener(this);
    }

    /**
     * Gets the ui component
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Builds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("fill:30:grow",
                "fill:pref:grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        JTree tree = new JTree(directoryTreeModel);
        tree.setCellRenderer(new MyTreeCellRenderer());
        JScrollPane scrollPane = new JScrollPane(tree);
        // Whitestrip
        UIUtil.removeBorder(scrollPane);
        UIUtil.setZeroHeight(scrollPane);
        builder.add(scrollPane, cc.xy(1, 1));


        uiComponent = builder.getPanel();
        uiComponent.setBorder(BorderFactory.createEtchedBorder());
    }

    public void adviseOfChange() {
        directoryTreeModel.setTree(directoryFilter.getModel());
    }

    private class MyTreeCellRenderer extends DefaultTreeCellRenderer {
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf, int row,
                                                      boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded,
                    leaf, row, hasFocus);
            Object userObject = UIUtil.getUserObject(value);
            if (userObject instanceof DirectoryTreeNodeUserObject) {
                DirectoryTreeNodeUserObject dtnuo =
                        (DirectoryTreeNodeUserObject) userObject;
                setText(dtnuo.getDisplayName());
                setIcon(Icons.FOLDER);
            }
            return this;
        }
    }
}
