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
package de.dal33t.powerfolder.ui.widget;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.UIPanel;

import javax.swing.*;
import java.awt.*;

/**
 * A simple panel to display a list of folder.
 * <p>
 * TODO: Add getter for model of selected items
 * <p>
 * TODO: Add sorting
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FolderComboBox implements UIPanel {
    private JPanel panel;
    private JComboBox list;
    private SelectionInList listModel;

    /**
     * Constructs a new folder list panel.
     * 
     * @param aListModel
     *            the model containing the folders to be displayed
     */
    public FolderComboBox(SelectionInList aListModel) {
        Reject.ifNull(aListModel, "Listmodel is nulls");
        listModel = aListModel;
    }

    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            // TODO Correct this: fill:pref:grow
            FormLayout layout = new FormLayout("100dlu", "pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(list, cc.xy(1, 1));
            panel = builder.getPanel();
            panel.setOpaque(false);
        }
        return panel;
    }

    private void initComponents() {
        list = BasicComponentFactory.createComboBox(listModel);
        list.setOpaque(false);
        list.setRenderer(new MyListCellRenderer());
    }

    private static class MyListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList theList,
            Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            Component comp = super.getListCellRendererComponent(theList, value,
                index, isSelected, cellHasFocus);
            if (value instanceof Folder) {
                Folder folder = (Folder) value;
                setIcon(Icons.FOLDER);
                setText(folder.getName());
            }
            return comp;
        }
    }
}
