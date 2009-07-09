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
package de.dal33t.powerfolder.ui.widget;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilter;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.PreferencesEntry;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A text field that has a X button to remove the text, has a ValueModel holding
 * the text data, so listening to text changes should be done on the ValueModel.
 * the "clear" Button is only visible if there is text.
 *
 * There is an internal and an external value model. The internal vm tracks the
 * text field changes. The external one is what the public accessor sees.
 * External value does not expose 'hint' text.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</A>
 */
public class FileFilterTextField extends PFComponent {

    private JPanel panel;
    private JTextField textField;
    private JButton clearTextJButton;
    private JLabel glassIcon;
    private ValueModel externalSearchTextValueModel; // String
    private ValueModel externalSearchModeValueModel; // Integer
    private ValueModel localValueModel;
    private boolean focus;
    private JPopupMenu contextMenu;

    private JRadioButtonMenuItem fileNameDirectoryNameRBMI;
    private JRadioButtonMenuItem fileNameOnlyRBMI;
    private JRadioButtonMenuItem modifierRBMI;

    /**
     * create a FilterTextField
     *
     * @param columns
     */
    public FileFilterTextField(Controller controller) {
        super(controller);
        localValueModel = new ValueHolder();
        externalSearchTextValueModel = new ValueHolder();
        externalSearchModeValueModel = new ValueHolder();
        externalSearchModeValueModel.setValue(PreferencesEntry
                .FILE_SEARCH_MODE.getValueInt(controller));
    }

    /**
     * @return The value model holding the text (excludes hint text value)
     */
    public ValueModel getSearchTextValueModel() {
        return externalSearchTextValueModel;
    }

    /**
     * @return The value model holding the filter mode
     */
    public ValueModel getSearchModeValueModel() {
        return externalSearchModeValueModel;
    }

