/* $Id: ComplexComponentFactory.java,v 1.19 2006/03/03 14:56:01 schaatser Exp $
 */
package de.dal33t.powerfolder.util.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import de.dal33t.powerfolder.Controller;
import static de.dal33t.powerfolder.disk.FolderSettings.*;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;

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
     * @param fileBaseModel
     *            the file base value model, will get/write base as String
     * @param additionalBrowseButtonListener
     *            an optional additional listern for the browse button
     * @return the create field.
     */
    public static JComponent createDirectorySelectionField(final String title,
        final ValueModel fileBaseModel, final ActionListener preEventListener,
        final ActionListener postEventListener, final Controller controller)
    {
        return createFileSelectionField(title, fileBaseModel,
            JFileChooser.DIRECTORIES_ONLY, null, preEventListener,
            postEventListener, controller);
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
     * @return the created field.
     */
    public static JComponent createFileSelectionField(final String title,
        final ValueModel fileSelectionModel, final int fileSelectionMode,
        final FileFilter fileFilter, final ActionListener preEventListener,
        final ActionListener postEventListener, final Controller controller)
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
        final JButton button = new JButton(Icons.DIRECTORY);
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

                // Temporary hack to fix possible issue with Leopard OS
                // if (fileSelectionMode == JFileChooser.DIRECTORIES_ONLY) {
                if (fileSelectionMode == JFileChooser.DIRECTORIES_ONLY
                    && OSUtil.isWindowsSystem())
                {

                    // Use the new Directory tree dialog
                    String file;
                    if (fileSelectionModel.getValue() == null) {
                        file = DialogFactory.chooseDirectory(controller, null);
                    } else {
                        file = DialogFactory.chooseDirectory(controller,
                            (String) fileSelectionModel.getValue());
                    }

                    fileSelectionModel.setValue(file);

                } else {

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
                    int result = fileChooser.showOpenDialog(button);
                    File selectedFile = fileChooser.getSelectedFile();

                    if (result == JFileChooser.APPROVE_OPTION
                        && selectedFile != null)
                    {
                        fileSelectionModel.setValue(selectedFile
                            .getAbsolutePath());
                    }

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

    public static JLabel createTransferCounterLabel(
        final Controller controller, final String format,
        final TransferCounter tc)
    {
        final JLabel label = new JLabel();
        // Create task which updates the counter each second
        controller.scheduleAndRepeat(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        label.setText(String.format(format, tc
                            .calculateCurrentKBS()));
                    }
                });
            }
        }, 0, 1000);
        return label;
    }
}