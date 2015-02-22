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
package de.dal33t.powerfolder.ui.dialog;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.friends.FindComputersDialog;
import de.dal33t.powerfolder.ui.model.NodesSelectTableModel;
import de.dal33t.powerfolder.ui.dialog.BaseDialog;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * Dialog for selecting a number of users.
 */
public class NodesSelectDialog2 extends BaseDialog {

    private NodesSelectTable nodesSelectTable;
    private NodesSelectTableModel nodesSelectTableModel;
    private JCheckBox hideOffline;
    private final Collection<Member> selectedMembers;
    private JButton okButton;

    /**
     * Initialize
     *
     * @param controller
     */
    public NodesSelectDialog2(Controller controller,
                              Collection<Member> selectedMembers) {
        super(Senior.NONE, controller, true);
        this.selectedMembers = selectedMembers;
    }

    public String getTitle() {
        return Translation.get("dialog.node_select.title");
    }

    protected Icon getIcon() {
        return null;
    }

    protected JComponent getContent() {
        // Layout
        FormLayout layout = new FormLayout(
            "pref:grow",
            "pref, 3dlu, pref, 3dlu, pref, 6dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Add components
        builder.addLabel(Translation.get("dialog.node_select.text"),
                cc.xy(1, 1));

        hideOffline = new JCheckBox(Translation
            .get("dialog.node_select.hide_offline.name"));
        builder.add(hideOffline, cc.xy(1, 3));
        hideOffline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doHide();
            }
        });
        nodesSelectTableModel = new NodesSelectTableModel(getController());
        nodesSelectTable = new NodesSelectTable(nodesSelectTableModel);

        nodesSelectTable.registerKeyboardAction(new SelectAllAction(),
		KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK),
		JComponent.WHEN_FOCUSED);

        // Autoselect row if there is only one member.
        if (nodesSelectTableModel.getRowCount() == 1) {
            nodesSelectTable.setRowSelectionInterval(0, 0);
        }
        JScrollPane pane = new JScrollPane(nodesSelectTable);
        pane.setPreferredSize(new Dimension(400, 200));

        builder.add(pane, cc.xy(1, 5));
        return builder.getPanel();
    }

    protected JButton getDefaultButton() {
        return okButton;
    }

    protected Component getButtonBar() {
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateMembers();
                close();
            }
        });

        JButton cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        JButton findFriendsButton = new JButton(Translation
                .get("general.search"));
        findFriendsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FindComputersDialog dialog = new FindComputersDialog(getController());
                dialog.open();
            }
        });
        return ButtonBarFactory.buildCenteredBar(okButton, findFriendsButton,
                cancelButton);
    }

    private void updateMembers() {
        Collection<Member> selections = nodesSelectTable.getSelectedMembers();
        selectedMembers.clear();
        selectedMembers.addAll(selections);
    }

    /**
     * Hide / show offline users.
     */
    private void doHide() {
        nodesSelectTableModel.setHideOffline(hideOffline.isSelected());
    }

    private class SelectAllAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            nodesSelectTable.selectAll();
        }
    }

}