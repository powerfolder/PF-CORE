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
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;

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
     * Creates a fileselection field especsially built to select basedir of a
     * folder. Suggests a basedir depending on name and recent settings about
     * folder
     * 
     * @param folderNameModel
     *            the model of folders name
     * @param fileBaseModel
     *            the model containing the base directory (Class File)
     * @param controller
     *            the controller
     * @return the selection field
     */
    public static JComponent createFolderBaseDirSelectionField(
        final ValueModel folderNameModel, final ValueModel fileBaseModel,
        final Controller controller)
    {
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        if (folderNameModel == null) {
            throw new NullPointerException("FolderNameModel is null");
        }
        if (fileBaseModel == null) {
            throw new NullPointerException("FileBaseModel is null");
        }
        ActionListener suggestor = new ActionListener() {
            private boolean shouldSuggest = true;

            public void actionPerformed(ActionEvent e) {
                // Suggest base dir only if basedir is currently empty
                shouldSuggest = StringUtils.isBlank((String) fileBaseModel
                    .getValue());
                if (shouldSuggest) {
                    String suggestedBase = null;
                    // Set base dir
                    String lastLocalBase = controller.getPreferences().get(
                        "folder." + (String) folderNameModel.getValue()
                            + ".last-localbase", null);
                    if (lastLocalBase != null) {

                        suggestedBase = lastLocalBase;
                    } else {
                        suggestedBase = controller.getFolderRepository()
                            .getFoldersBasedir()
                            + System.getProperty("file.separator")
                            + Util
                                .removeInvalidFilenameChars((String) folderNameModel
                                    .getValue());
                    }

                    if (suggestedBase != null) {
                        fileBaseModel.setValue(suggestedBase);
                    }
                }

                if (e != null) {
                    // Event was not manually in code fired. HACK
                    File fileBase = new File((String) fileBaseModel.getValue());
                    if (!fileBase.exists()) {
                        fileBase.mkdirs();
                    }
                }
            }
        };

        // Directly suggest if name is set
        if (!StringUtils.isBlank((String) folderNameModel.getValue())) {
            suggestor.actionPerformed(null);
        }

        return createDirectorySelectionField(Translation
            .getTranslation("general.localcopyplace"), fileBaseModel,
            suggestor, null);
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
        final ActionListener postEventListener)
    {
        return createFileSelectionField(title, fileBaseModel,
            JFileChooser.DIRECTORIES_ONLY, null, preEventListener,
            postEventListener);
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
        final ActionListener postEventListener)
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

        FormLayout layout = new FormLayout("pref:grow, 2dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);

        // The textfield
        final JTextField textField = BasicComponentFactory.createTextField(
            fileSelectionModel, false);
        textField.setEditable(false);
        Dimension p = textField.getPreferredSize();
        p.width = Sizes.dialogUnitXAsPixel(30, textField);
        textField.setPreferredSize(p);

        // The button
        final JButton button = new JButton("...");
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

                JFileChooser fileChooser = DialogFactory.createFileChooser();

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
                    fileSelectionModel.setValue(selectedFile.getAbsolutePath());
                }

                // If canceled, do nothing.

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
                boolean enabled = ((Boolean) evt.getNewValue()).booleanValue();
                textField.setEnabled(enabled);
                button.setEnabled(enabled);
            }
        });
        return panel;
    }

    /**
     * Creates a label which shows the online state of a controller
     * 
     * @param controller
     *            the controller.
     * @return the label.
     */
    public static JLabel createOnlineStateLabel(final Controller controller) {
        final JLabel label = SimpleComponentFactory.createLabel();

        NodeManagerListener nodeListener = new NodeManagerListener() {
            public void friendAdded(NodeManagerEvent e) {
            }

            public void friendRemoved(NodeManagerEvent e) {
            }

            public void nodeAdded(NodeManagerEvent e) {
                updateOnlineStateLabel(label, controller);
            }

            public void nodeConnected(NodeManagerEvent e) {
                updateOnlineStateLabel(label, controller);
            }

            public void nodeDisconnected(NodeManagerEvent e) {
                updateOnlineStateLabel(label, controller);
            }

            public void nodeRemoved(NodeManagerEvent e) {
                updateOnlineStateLabel(label, controller);
            }

            public void settingsChanged(NodeManagerEvent e) {
            }

            public boolean fireInEventDispathThread() {
                return true;
            }
        };
        // set initial values
        updateOnlineStateLabel(label, controller);

        // Add behavior
        controller.getNodeManager().addNodeManagerListener(nodeListener);

        return label;
    }

    private static void updateOnlineStateLabel(JLabel label,
        Controller controller)
    {
        // Get connectes node count
        int nOnlineUser = controller.getNodeManager().countConnectedNodes();

        // System.err.println("Got " + nOnlineUser + " online users");
        if (nOnlineUser > 0) {
            label.setText(Translation.getTranslation("onlinelabel.online"));
            label.setIcon(Icons.CONNECTED);
            label.setToolTipText(Translation
                .getTranslation("onlinelabel.online.text"));
        } else {
            label.setText(Translation.getTranslation("onlinelabel.connecting"));
            label.setIcon(Icons.DISCONNECTED);
            label.setToolTipText(Translation
                .getTranslation("onlinelabel.connecting.text"));
        }
    }

    public static JLabel createTransferCounterLabel(
        final Controller controller, final String format,
        final TransferCounter tc)
    {
        final JLabel label = new JLabel();
        // Create task which updates the counter each second
        controller.scheduleAndRepeat(new TimerTask() {
            @Override
            public void run()
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        label.setText(String.format(format, Double.valueOf(tc
                            .calculateCurrentKBS())));
                    }
                });
            }
        }, 0, 1000);
        return label;
    }
}