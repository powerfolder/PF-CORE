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
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.Translation;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

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
    private final ValueModel locationValueModel;
    private final ValueModel permissionsValueModel;
    private String location;
    private final String fileName;
    private JComboBox permissionsCombo;

    public SendInvitationsAdvancedPanel(Controller controller,
                                        ValueModel locationValueModel,
                                        ValueModel permissionsValueModel,
                                        String fileName) {
        super(controller, true);
        this.locationValueModel = locationValueModel;
        this.permissionsValueModel = permissionsValueModel;
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
        location = (String) locationValueModel.getValue();
        locationDirectoryField.setText(location);
        DefaultComboBoxModel permissionsModel = new DefaultComboBoxModel(
                new String[]{
                        Invitation.getNameForPermission(Invitation.READ_WRITE_PERMISSION),
                        Invitation.getNameForPermission(Invitation.READ_PERMISSION),
                        Invitation.getNameForPermission(Invitation.ADMIN_PERMISSION)});
        permissionsCombo = new JComboBox(permissionsModel);
        permissionsCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                // Slight generalization here: Assume that Invite.permissions
                // are sequential from zero.
                permissionsValueModel.setValue(permissionsCombo.getSelectedIndex());
            }
        });
        permissionsCombo.setSelectedIndex((Integer) permissionsValueModel.getValue());
        updateButtons();
    }

    private void ok() {
        if (location != null) {
            locationValueModel.setValue(location);
        }
        close();
    }

    private void cancel() {
        close();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    protected Component getContent() {
        FormLayout layout = new FormLayout("right:pref, 3dlu, pref, pref:grow",
                "pref, 3dlu, pref, 3dlu, pref, 6dlu, pref, 3dlu, pref");
             //  file sep    file name   file dir    perm sep    combo
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // File separator
        builder.addSeparator(Translation.getTranslation(
                "send_invitations_advanced.file_label"), cc.xyw(1, 1, 3));

        // File name
        builder.add(new JLabel(Translation.getTranslation(
                "send_invitations_advanced.filename")), cc.xy(1, 3));
        builder.add(new JLabel(fileName), cc.xy(3, 3));

        // File dir
        builder.add(new JLabel(Translation.getTranslation(
                "send_invitations_advanced.file_hint")), cc.xy(1, 5));
        FormLayout layout2 = new FormLayout("107dlu, 3dlu, pref, pref", "pref");
        PanelBuilder builder2 = new PanelBuilder(layout2);
        builder2.add(locationDirectoryField, cc.xy(1, 1));
        builder2.add(locationButton, cc.xy(3, 1));
        builder2.add(clearButton, cc.xy(4, 1));
        JPanel panel2 = builder2.getPanel();
        panel2.setOpaque(false);
        builder.add(panel2, cc.xy(3, 5));

        // Permission separator
        builder.addSeparator(Translation.getTranslation(
                "send_invitations_advanced.permissions_label"), cc.xyw(1, 7, 3));

        builder.add(new JLabel(Translation.getTranslation(
                "send_invitations_advanced.permissions_hint")), cc.xy(1, 9));
        FormLayout layout3 = new FormLayout("pref, pref:grow", "pref");
        PanelBuilder builder3 = new PanelBuilder(layout3);
        builder3.add(permissionsCombo, cc.xy(1, 1));
        JPanel panel3 = builder3.getPanel();
        panel3.setOpaque(false);
        builder.add(panel3, cc.xy(3, 9));


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
                String initial = (String) locationValueModel.getValue();
                String file = DialogFactory.chooseDirectory(getController(),
                        initial);
                location = file;
                locationDirectoryField.setText(file);
                updateButtons();
            }
        }
    }
}