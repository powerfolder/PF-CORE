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
import java.net.InetSocketAddress;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.net.ConnectionListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.net.NetworkUtil;
import de.dal33t.powerfolder.util.ui.UIPanel;

public class ServerSelectorPanel extends PFUIComponent implements UIPanel {

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
        addressField = new JTextField(getServerString());
        addressField.setEditable(false);
        searchButton = new JButtonMini(Icons.getIconById(Icons.EDIT),
            Translation.getTranslation("general.search"));
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ProUtil.openConfigLoaderDialog(getController());
            }
        });
    }

    private String getServerString() {
        String addrStr;
        if (getController().getOSClient().getServer() != null) {
            if (getController().getOSClient().getServer().isMySelf()) {
                addrStr = "myself";
            } else {
                InetSocketAddress addr = getController().getOSClient()
                    .getServer().getReconnectAddress();
                addrStr = addr != null ? NetworkUtil
                    .getHostAddressNoResolve(addr.getAddress()) : "n/a";
                if (addr != null
                    && addr.getPort() != ConnectionListener.DEFAULT_PORT)
                {
                    addrStr += ":" + addr.getPort();
                }
            }
        } else {
            addrStr = "n/a";
        }
        return addrStr;
    }
}
