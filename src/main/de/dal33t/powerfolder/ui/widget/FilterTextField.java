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
 * the "clear" Button is only visible if there is text. There is an internal and
 * an external value model. The internal vm tracks the text field changes. The
 * external one is what the public accessor sees. External value does not expose
 * 'hint' text.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class FilterTextField {
    private int columns;
    private String hint;
    private JPanel panel;
    private JTextField textField;
    private JButton clearTextJButton;
    private JLabel spacerIcon;
    private JLabel glassIcon;
    private ValueModel externalValueModel;
    private ValueModel localValueModel;
    private String tooltip;
    private boolean focus;
    private JPopupMenu contextMenu;

    /**
     * create a FilterTextField
     *
     * @param columns
     */
    public FilterTextField(int columns, String hint, String tooltip) {
        this.columns = columns;
        this.hint = hint;
        this.tooltip = tooltip;
        localValueModel = new ValueHolder();
        externalValueModel = new ValueHolder();
    }

    /**
     * @return The value model holding the text (excludes hint text value)
     */
    public ValueModel getValueModel() {
        return externalValueModel;
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
        if (tooltip != null && tooltip.length() > 0) {
            textField.setToolTipText(tooltip);
        }
        spacerIcon = SimpleComponentFactory.createLabel(Icons
            .getIconById(Icons.BLANK));
        spacerIcon.setVisible(false);
        clearTextJButton = new JButton3Icons(Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_NORMAL), Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_HOVER), Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_CLEAR_BUTTON_PUSH));
        clearTextJButton.setVisible(false);
        clearTextJButton.setToolTipText(Translation
            .getTranslation("filter_text_field.clear.hint"));
        // make sure the background is never drawn
        clearTextJButton.setContentAreaFilled(false);
        clearTextJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textField.setText("");
                externalValueModel.setValue("");
                textField.requestFocus();
            }
        });
        glassIcon = SimpleComponentFactory.createLabel(Icons
            .getIconById(Icons.FILTER_TEXT_FIELD_GLASS));

        localValueModel.addValueChangeListener(new MyPropertyChangeListener());

        MyFocusListener focusListener = new MyFocusListener();
        clearTextJButton.addFocusListener(focusListener);
        textField.addFocusListener(focusListener);
        setHint();
    }

    /**
     * Sets the hint text if there is no external text.
     */
    private void setHint() {
        if (!hasExternalText()) {
            textField.setForeground(Color.lightGray);
            textField.setText(hint);
        }
    }

    /**
     * Clears the hint text if there is no external text.
     */
    private void clearHint() {
        if (!hasExternalText()) {
            textField.setText("");
            textField.setForeground(SystemColor.textText);
        }
    }

    /**
     * Returns true if there is external text.
     *
     * @return
     */
    private boolean hasExternalText() {
        return externalValueModel.getValue() != null
            && ((CharSequence) externalValueModel.getValue()).length() > 0;
    }

    public JPopupMenu createPopupMenu() {
        if (contextMenu == null) {
            contextMenu = new JPopupMenu();
            contextMenu.add(new JMenuItem("todo"));
            // contextMenu.add(addRemoveFriendAction);
            // contextMenu.add(reconnectAction);
        }
        return contextMenu;
    }

    public void requestFocus() {
        textField.requestFocusInWindow();
    }

    /**
     * Listens for changes to the local text value model. if the component has
     * focus, set the external vm.
     */
    private class MyPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (focus) {
                externalValueModel.setValue(localValueModel.getValue());
            }
            // visible if there is text else invisible
            boolean hasExternalText = hasExternalText();
            clearTextJButton.setVisible(hasExternalText);
            spacerIcon.setVisible(!hasExternalText);
        }
    }

    /**
     * Listen for changes to text filed and cancel button. If focus is totally
     * lost, set hint if appropriate.
     */
    private class MyFocusListener extends FocusAdapter {

        public void focusGained(FocusEvent e) {
            doFocusChange();
        }

        public void focusLost(FocusEvent e) {
            doFocusChange();
        }

        private void doFocusChange() {
            focus = clearTextJButton.hasFocus() || textField.hasFocus();
            if (focus) {
                clearHint();
            } else {
                setHint();
            }
        }
    }
}