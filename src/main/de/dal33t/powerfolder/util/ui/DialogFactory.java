package de.dal33t.powerfolder.util.ui;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.sun.java.swing.plaf.windows.WindowsFileChooserUI;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.directory.DirectoryChooser;
import sun.swing.WindowsPlacesBar;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides some convenient one method access to some dialogs.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class DialogFactory {

    /**
     * Used to give an INDICATION that a dialog is currently being shown.
     * It is not guaranteed because this would involve locks around dialogs,
     * which would be unwise.
     * USE WITH CAUSTION: Do not cause a Thread to wait for this to clear.
     */
    private static final AtomicBoolean dialogInUse = new AtomicBoolean();

    public static boolean isDialogInUse() {
        return dialogInUse.get();
    }

    /**
     * shows a OK / CANCEL dialog with a long text in a JTextArea.
     *
     * @return a JOptionDialog compatible result either JOptionPane.OK_OPTION or
     *         JOptionPane.CANCEL_OPTION
     */
    public static int showScrollableOkCancelDialog(Controller controller,
                                                   boolean modal, boolean border, String title, String message,
                                                   String longText, Icon icon) {
        try {
            dialogInUse.set(true);
            ScrollableOkCancelDialog scrollableOkCancelDialog = new ScrollableOkCancelDialog(
                    controller, modal, border, title, message, longText, icon);
            scrollableOkCancelDialog.open();
            return scrollableOkCancelDialog.getChoice();
        } finally {
            dialogInUse.set(false);
        }
    }

    /**
     * Opens a DirectoryChooser with the current file and returns the new selection.
     * This will return the original value if nothing is selected.
     *
     * @param controller
     * @param initialDirectory
     * @return
     */
    public static String chooseDirectory(Controller controller, String initialDirectory) {
        try {
            dialogInUse.set(true);
            ValueModel valueModel = new ValueHolder(initialDirectory);
            DirectoryChooser dc = new DirectoryChooser(controller, valueModel);
            dc.open();
            return (String) valueModel.getValue();
        } finally {
            dialogInUse.set(false);
        }
    }

    /**
     * The prefered way to create a FileChooser in PowerFolder. Will override
     * the default look and feel of the FileChooser component on Windows to
     * windows look and feel. This adds the buttons like "recent", "My
     * Documents" etc.
     */
    public static JFileChooser createFileChooser() {
        JFileChooser fc = new JFileChooser();
        if (OSUtil.isWindowsSystem()) {
            WindowsFileChooserUI winUI = (WindowsFileChooserUI) WindowsFileChooserUI
                    .createUI(fc);
            winUI.installUI(fc);
            // now fix some borders and decorations
            // makes nicer togglebuttons
            // removes border on the top toolbar
            for (int i = 0; i < fc.getComponentCount(); i++) {
                Component component = fc.getComponent(i);
                if (component instanceof WindowsPlacesBar) {
                    WindowsPlacesBar places = (WindowsPlacesBar) component;
                    // TODO detect if win XP style and
                    // set lighter background of this toolbar

                    // if win2K maybe some fillers are needed but don't
                    // know if we can insert them
                    ToggleButtonDecorator decorator = new ToggleButtonDecorator();
                    // loop all buttons in this bar
                    for (int j = 0; j < places.getComponentCount(); j++) {
                        Component placesButton = places.getComponent(j);
                        // make sure they are what we think
                        if (placesButton instanceof JToggleButton) {
                            JToggleButton jToggleButton = (JToggleButton) placesButton;
                            jToggleButton.addActionListener(decorator);
                            jToggleButton.addMouseListener(decorator);
                            decorator.updateButton(jToggleButton);
                        }
                    }
                } else if (component instanceof JToolBar) {
                    JToolBar toolbar = (JToolBar) component;
                    for (int j = 0; j < toolbar.getComponentCount(); j++) {
                        Component toolBarComponent = toolbar.getComponent(j);
                        // make sure its the top toolbar by detecting the
                        // combobox
                        if (toolBarComponent instanceof JComboBox) {
                            // remove border
                            toolbar.setBorder(null);
                        }
                    }
                }
            }
        }
        return fc;
    }

    ////////////////////////////////////////////////////////////////////
    // GenericDialog provides a standard dialog look for PowerFolder. //
    // If possible, use this instead of JOptionPanel, etc             //
    ////////////////////////////////////////////////////////////////////

    /**
     * Generic dialog with message and OK button.
     *
     * @param parent
     * @param title
     * @param message
     * @param type
     * @return
     */
    public static int genericDialog(JFrame parent,
                             String title,
                             String message,
                             GenericDialogType type) {

        return genericDialog(parent, title, message,
                new String[]{Translation.getTranslation("general.ok")}, 0,
                type);
    }

    /**
     * Generic dialog with message and throwable and OK button.
     * The throwable is only shown in verbose mode.
     *
     * @param parent
     * @param title
     * @param message
     * @param type
     * @param throwable
     * @return
     */
    public static int genericDialog(JFrame parent,
                             String title,
                             String message,
                             boolean verbose,
                             Throwable throwable) {

        String innerText;
        if (verbose && throwable != null) {
            innerText = message + "\nReason: " + throwable.toString();
        } else {
            innerText = message;
        }

        return genericDialog(parent, title, innerText,
                new String[]{Translation.getTranslation("general.ok")}, 0,
                GenericDialogType.ERROR);
    }

    /**
     * Generic dialog with message.
     *
     * @param parent
     * @param title
     * @param message
     * @param options
     * @param defaultOption
     * @param type
     * @return
     */
    public static int genericDialog(JFrame parent,
                             String title,
                             String message,
                             String[] options,
                             int defaultOption,
                             GenericDialogType type) {

        PanelBuilder panelBuilder = LinkedTextBuilder.build(message);
        return genericDialog(parent, title, panelBuilder.getPanel(), options, defaultOption,
                type);
    }

    /**
     * Generic dialog with custom panel.
     *
     * @param parent
     * @param title
     * @param panel
     * @param options
     * @param defaultOption
     * @param type
     * @return
     */
    public static int genericDialog(JFrame parent,
                             String title,
                             JPanel panel,
                             String[] options,
                             int defaultOption,
                             GenericDialogType type) {

        try {
            dialogInUse.set(true);
            GenericDialog dialog = new GenericDialog(parent, title, panel, type,
                    options, defaultOption, null);

            return dialog.display();
        } finally {
            dialogInUse.set(false);
        }
    }

    /**
     * Generic dialog with 'never ask again' checkbox.
     *
     * @param parent
     * @param title
     * @param message
     * @param options
     * @param defaultOption
     * @param type
     * @param neverAskAgainMessage
     * @return
     */
    public static NeverAskAgainResponse genericDialog(
            JFrame parent,
            String title,
            String message,
            String[] options,
            int defaultOption,
            GenericDialogType type,
            String neverAskAgainMessage) {

        PanelBuilder panelBuilder = LinkedTextBuilder.build(message);
        return genericDialog(parent, title, panelBuilder.getPanel(), options, defaultOption,
                type, neverAskAgainMessage);
    }

    /**
     * Generic dialog with custom panle and 'never ask again' checkbox.
     *
     * @param parent
     * @param title
     * @param panel
     * @param options
     * @param defaultOption
     * @param type
     * @param neverAskAgainMessage
     * @return
     */
    public static NeverAskAgainResponse genericDialog(
            JFrame parent,
            String title,
            JPanel panel,
            String[] options,
            int defaultOption,
            GenericDialogType type,
            String neverAskAgainMessage) {

        try {
            dialogInUse.set(true);
            GenericDialog dialog = new GenericDialog(parent, title, panel, type,
                    options, defaultOption, neverAskAgainMessage);

            return new NeverAskAgainResponse(dialog.display(),
                    dialog.isNeverAskAgain());

        } finally {
            dialogInUse.set(false);
        }
    }
}