    public JPanel getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout("pref, 1dlu, pref:grow, 15dlu",
                    "pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(glassIcon, cc.xy(1, 1));
            builder.add(textField, cc.xy(3, 1));
            builder.add(clearTextJButton, cc.xy(4, 1, CellConstraints.RIGHT,
                    CellConstraints.DEFAULT));
            builder.setBorder(new EtchedBorder());
            panel = builder.getPanel();
            panel.setBackground(Color.white);
        }
        return panel;
    }

    private void initComponents() {
        // true = editable
        textField = BasicComponentFactory.createTextField(localValueModel,
                false);
        textField.setColumns(15);
        textField.setBorder(null);
        // make sure we have room for the button
        // since the button may not be visible we need to force the height
        // else the ui will "jump"
        textField.setPreferredSize(new Dimension(17, 17));
        textField.setToolTipText(Translation.getTranslation("filter_text_field.tip"));
        clearTextJButton = new JButton3Icons(
                Icons.getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL),
                Icons.getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER),
                Icons.getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH));
        clearTextJButton.setVisible(false);
        clearTextJButton.setToolTipText(Translation.getTranslation(
                "filter_text_field.clear.hint"));
        // make sure the background is never drawn
        clearTextJButton.setContentAreaFilled(false);
        clearTextJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textField.setText("");
                externalSearchTextValueModel.setValue("");
                textField.requestFocus();
            }
        });
        glassIcon = SimpleComponentFactory.createLabel(Icons.getIconById(
                Icons.FILTER_TEXT_FIELD_GLASS_ARROW));
        glassIcon.setToolTipText(Translation.getTranslation(
                "filter_text_field.glass.hint"));
        glassIcon.addMouseListener(new MyMouseListener());

        localValueModel.addValueChangeListener(new MyPropertyChangeListener());

        MyFocusListener focusListener = new MyFocusListener();
        clearTextJButton.addFocusListener(focusListener);
        textField.addFocusListener(focusListener);
        updateForFocus();
    }

    private void updateForFocus() {
        focus = clearTextJButton.hasFocus() || textField.hasFocus();
        if (!hasExternalText()) {
        if (focus) {
            textField.setText("");
            textField.setForeground(SystemColor.textText);
        } else {
            textField.setForeground(Color.lightGray);
            int mode = (Integer) externalSearchModeValueModel.getValue();
            if (mode == DirectoryFilter.SEARCH_MODE_FILE_NAME_ONLY) {
                textField.setText(Translation.getTranslation(
                        "filter_text_field.menu.file_name_only.text"));
            } else if (mode == DirectoryFilter.SEARCH_MODE_FILE_NAME_DIRECTORY_NAME) {
                textField.setText(Translation.getTranslation(
                    "filter_text_field.menu.file_name_directory_name.text"));
            } else if (mode == DirectoryFilter.SEARCH_MODE_MODIFIER) {
                textField.setText(Translation.getTranslation(
                    "filter_text_field.menu.modifier.text"));
            }
        }
        }
    }

    /**
     * Returns true if there is external text.
     *
     * @return
     */
    private boolean hasExternalText() {
        return externalSearchTextValueModel.getValue() != null
                && ((CharSequence) externalSearchTextValueModel.getValue()).length() > 0;
    }

    public JPopupMenu createPopupMenu() {
        if (contextMenu == null) {
            MyActionListener listener = new MyActionListener();
            contextMenu = new JPopupMenu();

            Integer current = PreferencesEntry.FILE_SEARCH_MODE.getValueInt(getController());
            fileNameDirectoryNameRBMI = new JRadioButtonMenuItem(Translation.getTranslation(
                    "filter_text_field.menu.file_name_directory_name.text"));
            fileNameDirectoryNameRBMI.addActionListener(listener);
            fileNameDirectoryNameRBMI.setSelected(current == DirectoryFilter.SEARCH_MODE_FILE_NAME_DIRECTORY_NAME);
            fileNameOnlyRBMI = new JRadioButtonMenuItem(Translation.getTranslation(
                    "filter_text_field.menu.file_name_only.text"));
            fileNameOnlyRBMI.addActionListener(listener);
            fileNameOnlyRBMI.setSelected(current == DirectoryFilter.SEARCH_MODE_FILE_NAME_ONLY);
            modifierRBMI = new JRadioButtonMenuItem(Translation.getTranslation(
                    "filter_text_field.menu.modifier.text"));
            modifierRBMI.addActionListener(listener);
            modifierRBMI.setSelected(current == DirectoryFilter.SEARCH_MODE_MODIFIER);
            ButtonGroup bg = new ButtonGroup();
            bg.add(fileNameDirectoryNameRBMI);
            bg.add(fileNameOnlyRBMI);
            bg.add(modifierRBMI);
            contextMenu.add(fileNameDirectoryNameRBMI);
            contextMenu.add(fileNameOnlyRBMI);
            contextMenu.add(modifierRBMI);
        }
        return contextMenu;
    }

    /**
     * Listens for changes to the local text value model.
     * if the component has focus, set the external vm.
     */
    private class MyPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (focus) {
                externalSearchTextValueModel.setValue(localValueModel.getValue());
            }
            // visible if there is text else invisible
            boolean hasExternalText = hasExternalText();
            clearTextJButton.setVisible(hasExternalText);
        }
    }

    /**
     * Listen for changes to text filed and cancel button.
     * If focus is totally lost, set hint if appropriate.
     */
    private class MyFocusListener extends FocusAdapter {

        public void focusGained(FocusEvent e) {
            updateForFocus();
        }

        public void focusLost(FocusEvent e) {
            updateForFocus();
        }
    }

    private class MyMouseListener extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            showContextMenu(e);
        }

        private void showContextMenu(MouseEvent e) {
            createPopupMenu().show(e.getComponent(), glassIcon.getX(),
                    glassIcon.getY() + glassIcon.getHeight());
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == fileNameOnlyRBMI) {
                externalSearchModeValueModel.setValue(DirectoryFilter.SEARCH_MODE_FILE_NAME_ONLY);
            } else if (e.getSource() == fileNameDirectoryNameRBMI) {
                externalSearchModeValueModel.setValue(DirectoryFilter.SEARCH_MODE_FILE_NAME_DIRECTORY_NAME);
            } else if (e.getSource() == modifierRBMI) {
                externalSearchModeValueModel.setValue(DirectoryFilter.SEARCH_MODE_MODIFIER);
            }
            PreferencesEntry.FILE_SEARCH_MODE.setValue(getController(),
                    (Integer) externalSearchModeValueModel.getValue());

            updateForFocus();
        }
    }
}