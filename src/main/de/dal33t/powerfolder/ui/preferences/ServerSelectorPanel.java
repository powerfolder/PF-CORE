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
 * $Id: PreferenceTab.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.ui.preferences;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.dialog.ConfigurationLoaderDialog;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Translation;

public class ServerSelectorPanel extends PFUIComponent {

    private JPanel panel;
    private JTextField addressField;
    private JButton searchButton;

    ServerSelectorPanel(Controller controller) {
        super(controller);
    }

    public Component getUIComponent() {
        if (panel == null) {
            initComponent();

            FormLayout layout = new FormLayout("0:grow, 3dlu, pref", "pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(addressField, cc.xy(1, 1));
            builder.add(searchButton, cc.xy(3, 1));

            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponent() {
        addressField = new JTextField(getController().getOSClient()
            .getServerString());
        addressField.setEditable(false);
        addressField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new ConfigurationLoaderDialog(getController()).openAndWait();
            }
        });
        searchButton = new JButtonMini(Icons.getIconById(Icons.EDIT),
            Translation.getTranslation("general.search"));
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new ConfigurationLoaderDialog(getController()).openAndWait();
            }
        });
        searchButton.setEnabled(true);
    }
}
