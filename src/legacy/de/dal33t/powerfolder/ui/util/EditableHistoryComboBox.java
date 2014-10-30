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
package de.dal33t.powerfolder.ui.util;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.binding.value.ValueModel;

import de.dal33t.powerfolder.util.StringUtils;



/**
 * This item is a editable combo box, which manages a history. It writes to the
 * textmodel on each entered character. Does not buffer until user presses
 * enter. Values are added to the history when selected or entered.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class EditableHistoryComboBox extends JComboBox {

    private static final Logger log = Logger.getLogger(EditableHistoryComboBox.class.getName());
    private static final int DEFAULT_HISTORY_LENGTH = 30;
    private ValueModel textModel;
    private int maxHistoryLength;
    private String boxName;
    private Preferences prefs;
    private String infoText;
    private EditorsDocumentListener editorsDocumentListener;
    private boolean textModelChangedFromInnerBox;
    private boolean selectionChangedFromTextModel;

    // The number of documents events to ignore until write again to text model
    private int nDocumentEventToIgnore;

    /**
     * Builds a textfield, which offers a dropdown box with the last number of
     * entered values. History is not kept persistent
     * 
     * @param textModel
     *            the model the text will be placed in
     * @param maxHistoryLength
     *            the maximum number of history entries
     */
    public EditableHistoryComboBox(ValueModel textModel, int maxHistoryLength) {
        this(textModel, maxHistoryLength, null, null, null);
    }

    /**
     * Builds a textfield, which offers a dropdown box with the last number of
     * entered values. History may kept persistent in preferences in persistent
     * name is given. Optional a info text can be provided, it is shown until
     * the first time the focus is gained
     * 
     * @param textModel
     *            the model the text will be placed in
     * @param maxHistoryLength
     *            the maximum number of history entries
     * @param boxName
     *            the persistent box name for the history
     * @param prefs
     *            the prefs to store the history in
     * @param infoText
     *            the info text displayed until first focus is gained
     */
    public EditableHistoryComboBox(ValueModel textModel, int maxHistoryLength,
        String boxName, Preferences prefs, String infoText)
    {
        super();
        this.textModel = textModel;
        this.maxHistoryLength = maxHistoryLength > 0 ? maxHistoryLength : DEFAULT_HISTORY_LENGTH;
        this.boxName = boxName;
        this.prefs = prefs;
        this.infoText = infoText;
        this.editorsDocumentListener = new EditorsDocumentListener();
        
        this.selectionChangedFromTextModel = false;
        this.textModelChangedFromInnerBox = false;

        // Setup
        super.setEditable(true);

        // Load history from preferences
        loadHistory();

        // Listen to changes from "below"
        textModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (!textModelChangedFromInnerBox) {
                    log.finer("Value changed from below to "
                        + evt.getNewValue());
                    // Only set selected item if textmodel not changed from
                    // ourself
                    selectionChangedFromTextModel = true;
                    setSelectedItem(evt.getNewValue());
                    selectionChangedFromTextModel = false;
                }
            }
        });

        // Item/Selection listener
        addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (selectionChangedFromTextModel) {
                    log.finer("Ingonoring Selection, came from textmodel directly item: "
                            + e.getItem());
                    return;
                }
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    log.finer("Selected item: " + e.getItem());
                    nDocumentEventToIgnore = 2;

                    // update text model value
                    setTextModelValue(getEditor().getItem());

                    // Add item to history if new
                    addToHistory(e.getItem());
                }
            }
        });

        // Behavior on editor chage
        addPropertyChangeListener("editor", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                ComboBoxEditor oldEditor = (ComboBoxEditor) evt.getOldValue();
                ComboBoxEditor newEditor = (ComboBoxEditor) evt.getNewValue();

                // Remove our document listener from the old editor
                if (oldEditor != null
                    && oldEditor.getEditorComponent() instanceof JTextField)
                {
                    JTextField oldEditorsTextField = (JTextField) oldEditor
                        .getEditorComponent();
                    oldEditorsTextField.getDocument().removeDocumentListener(
                        editorsDocumentListener);
                }

                // Add our document listener to the new editors document
                if (newEditor != null
                    && newEditor.getEditorComponent() instanceof JTextField)
                {
                    JTextField newEditorsTextField = (JTextField) newEditor
                        .getEditorComponent();
                    newEditorsTextField.getDocument().addDocumentListener(
                        editorsDocumentListener);
                }

                log.finer("Editor has changed");
            }
        });

        // Register our document listner at current editors documents
        if (getEditor().getEditorComponent() instanceof JTextField) {
            JTextField field = (JTextField) getEditor().getEditorComponent();
            if (!StringUtils.isBlank(infoText)) {
                installInfoText(field, infoText);
            }
            field.getDocument().addDocumentListener(editorsDocumentListener);
        }
    }

    public void updateInfoText() {
        if (getEditor().getEditorComponent() instanceof JTextField) {
            JTextField field = (JTextField) getEditor().getEditorComponent();
            installInfoText(field, infoText);
        }
    }
    
    /**
     * Installs a preview info text on a TextField. The infotext will be shown
     * gray. The info text will be removed when focus is gained
     * 
     * @param field
     *            the field
     * @param aInfoText
     *            the info text to display until
     */
    private void installInfoText(final JTextField field, String aInfoText) {
        final Color originalColor = field.getForeground();
        final String originalText = field.getText();

        // Set info text
        field.setText(aInfoText);
        field.setForeground(Color.GRAY);

        FocusListener focusListener = new FocusListener() {
            public void focusGained(FocusEvent e) {
                // Reset field to origial values
                log.finer("Resetting input field to orignal content");
                field.setText(originalText);
                field.setForeground(originalColor);
                //field.removeFocusListener(this);
            }

            public void focusLost(FocusEvent e) {
                //if no focus and empty text reset the infoText
                if (field.getDocument().getLength() == 0) {
                    updateInfoText();
                }
            }
        };

        // Focuslister to reset field
        field.addFocusListener(focusListener);
    }

    /**
     * Overridden and disabled
     * 
     * @see javax.swing.JComboBox#setEditable(boolean)
     */
    public void setEditable(boolean editable) {
        throw new IllegalStateException(
            "Unable to change editable state of this component");
    }

    /**
     * Sets the value from text model, while telling that the change is comming
     * from inside this instance
     * ignores it if it equals the infoText
     * @param value
     *            the new value
     */
    private void setTextModelValue(Object value) {
        if (value instanceof String) {
            String strValue = (String)value;
            if (strValue.equals(infoText)) {
                return;
            }
        }
        textModelChangedFromInnerBox = true;
        textModel.setValue(value);
        textModelChangedFromInnerBox = false;
    }

    /**
     * Adds an item to the history list. Removes old items if nesseary. Item
     * won't be added if already in list. String items will be stored in
     * preferences is box is persistent
     * 
     * @param item
     */
    private void addToHistory(Object item) {
        addToHistory0(item);

        // History only available for string items
        if (prefs != null && item instanceof String) {
            int prefIndex = prefs.getInt(boxName + ".startindex", 0);
            prefIndex++;
            if (prefIndex >= maxHistoryLength) {
                prefIndex = 0;
            }

            prefs.put(boxName + "." + prefIndex, (String) item);
            prefs.putInt(boxName + ".startindex", prefIndex);
        }
    }

    /**
     * Adds an item to the history list. Removes old items if nesseary. Item
     * won't be added if already in list (FIXME).
     * 
     * @param item
     */
    private void addToHistory0(Object item) {
        if (item == null) {
            return;
        }

        // Not add empty string to history
        if (item instanceof String && StringUtils.isBlank((String) item)) {
            return;
        }

        // Not addd item if in list
        int itemCount = getItemCount();
        for (int i = 0; i < itemCount; i++) {
            if (item.equals(getItemAt(i))) {
                return;
            }
        }

        log.finer("Adding to history: " + item);

        // Add Item at top position
        insertItemAt(item, 0);

        //Remove old items
        if (itemCount >= maxHistoryLength) {
            // Remove last element
            removeItemAt(itemCount);
        }
    }

    /**
     * Loads the history from the preferences if available
     */
    private void loadHistory() {
        if (prefs == null) {
            return;
        }
        int prefIndex = prefs.getInt(boxName + ".startindex", 0);
        prefIndex++;
        if (prefIndex >= maxHistoryLength) {
            prefIndex = 0;
        }

        for (int i = 0; i < maxHistoryLength; i++) {
            String item = prefs.get(boxName + "." + prefIndex, null);
            if (item != null) {
                addToHistory0(item);
            }
            prefIndex++;
            if (prefIndex >= maxHistoryLength) {
                prefIndex = 0;
            }
        }
    }

    /**
     * Document listener on the editors document of the JComboBox. Basically
     * acts on document changes and gives it to the text model
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.6 $
     */
    private class EditorsDocumentListener implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            if (nDocumentEventToIgnore <= 0) {
                //                LOG
                //                    .warn("changedUpdate " + e.getOffset() + "/"
                //                        + e.getLength());
                setTextModelValue(getEditor().getItem());
            } else {
                nDocumentEventToIgnore--;
            }
        }

        public void insertUpdate(DocumentEvent e) {
            if (nDocumentEventToIgnore <= 0) {
                //LOG.warn("insertUpdate " + e.getOffset() + "/" +
                // e.getLength());
                setTextModelValue(getEditor().getItem());
            } else {
                nDocumentEventToIgnore--;
            }
        }

        public void removeUpdate(DocumentEvent e) {
            if (nDocumentEventToIgnore <= 0) {
                //LOG.warn("removeUpdate " + e.getOffset() + "/" +
                // e.getLength());
                setTextModelValue(getEditor().getItem());
            } else {
                nDocumentEventToIgnore--;
            }
        }
    }
}