/* $Id: SimpleComponentFactory.java,v 1.11 2006/04/14 20:44:53 totmacherr Exp $
 */
package de.dal33t.powerfolder.util.ui;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.*;

import com.jgoodies.binding.adapter.DocumentAdapter;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;
import com.jgoodies.uif.component.UIFLabel;

/**
 * Simple factory for creating PowerFolder ui elements
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
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
     * Creates a textfield upon an value model
     * 
     * @param textModel
     *            the valuemodel that contains the text
     * @param editable
     *            if the field should be editable
     * @return
     */
    public static JTextField createTextField(ValueModel textModel,
        boolean editable)
    {
        if (textModel == null) {
            throw new NullPointerException("textModel is null");
        }
        JTextField field = new JTextField();
        field.setDocument(new DocumentAdapter(textModel));
        field.setEditable(editable);
        return field;
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
        JLabel label = new UIFLabel(text, true);
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
        JLabel label = new UIFLabel(text, true);
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