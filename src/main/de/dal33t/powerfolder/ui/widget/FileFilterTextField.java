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
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.Translation;

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
public class FileFilterTextField {

    public static final int FILE_NAME_FILTER_MODE = 0;
    public static final int MODIFIER_FILTER_MODE = 1;

    private int columns;
    private JPanel panel;
    private JTextField textField;
    private JButton clearTextJButton;
    private JLabel spacerIcon;
    private JLabel glassIcon;
    private ValueModel externalTextValueModel; // String
    private ValueModel externalModeValueModel; // Integer
    private ValueModel localValueModel;
    private boolean focus;
    private JPopupMenu contextMenu;

    private JRadioButtonMenuItem fileNameRBMI;
    private JRadioButtonMenuItem modifierRBMI;

    /**
     * create a FilterTextField
     *
     * @param columns
     */
    public FileFilterTextField(int columns) {
        this.columns = columns;
        localValueModel = new ValueHolder();
        externalTextValueModel = new ValueHolder();
        externalModeValueModel = new ValueHolder();
        externalModeValueModel.setValue(FILE_NAME_FILTER_MODE);
    }

    /**
     * @return The value model holding the text (excludes hint text value)
     */
    public ValueModel getTextValueModel() {
        return externalTextValueModel;
    }

    /**
     * @return The value model holding the filter mode
     */
    public ValueModel getModeValueModel() {
        return externalModeValueModel;
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
            builder.add(spacerIcon, cc.xy(4, 1));
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
        textField.setColumns(columns);
        textField.setBorder(null);
        // make sure we have room for the button
        // since the button may not be visible we need to force the height
        // else the ui will "jump"
        textField.setPreferredSize(new Dimension(17, 17));
        textField.setToolTipText(Translation.getTranslation("filter_text_field.tip"));
        spacerIcon = SimpleComponentFactory
                .createLabel(Icons.getIconById(Icons.BLANK));
        spacerIcon.setVisible(false);
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
                externalTextValueModel.setValue("");
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
            int mode = (Integer) externalModeValueModel.getValue();
            if (mode == FILE_NAME_FILTER_MODE) {
                textField.setText(Translation.getTranslation(
                    "filter_text_field.menu.file_name.text"));
            } else if (mode == MODIFIER_FILTER_MODE) {
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
        return externalTextValueModel.getValue() != null
                && ((CharSequence) externalTextValueModel.getValue()).length() > 0;
    }

    public JPopupMenu createPopupMenu() {
        if (contextMenu == null) {
            MyActionListener listener = new MyActionListener();
            contextMenu = new JPopupMenu();
            fileNameRBMI = new JRadioButtonMenuItem(Translation.getTranslation(
                    "filter_text_field.menu.file_name.text"));
            fileNameRBMI.setSelected(true);
            fileNameRBMI.addActionListener(listener);
            modifierRBMI = new JRadioButtonMenuItem(Translation.getTranslation(
                    "filter_text_field.menu.modifier.text"));
            modifierRBMI.addActionListener(listener);
            ButtonGroup bg = new ButtonGroup();
            bg.add(fileNameRBMI);
            bg.add(modifierRBMI);
            contextMenu.add(fileNameRBMI);
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
                externalTextValueModel.setValue(localValueModel.getValue());
            }
            // visible if there is text else invisible
            boolean hasExternalText = hasExternalText();
            clearTextJButton.setVisible(hasExternalText);
            spacerIcon.setVisible(!hasExternalText);
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
            if (e.getSource() == fileNameRBMI) {
                externalModeValueModel.setValue(FILE_NAME_FILTER_MODE);
            } else if (e.getSource() == modifierRBMI) {
                externalModeValueModel.setValue(MODIFIER_FILTER_MODE);
            }
            updateForFocus();
        }
    }
}