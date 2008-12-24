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

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.friends.FindComputersDialog;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerSelectorPanel extends PFUIComponent implements UIPanel {

    private JPanel panel;
    private JComboBox selectBox;
    private JButton searchButton;
    private SelectionInList<Member> selectModel;
    private ValueModel serverModel;

    ServerSelectorPanel(Controller controller, ValueModel serverModel) {
        super(controller);
        Reject.ifNull(serverModel, "Server model is null");
        this.serverModel = serverModel;
    }

    public Component getUIComponent() {
        if (panel == null) {
            initComponent();

            FormLayout layout = new FormLayout("0:grow, 3dlu, pref", "pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(selectBox, cc.xy(1, 1));
            builder.add(searchButton, cc.xy(3, 1));

            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponent() {
        selectModel = new SelectionInList<Member>(getUIController()
            .getApplicationModel().getNodeManagerModel().getFriendsListModel(),
            serverModel);
        selectBox = BasicComponentFactory.createComboBox(selectModel);
        selectBox.setRenderer(new MemberListCellRenderer());

        searchButton = new JButton(Translation.getTranslation("general.search"));
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FindComputersDialog dialog = new FindComputersDialog(getController(),
                    true);
                dialog.open();
            }
        });
    }

    private class MemberListCellRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus)
        {

            Member member = (Member) value;
            if (member == null) {
                setText("- n/a -");
                return this;
            }
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

}
