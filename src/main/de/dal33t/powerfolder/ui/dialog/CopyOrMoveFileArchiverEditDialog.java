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
import de.dal33t.powerfolder.disk.CopyOrMoveFileArchiver;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialog for creatigng or editing profile configuration. User can select a
 * default profile and then adjust the configuration.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class CopyOrMoveFileArchiverEditDialog extends BaseDialog {

    private static final int MIN_KEEP_VALUE = 0;
    private static final int MAX_KEEP_VALUE = 999;
    private final CopyOrMoveFileArchiver archiver;
    private JButton okButton;
    private JCheckBox unlimitedCB;
    private JLabel spinnerLabel;
    private SpinnerNumberModel spinnerModel;
    private JSpinner spinner;

    /**
     * Constructor.
     *
     * @param controller
     * @param archiver
     */
    public CopyOrMoveFileArchiverEditDialog(Controller controller,
        CopyOrMoveFileArchiver archiver) {
        super(controller, true);
        this.archiver = archiver;
    }

    /**
     * Gets the title of the dialog.
     *
     * @return
     */
    public String getTitle() {
        return Translation.getTranslation(
                "dialog.copy_or_move_file_archiver_edit.title");
    }

    protected Icon getIcon() {
        return null;
    }

    /**
     * Creates the visual component.
     *
     * @return
     */
    protected JComponent getContent() {
        initComponents();
        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, pref, pref:grow",
            "pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation.getTranslation(
                "dialog.copy_or_move_file_archiver_edit.info"), cc.xyw(1, 1, 4));

        builder.add(unlimitedCB, cc.xyw(3, 3, 2));

        builder.add(spinnerLabel, cc.xy(1, 5));
        builder.add(spinner, cc.xy(3, 5));

        return builder.getPanel();
    }

    /**
     * Initialize the dialog components.
     */
    private void initComponents() {
        int versionsPerFile = archiver.getVersionsPerFile();
        unlimitedCB = new JCheckBox(Translation.getTranslation(
                "dialog.copy_or_move_file_archiver_edit.unlimited"));
        spinnerLabel = new JLabel(Translation.getTranslation(
                "dialog.copy_or_move_file_archiver_edit.versions"));
        if (versionsPerFile < MIN_KEEP_VALUE || versionsPerFile > MAX_KEEP_VALUE)
        {
            spinnerModel = new SpinnerNumberModel(0, MIN_KEEP_VALUE,
                    MAX_KEEP_VALUE, 1);
            unlimitedCB.setSelected(true);
        } else {
            spinnerModel = new SpinnerNumberModel(versionsPerFile, MIN_KEEP_VALUE,
                    MAX_KEEP_VALUE, 1);
        }
        spinner = new JSpinner(spinnerModel);

        unlimitedCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableSpinner();
            }
        });
        enableSpinner();
    }

    private void enableSpinner() {
        spinnerLabel.setEnabled(!unlimitedCB.isSelected());
        spinner.setEnabled(!unlimitedCB.isSelected());
    }

    /**
     * The OK / Cancel buttons.
     *
     * @return
     */
    protected Component getButtonBar() {

        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed();
            }
        });

        JButton cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelPressed();
            }
        });

        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    protected JButton getDefaultButton() {
        return okButton;
    }

    private void okPressed() {
        if (unlimitedCB.isSelected()) {
            archiver.setVersionsPerFile(Integer.MAX_VALUE);
        } else {
            archiver.setVersionsPerFile((Integer) spinnerModel.getNumber());
        }
        close();
    }

    private void cancelPressed() {
        close();
    }

}