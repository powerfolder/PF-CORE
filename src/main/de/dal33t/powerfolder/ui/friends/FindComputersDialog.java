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
package de.dal33t.powerfolder.ui.friends;

import com.jgoodies.forms.factories.ButtonBarFactory;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.dialog.BaseDialog;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FindComputersDialog extends BaseDialog {

    private FindComputersPanel panel;
    private JButton addToFriendsButton;
    private JButton closeButton;
    private JButton connectButton;

    public FindComputersDialog(Controller controller) {
        super(Senior.NONE, controller, true, true);
        initComponents();
    }

    private void initComponents() {
        panel = new FindComputersPanel(getController());
        addToFriendsButton = createAddToFriendsButton();
        addToFriendsButton.setIcon(null);
        closeButton = createCloseButton();
        connectButton = createConnectButton();
        connectButton.setIcon(null);
    }

    private JButton createAddToFriendsButton() {
        return new JButton(panel.getAddFriendAction());
    }

    private JButton createConnectButton() {
        return new JButton(panel.getConnectAction());
    }

    public String getTitle() {
        return Translation.getTranslation("exp.find_computers_dialog.title");
    }

    protected Icon getIcon() {
        return null;
    }

    protected JComponent getContent() {
        return panel.getUIComponent();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(connectButton,
            addToFriendsButton, closeButton);
    }

    @Override
    protected JButton getDefaultButton() {
        return addToFriendsButton;
    }

    @Override
    public void close() {
        panel.cancelSearch();
        super.close();
    }

    /**
     * Creates the okay button for the whole pref dialog
     */
    private JButton createCloseButton() {
        return createCloseButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }
}
