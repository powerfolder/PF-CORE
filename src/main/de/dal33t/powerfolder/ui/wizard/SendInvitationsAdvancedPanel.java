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
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.Translation;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.binding.value.ValueModel;

/**
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class SendInvitationsAdvancedPanel extends BaseDialog {

    private JButton okButton;
    private JButton cancelButton;
    private JTextField locationDirectoryField;
    private JButton locationButton;
    private JButton clearButton;
    private final ValueModel locationModel;
    private String location;
    private final String fileName;

    public SendInvitationsAdvancedPanel(Controller controller,
                                        ValueModel locationModel,
                                        String fileName) {
        super(controller, true);
        this.locationModel = locationModel;
        this.fileName = fileName;
        initComponents();
    }

    private void initComponents() {
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        locationDirectoryField = new JTextField();
        locationDirectoryField.setEnabled(false);
        locationButton = new JButtonMini(Icons.getIconById(Icons.DIRECTORY),
                Translation.getTranslation("send_invitations_advanced.location_tip"));
        locationButton.addActionListener(new MyActionListener());
        clearButton = new JButtonMini(Icons.getIconById(Icons.DELETE),
                Translation.getTranslation("send_invitations_advanced.clear_tip"));
        clearButton.addActionListener(new MyActionListener());
        location = (String) locationModel.getValue();
        locationDirectoryField.setText(location);
        updateButtons();
    }

    private void ok() {
        if (location != null) {
            locationModel.setValue(location);
        }
        close();
    }

    private void cancel() {
        close();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton,  cancelButton);
    }

    protected Component getContent() {
        FormLayout layout = new FormLayout("pref, pref:grow",
                "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(new JLabel(Translation.getTranslation(
                "send_invitations_advanced.select_hint", fileName)), cc.xy(1, 1));

        FormLayout layout2 = new FormLayout("107dlu, 3dlu, pref, pref", "pref");
        PanelBuilder builder2 = new PanelBuilder(layout2);
        builder2.add(locationDirectoryField, cc.xy(1, 1));
        builder2.add(locationButton, cc.xy(3, 1));
        builder2.add(clearButton, cc.xy(4, 1));
        JPanel panel2 = builder2.getPanel();
        panel2.setOpaque(false);
        builder.add(panel2, cc.xy(1, 3));

        return builder.getPanel();
    }

    private void updateButtons() {
        clearButton.setEnabled(location != null && location.length() > 0);
    }

    protected Icon getIcon() {
        return Icons.getIconById(Icons.PROJECT_WORK_PICTO);
    }

    public String getTitle() {
        return Translation.getTranslation(
                "wizard.send_invitations_advanced.title");
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == clearButton) {
                location = "";
                locationDirectoryField.setText("");
                updateButtons();
            } else if (e.getSource() == locationButton) {
                String initial = (String) locationModel.getValue();
                String file = DialogFactory.chooseDirectory(getController(),
                    initial);
                location = file;
                locationDirectoryField.setText(file);
                updateButtons();
            }
        }
    }
}