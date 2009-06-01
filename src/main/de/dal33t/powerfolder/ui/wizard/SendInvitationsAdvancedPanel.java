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
package de.dal33t.powerfolder.ui.wizard;

import javax.swing.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.Translation;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class SendInvitationsAdvancedPanel extends BaseDialog {

    private JButton closeButton;

    public SendInvitationsAdvancedPanel(Controller controller) {
        super(controller, true);
        initComponents();
    }

    private void initComponents() {
        closeButton = createCloseButton();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(closeButton);
    }

    protected Component getContent() {
        FormLayout layout = new FormLayout("pref, pref:grow", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(new JLabel("test"), cc.xy(1, 1));
        return builder.getPanel();
    }

    protected Icon getIcon() {
        return Icons.getIconById(Icons.PROJECT_WORK_PICTO);
    }

    public String getTitle() {
        return Translation.getTranslation(
                "wizard.send_invitations_advanced.title");
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