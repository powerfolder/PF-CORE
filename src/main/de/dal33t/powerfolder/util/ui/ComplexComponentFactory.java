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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Translation;

/**
 * Factory for several complexer fields.
 * <p>
 * TODO CLEANUP THIS MESS
 * 
 * @see de.dal33t.powerfolder.util.ui.SimpleComponentFactory
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.19 $
 */
public class ComplexComponentFactory {

    private ComplexComponentFactory() {
        // No instance allowed
    }

    /**
     * Creates a file selection field. A browse button is attached at the right
     * side
     * 
     * @param title
     *            the title of the filechoose if pressed the browse button
     * @param fileSelectionModel
     *            the file base value model, will get/write base as String
     * @param fileSelectionMode
     *            the selection mode of the filechooser
     * @param fileFilter
     *            the filefilter used for the filechooser. may be null will
     *            ignore it then
     * @param preEventListener
     *            an optional additional listern for the browse button
     * @param open
     *            true ? show open dialog : show save dialog
     * @return the created field.
     * @return
     */
    public static JComponent createFileSelectionField(final String title,
        final ValueModel fileSelectionModel, final int fileSelectionMode,
        final FileFilter fileFilter, final ActionListener preEventListener,
        final ActionListener postEventListener, final boolean open)
    {
        if (fileSelectionModel == null) {
            throw new NullPointerException("Filebase value model is null");
        }
        if (fileSelectionModel.getValue() != null
            && !(fileSelectionModel.getValue() instanceof String))
        {
            throw new IllegalArgumentException(
                "Value of fileselection is not of type String");
        }

        FormLayout layout = new FormLayout("100dlu, 4dlu, 15dlu", "pref");
        PanelBuilder builder = new PanelBuilder(layout);

        // The textfield
        final JTextField textField = BasicComponentFactory.createTextField(
            fileSelectionModel, false);
        textField.setEditable(false);
        Dimension p = textField.getPreferredSize();
        p.width = Sizes.dialogUnitXAsPixel(30, textField);
        textField.setPreferredSize(p);

        // The button
        final JButton button = new JButtonMini(Icons.DIRECTORY, Translation
            .getTranslation("folder_create.dialog.select_file.text"));
        Dimension d = button.getPreferredSize();
        d.height = textField.getPreferredSize().height;
        button.setPreferredSize(d);

        // Button logic
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Call additional listener
                if (preEventListener != null) {
                    preEventListener.actionPerformed(e);
                }

                File fileSelection = null;
                if (fileSelectionModel.getValue() != null) {
                    fileSelection = new File((String) fileSelectionModel
                        .getValue());
                }

                JFileChooser fileChooser = DialogFactory
                    .createFileChooser();
                fileChooser.setFileSelectionMode(fileSelectionMode);

                if (fileSelection != null) {
                    fileChooser.setSelectedFile(fileSelection);
                    fileChooser.setCurrentDirectory(fileSelection);
                }

                fileChooser.setDialogTitle(title);
                fileChooser.setFileSelectionMode(fileSelectionMode);
                if (fileFilter != null) {
                    fileChooser.setFileFilter(fileFilter);
                }
                int result;
                if (open) {
                    result = fileChooser.showOpenDialog(button);
                } else {
                    result = fileChooser.showSaveDialog(button);
                }

                File selectedFile = fileChooser.getSelectedFile();

                if (result == JFileChooser.APPROVE_OPTION
                    && selectedFile != null)
                {
                    fileSelectionModel.setValue(selectedFile
                        .getAbsolutePath());
                }

                if (postEventListener != null) {
                    postEventListener.actionPerformed(e);
                }
            }
        });

        CellConstraints cc = new CellConstraints();
        builder.add(textField, cc.xy(1, 1));
        builder.add(button, cc.xy(3, 1));

        JPanel panel = builder.getPanel();
        panel.addPropertyChangeListener("enabled", new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent evt) {
                boolean enabled = (Boolean) evt.getNewValue();
                textField.setEnabled(enabled);
                button.setEnabled(enabled);
            }
        });
        return panel;
    }
}