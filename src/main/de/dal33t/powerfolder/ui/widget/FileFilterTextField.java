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
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.information.folder.files.DirectoryFilter;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.*;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A text field that has a X button to remove the text, has a ValueModel holding
 * the text data, so listening to text changes should be done on the ValueModel.
 * the "clear" Button is only visible if there is text. There is an internal and
 * an external value model. The internal vm tracks the text field changes. The
 * external one is what the public accessor sees. External value does not expose
 * 'hint' text.
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
    private ButtonGroup buttonGroup;

    private JRadioButtonMenuItem fileNameDirectoryNameRBMI;
    private JRadioButtonMenuItem fileNameOnlyRBMI;
    private JRadioButtonMenuItem modifierRBMI;
    private Map<Member, JRadioButtonMenuItem> computerButtons;
    private MyActionListener popupMenuListener;
    private boolean currentlyMemberMode;
    private boolean previouslyMemberMode;

    /**
     * create a FilterTextField
     *
     * @param controller
     */
    public FileFilterTextField(Controller controller) {
        super(controller);
        localValueModel = new ValueHolder();
        externalSearchTextValueModel = new ValueHolder();
        externalSearchModeValueModel = new ValueHolder();
        externalSearchModeValueModel.setValue(PreferencesEntry.FILE_SEARCH_MODE
            .getValueInt(controller));
        computerButtons = new ConcurrentHashMap<Member, JRadioButtonMenuItem>();
        buttonGroup = new ButtonGroup();
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

    public void reset() {
        textField.setText("");
        externalSearchTextValueModel.setValue("");
    }

    private void initComponents() {
        // true = editable
        textField = BasicComponentFactory.createTextField(localValueModel,
            false);
        textField.setColumns(15);
        textField.setBorder(null);
        // Make sure we have room for the button
        // since the button may not be visible we need to force the height
        // else the ui will "jump"
        textField.setPreferredSize(new Dimension(17, 17));
        textField.setToolTipText(Translation
            .get("filter_text_field.tip"));
        clearTextJButton = new JButton3Icons(Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL), Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER), Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH));
        clearTextJButton.setVisible(false);
        clearTextJButton.setToolTipText(Translation
            .get("filter_text_field.clear.hint"));
        // Make sure the background is never drawn
        clearTextJButton.setContentAreaFilled(false);
        clearTextJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textField.setText("");
                externalSearchTextValueModel.setValue("");
                textField.requestFocus();
            }
        });
        glassIcon = SimpleComponentFactory.createLabel(Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_GLASS_ARROW));
        glassIcon.setToolTipText(Translation
            .get("filter_text_field.glass.hint"));
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
            if (focus && !currentlyMemberMode) {
                textField.setText("");
                textField.setForeground(SystemColor.textText);
            } else {
                textField.setForeground(Color.lightGray);
                int mode = (Integer) externalSearchModeValueModel.getValue();
                if (mode == DirectoryFilter.SEARCH_MODE_FILE_NAME_ONLY) {
                    textField
                        .setText(Translation
                            .get("filter_text_field.menu.file_name_only.text"));
                } else if (mode == DirectoryFilter.SEARCH_MODE_FILE_NAME_DIRECTORY_NAME)
                {
                    textField
                        .setText(Translation
                            .get("filter_text_field.menu.file_name_directory_name.text"));
                } else if (mode == DirectoryFilter.SEARCH_MODE_MODIFIER) {
                    textField
                        .setText(Translation
                            .get("filter_text_field.menu.modifier.text"));
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
            && ((CharSequence) externalSearchTextValueModel.getValue())
                .length() > 0;
    }

    public JPopupMenu createPopupMenu() {
        if (contextMenu == null) {
            popupMenuListener = new MyActionListener();
            contextMenu = new JPopupMenu();

            Integer current = PreferencesEntry.FILE_SEARCH_MODE
                .getValueInt(getController());
            fileNameDirectoryNameRBMI = new JRadioButtonMenuItem(
                Translation
                    .get("filter_text_field.menu.file_name_directory_name.text"));
            fileNameDirectoryNameRBMI.addActionListener(popupMenuListener);
            fileNameDirectoryNameRBMI
                .setSelected(current == DirectoryFilter.SEARCH_MODE_FILE_NAME_DIRECTORY_NAME);
            fileNameOnlyRBMI = new JRadioButtonMenuItem(Translation
                .get("filter_text_field.menu.file_name_only.text"));
            fileNameOnlyRBMI.addActionListener(popupMenuListener);
            fileNameOnlyRBMI
                .setSelected(current == DirectoryFilter.SEARCH_MODE_FILE_NAME_ONLY);
            modifierRBMI = new JRadioButtonMenuItem(Translation
                .get("filter_text_field.menu.modifier.text"));
            modifierRBMI.addActionListener(popupMenuListener);
            modifierRBMI
                .setSelected(current == DirectoryFilter.SEARCH_MODE_MODIFIER);
            buttonGroup.add(fileNameDirectoryNameRBMI);
            buttonGroup.add(fileNameOnlyRBMI);
            buttonGroup.add(modifierRBMI);
            contextMenu.add(fileNameDirectoryNameRBMI);
            contextMenu.add(fileNameOnlyRBMI);
            contextMenu.add(modifierRBMI);
        }
        return contextMenu;
    }

    /**
     * Update changes to the members in the filter list.
     *
     * @param members
     */
    public void setMembers(Collection<Member> members) {
        if (members == null || members.isEmpty()) {
            for (JRadioButtonMenuItem computerButton : computerButtons.values())
            {
                computerButton.removeActionListener(popupMenuListener);
                buttonGroup.remove(computerButton);
                if (computerButton.isSelected()) {
                    fileNameDirectoryNameRBMI.setSelected(true);
                    textField.setText("");
                }
                createPopupMenu().remove(computerButton);
            }
            computerButtons.clear();
        } else {
            // Remove any that are gone.
            for (Map.Entry<Member, JRadioButtonMenuItem> entry : computerButtons
                .entrySet())
            {
                Member member = entry.getKey();
                JRadioButtonMenuItem button = entry.getValue();
                if (!members.contains(member)) {
                    button.removeActionListener(popupMenuListener);
                    buttonGroup.remove(button);
                    createPopupMenu().remove(button);
                    computerButtons.remove(member);
                    if (button.isSelected()) {
                        fileNameDirectoryNameRBMI.setSelected(true);
                        textField.setText("");
                        externalSearchModeValueModel
                            .setValue(DirectoryFilter.SEARCH_MODE_FILE_NAME_ONLY);
                        externalSearchTextValueModel.setValue("");
                        textField.setEnabled(true);
                        updateForFocus();
                    }
                    if (isFine()) {
                        logFine("Removed " + member.getNick());
                    }
                }
            }

            // Add any new ones.
            for (Member member : members) {
                if (!computerButtons.keySet().contains(member)) {
                    JRadioButtonMenuItem button = new JRadioButtonMenuItem(
                        Translation.get(
                            "filter_text_field.menu.computer.text", member
                                .getNick()));
                    computerButtons.put(member, button);
                    buttonGroup.add(button);
                    createPopupMenu().add(button);
                    button.addActionListener(popupMenuListener);
                    if (isFine()) {
                        logFine("Added " + member.getNick());
                    }
                }
            }
        }
    }

    /**
     * Listens for changes to the local text value model. If the component has
     * focus, set the external vm.
     */
    private class MyPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (focus) {
                externalSearchTextValueModel.setValue(localValueModel
                    .getValue());
            }
            // visible if there is text else invisible
            boolean hasExternalText = hasExternalText();
            clearTextJButton
                .setVisible(hasExternalText && !currentlyMemberMode);
        }
    }

    /**
     * Listen for changes to text filed and cancel button. If focus is totally
     * lost, set hint if appropriate.
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
            previouslyMemberMode = currentlyMemberMode;
            if (e.getSource() == fileNameOnlyRBMI) {
                setMode(DirectoryFilter.SEARCH_MODE_FILE_NAME_DIRECTORY_NAME);
            } else if (e.getSource() == fileNameDirectoryNameRBMI) {
                setMode(DirectoryFilter.SEARCH_MODE_FILE_NAME_ONLY);
            } else if (e.getSource() == modifierRBMI) {
                setMode(DirectoryFilter.SEARCH_MODE_MODIFIER);
            } else {
                currentlyMemberMode = true;
                for (Map.Entry<Member, JRadioButtonMenuItem> entry : computerButtons
                    .entrySet())
                {
                    if (entry.getValue() == e.getSource()) {
                        externalSearchModeValueModel
                            .setValue(DirectoryFilter.SEARCH_MODE_COMPUTER);
                        externalSearchTextValueModel.setValue(entry.getKey()
                            .getId());
                        textField.setEnabled(false);
                        textField.setText(Translation.get(
                            "filter_text_field.menu.computer.text", entry
                                .getKey().getNick()));
                    }
                }
            }

            // Don't set preference if one of the member filters is set,
            // because next time might not be a member of the displayed folder.
            if (!currentlyMemberMode) {
                PreferencesEntry.FILE_SEARCH_MODE.setValue(getController(),
                    (Integer) externalSearchModeValueModel.getValue());
            }

            updateForFocus();
        }

        private void setMode(int mode) {
            externalSearchModeValueModel.setValue(mode);
            textField.setEnabled(true);
            currentlyMemberMode = false;
            if (previouslyMemberMode) {
                textField.setText("");
                externalSearchTextValueModel.setValue("");
                textField.setForeground(SystemColor.textText);
                clearTextJButton.setVisible(false);
                textField.transferFocus();
            }
        }
    } // End MyActionListener
}