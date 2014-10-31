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
package de.dal33t.powerfolder.ui.preferences;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.ui.dialog.AddressEditor;
import de.dal33t.powerfolder.ui.dialog.AddressEditor.EditorResult;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.net.AddressRange;

public class LANList extends PFComponent {

    private JPanel panel;
    private JList<String> networklist;
    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;
    private boolean modified;

    public LANList(Controller c) {
        super(c);
        modified = false;
        initComponents();
    }

    private void initComponents() {
        networklist = new JList<>(new DefaultListModel<String>());
        networklist
            .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        addButton = new JButtonMini(Icons.getIconById(Icons.ADD), Translation
            .getTranslation("folder_panel.settings_tab.addbutton.name"));
        // addButton.setText();
        removeButton = new JButtonMini(Icons.getIconById(Icons.DELETE),
            Translation
                .getTranslation("folder_panel.settings_tab.removebutton.name"));
        // removeButton.setText();
        editButton = new JButtonMini(Icons.getIconById(Icons.EDIT), Translation
            .getTranslation("folder_panel.settings_tab.editbutton.name"));
        // editButton.setText();

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AddressEditor editor = new AddressEditor(getController());
                editor.open();
                if (editor.getResult() == EditorResult.OK) {
                    modified = true;
                    ((DefaultListModel<String>) networklist.getModel())
                        .addElement(editor.getAddressRange());
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (networklist.isSelectionEmpty()) {
                    return;
                }
                AddressEditor editor = new AddressEditor(getController(),
                    networklist.getSelectedValue().toString());
                editor.open();
                if (editor.getResult() == EditorResult.OK) {
                    ((DefaultListModel<String>) networklist.getModel()).set(networklist
                        .getSelectedIndex(), editor.getAddressRange());
                    modified = true;
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (String o : networklist.getSelectedValuesList()) {
                    ((DefaultListModel<String>) networklist.getModel())
                        .removeElement(o);
                    modified = true;
                }
            }
        });
    }

    /**
     * @return
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout("pref, pref, pref, pref:grow",
                "40dlu, 3dlu, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(new JScrollPane(networklist), cc.xyw(1, 1, 4));
            builder.add(addButton, cc.xy(1, 3));
            builder.add(editButton, cc.xy(2, 3));
            builder.add(removeButton, cc.xy(3, 3));

            panel = builder.getPanel();
        }
        return panel;
    }

    public boolean save() {
        Object[] ips = ((DefaultListModel<String>) networklist.getModel()).toArray();
        StringBuilder list = new StringBuilder();
        for (Object o : ips) {
            if (list.length() > 0) {
                list.append(", ");
            }
            list.append((String) o);
        }
        ConfigurationEntry.LANLIST.setValue(getController(), list.toString());
        return modified;
    }

    public void load() {
        String lanListValue = ConfigurationEntry.LANLIST
            .getValue(getController());
        if (StringUtils.isBlank(lanListValue)) {
            return;
        }
        String[] lanlist = lanListValue.split(",");
        for (String ip : lanlist) {
            AddressRange ar;
            try {
                ar = AddressRange.parseRange(ip);
            } catch (ParseException e) {
                logWarning("Invalid lanlist entry in configuration file!");
                continue;
            }
            ((DefaultListModel<String>) networklist.getModel()).addElement(ar
                .toString());
        }
    }
}
