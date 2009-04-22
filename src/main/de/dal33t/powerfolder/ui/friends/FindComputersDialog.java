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
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FindComputersDialog extends BaseDialog {

    private FindComputersPanel panel;
    private JButton addToFriendsButton;
    private JButton closeButton;

    public FindComputersDialog(Controller controller, boolean modal) {
        super(controller, modal);
        initComponents();
    }

    private void initComponents() {
        panel = new FindComputersPanel(getController());
        addToFriendsButton = createAddToFriendsButton();
        closeButton = createCloseButton();
    }

    private JButton createAddToFriendsButton() {        
        return new JButton(panel.getAddFriendAction());
    }

    public String getTitle() {
        return Translation.getTranslation("find_computers_dialog.title");
    }

    protected Icon getIcon() {
        return Icons.getIconById(Icons.FRIENDS);
    }

    protected Component getContent() {
        return panel.getUIComponent();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(addToFriendsButton, closeButton);
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
