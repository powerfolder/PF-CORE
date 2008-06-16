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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * A text field that has a X button to remove the text, has a ValueModel holding
 * the text data, so listening to text changes should be done on the ValueModel.
 * the "clear" Button is only visible if there is text.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class FilterTextField {
    private int columns;
    private JPanel panel;
    private JTextField jTextField;
    private JButton cancelTextJButton;
    private ValueModel valueModel;

    /** create a FilterTextField */
    public FilterTextField(int columns) {
        this.columns = columns;
        valueModel = new ValueHolder();
    }

    /** The value model holding the text */
    public ValueModel getValueModel() {
        return valueModel;
    }

    public JPanel getUIComponent() {
        if (panel == null) {
            initComponents();
            FormLayout layout = new FormLayout("pref:grow, pref, 1dlu", "pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            builder.add(jTextField, cc.xy(1, 1));
            builder.add(cancelTextJButton, cc.xy(2, 1));
            builder.setBorder(new EtchedBorder());
            panel = builder.getPanel();
            panel.setBackground(Color.WHITE);
        }
        return panel;
    }

    private void initComponents() {
        // true = editable
        jTextField = BasicComponentFactory.createTextField(valueModel, false);
        jTextField.setColumns(columns);
        jTextField.setBorder(null);
        // make sure we have room for the button
        // since the button may not be visible we need to force the height
        // else the ui will "jump"
        jTextField.setPreferredSize(new Dimension(17, 17));
        valueModel.addValueChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                String text = (String) valueModel.getValue();
                // visible if there is text else invisible
                boolean visible = text.length() > 0;
                cancelTextJButton.setVisible(visible);
                if (!visible) {// textfiled should get focus if button is
                    // hidden
                    jTextField.requestFocus();
                }
            }

        });
        cancelTextJButton = new JButton3Icons(
            Icons.FILTER_TEXTFIELD_CLEARBUTTON_NORMAL,
            Icons.FILTER_TEXTFIELD_CLEARBUTTON_HOVER,
            Icons.FILTER_TEXTFIELD_CLEARBUTTON_PUSH);
        cancelTextJButton.setVisible(false);
        //make sure the background is never drawn
        cancelTextJButton.setContentAreaFilled(false);
        cancelTextJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jTextField.setText("");
            }
        });
        
        
    }
}