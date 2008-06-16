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
package de.dal33t.powerfolder.util.ui;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;

import de.dal33t.powerfolder.ui.widget.AntialiasedLabel;

/**
 * Simple factory for creating PowerFolder ui elements
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public class SimpleComponentFactory {
    public static final int BIG_FONT_SIZE = 15;
    public static final int BIGGER_FONT_SIZE = 20;

    /**
     * No instance allowed
     */
    private SimpleComponentFactory() {
        // Private constructor
    }

    /**
     * Creates a textfield
     * 
     * @param editable
     *            if the field should be editable
     * @return
     */
    public static JTextField createTextField(boolean editable) {
        JTextField field = new JTextField();
        field.setEditable(editable);
        return field;
    }
    
    /**
     * @return a new password field
     */
    public static JPasswordField createPasswordField() {
        return new JPasswordField();
    }

    /**
     * Creates a simple label
     * 
     * @return
     */
    public static JLabel createLabel() {
        return new JLabel();
    }

    /**
     * Creates a simple label with text
     * 
     * @param text
     *            the text of the label
     * @return
     */
    public static JLabel createLabel(String text) {
        return new JLabel(text);
    }

    /**
     * Creates a label which has big font than normal. Has smoothed font
     * 
     * @param text
     * @return
     */
    public static JLabel createBigTextLabel(String text) {
        AntialiasedLabel label = new AntialiasedLabel(text);
        setFontSize(label, BIG_FONT_SIZE);
        return label;
    }

    /**
     * Creates a label which has bigger font than normal. Has smoothed font
     * 
     * @param text
     * @return
     */
    public static JLabel createBiggerTextLabel(String text) {
        AntialiasedLabel label = new AntialiasedLabel(text);
        setFontSize(label, BIGGER_FONT_SIZE);
        return label;
    }

    /**
     * Sets the fontsize on of label
     * 
     * @param label
     * @param fontSize
     */
    public static void setFontSize(JLabel label, int fontSize) {
        Font font = new Font(label.getFont().getFontName(), 0, fontSize);
        label.setFont(font);
    }

    /**
     * Creates a checkbox
     * 
     * @return the fresh intalized checkbox
     */
    public static JCheckBox createCheckBox() {
        return new JCheckBox();
    }

    /**
     * Creates a checkbox with a title
     * 
     * @param title
     *            the title of the checkbox
     * @return the fresh intalized checkbox
     */
    public static JCheckBox createCheckBox(String title) {
        return new JCheckBox(title);
    }

    /**
     * Creates a popup menu
     * 
     * @return
     */
    public static JPopupMenu createPopupMenu() {
        JPopupMenu popupmenu = new JPopupMenu();
        popupmenu.putClientProperty(Options.HEADER_STYLE_KEY, HeaderStyle.BOTH);
        return popupmenu;
    }

    /**
     * Creates a new toolbar
     * 
     * @return
     */
    public static JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();
        return toolbar;
    }

    /**
     * Creates a empty combobox. set the model to the selected value. Does not
     * acts on model changes
     * 
     * @param model
     *            the model
     * @return
     */
    public static JComboBox createComboBox(final ValueModel model) {
        if (model == null) {
            throw new NullPointerException("Model is null");
        }
        JComboBox box = new JComboBox();

        // Behavior
        box.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    model.setValue(e.getItem());
                }

            }
        });

        return box;
    }
}