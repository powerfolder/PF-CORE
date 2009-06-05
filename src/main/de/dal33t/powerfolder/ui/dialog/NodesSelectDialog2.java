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
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.friends.FindComputersDialog;
import de.dal33t.powerfolder.ui.model.NodesSelectTableModel;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Dialog for selecting a number of users.
 */
public class NodesSelectDialog2 extends PFUIComponent {

    private JDialog uiComponent;
    private NodesSelectTable nodesSelectTable;
    private NodesSelectTableModel nodesSelectTableModel;
    private JCheckBox hideOffline;
    private final Dialog owner;
    private final Collection<Member> selectedMembers;

    /**
     * Initialize
     *
     * @param controller
     */
    public NodesSelectDialog2(Controller controller, Dialog owner,
                              Collection<Member> selectedMembers) {
        super(controller);
        this.owner = owner;
        this.selectedMembers = selectedMembers;
    }

    /**
     * Initalizes / builds all ui elements
     */
    public void initComponents() {

        // General dialog initalization
        uiComponent = new JDialog(owner, Translation.getTranslation(
                "dialog.node_select.title"), true);

        uiComponent.setResizable(false);
        uiComponent.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JButton okButton = new JButton(Translation.getTranslation("general.ok"));
        okButton.setMnemonic(Translation.getTranslation("general.ok.key")
            .charAt(0));
        JButton cancelButton = new JButton(Translation
            .getTranslation("general.cancel"));
        cancelButton.setMnemonic(Translation.getTranslation(
            "general.cancel.key").charAt(0));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        JButton findFriendsButton = new JButton(Translation
            .getTranslation("general.search"));
        findFriendsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FindComputersDialog dialog = new FindComputersDialog(getController(),
                    true);
                dialog.open();
            }
        });
        JComponent buttonBar = ButtonBarFactory.buildCenteredBar(okButton,
            findFriendsButton, cancelButton);

        // OK is the default
        uiComponent.getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(new MyOkListener());

        // Layout
        FormLayout layout = new FormLayout(
            "pref:grow",
            "pref, 3dlu, pref, 3dlu, pref, 6dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(Borders.createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
        CellConstraints cc = new CellConstraints();

        // Add components
        builder.addLabel(Translation.getTranslation("dialog.node_select.text"),
                cc.xy(1, 1));

        hideOffline = new JCheckBox(Translation
            .getTranslation("dialog.node_select.hide_offline.name"));
        builder.add(hideOffline, cc.xy(1, 3));
        hideOffline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doHide();
            }
        });
        nodesSelectTableModel = new NodesSelectTableModel(getController());
        nodesSelectTable = new NodesSelectTable(nodesSelectTableModel);

        // Autoselect row if there is only one member.
        if (nodesSelectTableModel.getRowCount() == 1) {
            nodesSelectTable.setRowSelectionInterval(0, 0);
        }
        JScrollPane pane = new JScrollPane(nodesSelectTable);
        pane.setPreferredSize(new Dimension(400, 200));

        builder.add(pane, cc.xy(1, 5));

        builder.add(buttonBar, cc.xy(1, 7));

        uiComponent.getContentPane().add(builder.getPanel());
        uiComponent.pack();

        // Orientation
        Component parent = uiComponent.getOwner();
        if (parent != null) {
            int x = parent.getX()
                + (parent.getWidth() - uiComponent.getWidth()) / 2;
            int y = parent.getY()
                + (parent.getHeight() - uiComponent.getHeight()) / 2;
            uiComponent.setLocation(x, y);
        }
    }

    /**
     * Hide / show offline users.
     */
    private void doHide() {
        nodesSelectTableModel.setHideOffline(hideOffline.isSelected());
    }

    /**
     * Returns the ui component (dialog)
     *
     * @return
     */
    private JDialog getUIComponent() {
        if (uiComponent == null) {
            initComponents();
        }
        return uiComponent;
    }

    /**
     * Opens the dialog
     *
     * @return not used
     */
    public boolean open() {
        logWarning("Opening download dialog");
        getUIComponent().setVisible(true);
        return true;
    }

    /**
     * Closes the dialog
     */
    public void close() {
        if (uiComponent != null) {
            uiComponent.dispose();
        }
    }

    /**
     * Confirmation button action.
     */
    private class MyOkListener implements ActionListener {

        /**
         * Set the value model and user collection in the underlying wizard.
         *
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            Collection<Member> selections = nodesSelectTable.getSelectedMembers();
            selectedMembers.clear();
            selectedMembers.addAll(selections);
            close();
        }
    }
}