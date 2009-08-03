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
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.disk.FileVersionInfo;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

/**
 * Dialog for restoring a selected file archive.
 */
public class RestoreArchiveDialog extends BaseDialog {

    private JPanel uiComponent;

    private FileVersionInfo fileVersionInfo;
    private JRadioButton restoreRB;
    private JRadioButton saveRB;
    private JLabel fileLocationLabel;
    private JTextField fileLocationField;
    private JButtonMini fileLocationButton;

    /**
     * Constructor
     *
     * @param controller
     * @param fileVersionInfo
     *                 the info of the file version to restore
     */
    public RestoreArchiveDialog(Controller controller,
                                FileVersionInfo fileVersionInfo) {
        super(controller, true);
        this.fileVersionInfo = fileVersionInfo;
    }

    protected Component getContent() {
        if (uiComponent == null) {

            restoreRB = new JRadioButton(Translation.getTranslation(
                    "dialog.restore_archive.restore"));
            saveRB = new JRadioButton(Translation.getTranslation(
                    "dialog.restore_archive.save"));
            ButtonGroup bg = new ButtonGroup();
            bg.add(restoreRB);
            bg.add(saveRB);

            // Layout
            FormLayout layout = new FormLayout(
                "pref, 3dlu, 122dlu, 3dlu, 15dlu, pref:grow",
                "pref, 3dlu, pref, 3dlu, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            CellConstraints cc = new CellConstraints();

            fileLocationLabel = new JLabel(Translation.getTranslation(
                    "dialog.restore_archive.file_location"));
            fileLocationField = new JTextField();
            fileLocationField.setEnabled(false);
            fileLocationButton = new JButtonMini(Icons.getIconById(Icons.DIRECTORY),
                    Translation.getTranslation("dialog.restore_archive.file_location.tip"));

            // Add components
            builder.add(restoreRB, cc.xyw(1, 1, 6));
            builder.add(saveRB, cc.xyw(1, 3, 6));
            builder.add(fileLocationLabel, cc.xy(1, 5));
            builder.add(fileLocationField, cc.xy(3, 5));
            builder.add(fileLocationButton, cc.xy(5, 5));

            restoreRB.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    enableFileLocationComponents();
                }
            });
            saveRB.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    enableFileLocationComponents();
                }
            });

            restoreRB.setSelected(true);
            enableFileLocationComponents();
            uiComponent = builder.getPanel();
        }
        return uiComponent;
    }

    protected Icon getIcon() {
        return null;
    }

    public String getTitle() {
        return Translation.getTranslation("dialog.restore_archive.title");
    }

    protected Component getButtonBar() {
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

        okButton.addActionListener(new MyOkListener());

        return ButtonBarFactory.buildCenteredBar(okButton,
            cancelButton);
    }

    private void enableFileLocationComponents() {
        boolean enabled = saveRB.isSelected();
        fileLocationLabel.setEnabled(enabled);
        fileLocationButton.setEnabled(enabled);
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
            close();
        }
    }
}