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
package de.dal33t.powerfolder.ui.folder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ui.widget.FilterTextField;
import de.dal33t.powerfolder.util.Translation;

/**
 * Holds a box with for entering keywords and 3 checkboxes (showNormal,
 * showDeleted and shoeExpexted). The checkboxes are optional displayed. The
 * checkboxes labels also hold a count of the number of files that matches the
 * criteria in the FileFilterModel.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class FileFilterPanel {

    private JPanel panel;
    private FileFilterModel fileFilterModel;
    private JComboBox filterSelectionComboBox;

    /**
     * @param showCheckBoxes
     *            set to false to hide them
     */
    public FileFilterPanel(FileFilterModel fileFilterModel) {
        this.fileFilterModel = fileFilterModel;
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
        }
        return panel;
    }

    public void reset() {
        fileFilterModel.reset();
    }

    /**
     * Initalize all nessesary components
     */
    private void initComponents() {
        FilterTextField filterTextField = new FilterTextField(12, Translation
                .getTranslation("file_filter_panel.filter_by_filename.hint"),
        Translation
                .getTranslation("file_filter_panel.filter_by_filename.tooltip"));

        fileFilterModel.setSearchField(filterTextField.getValueModel());

        filterSelectionComboBox = new JComboBox();
        filterSelectionComboBox.setToolTipText(Translation
                .getTranslation("file_filter_panel.combo.tooltip"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("file_filter_panel.local_and_incoming"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("file_filter_panel.local_files_only"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("file_filter_panel.incoming_files_only"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("file_filter_panel.new_files_only"));
        filterSelectionComboBox.addItem(Translation
            .getTranslation("file_filter_panel.deleted_and_previous_files"));
        filterSelectionComboBox.addActionListener(new MyActionListener());

        FormLayout layout = new FormLayout("pref, fill:pref:grow, 105dlu",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(filterSelectionComboBox, cc.xy(1, 1));
        builder.add(filterTextField.getUIComponent(), cc.xy(3, 1));
        panel = builder.getPanel();
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(filterSelectionComboBox)) {
                fileFilterModel.setMode(filterSelectionComboBox
                    .getSelectedIndex());
                fileFilterModel.scheduleFiltering();
            }
        }
    }
}
