package de.dal33t.powerfolder.util.ui;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
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
     * Shows a general message dialog.
     *
     * @param parent
     * @param title
     * @param text
     * @param messageType ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE,
     *                    QUESTION_MESSAGE, or PLAIN_MESSAGE
     */
    public static void showMessageDialog(Component parent, String title,
                                         String text, int messageType) {
        try {
            dialogInUse.set(true);
            JOptionPane.showMessageDialog(parent, text, title, messageType);
        } finally {
            dialogInUse.set(false);
        }
    }

    /**
     * Displays an error dialog with the throwable message if verbose mode
     *
     * @param parent
     * @param verbose
     * @param title
     * @param text
     * @param throwable
     */
    public static void showErrorMessage(Component parent, boolean verbose,
                                        String title, String text,
                                        Throwable throwable) {
        String innerText;
        if (verbose && throwable != null) {
            innerText = text + "\nReason: " + throwable.toString();
        } else {
            innerText = text;
        }

        showMessageDialog(
                parent,
                title,
                innerText,
                JOptionPane.ERROR_MESSAGE);
    }


    /**
     * Shows a yes no (cancel) dialog.
     *
     * @param parent
     * @param title
     * @param text
     * @param optionType  OK_CANCEL_OPTION, YES_NO_OPTION, or
     *                    YES_NO_CANCEL_OPTION
     * @param messageType ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE,
     *                    QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @return the return value of JOptionPane.
     * @deprecated Use showOptionDialog.
     *             Instead of perhaps
     *             showConfirmDialog(Do you want to delete folder? [Yes / No]),
     *             do something like
     *             showOptionDialog(Folder will be deleted. [Delete Folder / Cancel]).
     */
    @Deprecated
    public static int showConfirmDialog(Component parent, String title,
                                        String text, int optionType,
                                        int messageType) {
        try {
            dialogInUse.set(true);
            return JOptionPane.showConfirmDialog(parent, text, title, optionType,
                    messageType);
        } finally {
            dialogInUse.set(false);
        }
    }

    /**
     * Shows an option dialog.
     * Note: Always consider CLOSED_OPTION result.
     * CLOSED_OPTION has an integer value of 2,
     * which equates to a third option button.
     *
     * @param parent
     * @param title
     * @param text
     * @param messageType  ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE,
     *                     QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param optionTexts  the text to be displayed on each of the option buttons
     * @param initialValue the default option button number
     * @return an integer indicating the option chosen by the user,
     *         or CLOSED_OPTION if the user closed the dialog.
     */
    public static int showOptionDialog(Component parent, String title,
                                       String text, int messageType,
                                       String[] optionTexts, int initialValue) {

        // Show option dialog.
        try {
            dialogInUse.set(true);
            return JOptionPane.showOptionDialog(parent, text, title,
                    JOptionPane.DEFAULT_OPTION, messageType, null, optionTexts,
                    optionTexts[initialValue]);
        } finally {
            dialogInUse.set(false);
        }
    }

    /**
     * Shows an OK message dialog,
     * asks if should be shown never again.
     *
     * @param parent
     * @param title
     * @param message
     * @param showNeverAgainText
     * @return
     */
    public static NeverAskAgainResponse showNeverAskAgainMessageDialog(
            Frame parent, String title,
            String message, String showNeverAgainText) {
        return showNeverAskAgainMessageDialog(parent, title, message, showNeverAgainText,
                new String[]{Translation.getTranslation("general.ok")});
    }

    /**
     * Shows an option dialog,
     * asks if should be shown never again.
     *
     * @param parent
     * @param title
     * @param message
     * @param showNeverAgainText
     * @param optionTexts
     * @return
     */
    public static NeverAskAgainResponse showNeverAskAgainMessageDialog(
            Frame parent, String title,
            String message, String showNeverAgainText,
            String[] optionTexts) {
        try {
            dialogInUse.set(true);
            NeverAskAgainMessageDialog neverAskAgainMessageDialog = new NeverAskAgainMessageDialog(
                    parent, title, message, showNeverAgainText, optionTexts);
            neverAskAgainMessageDialog.setVisible(true);
            return neverAskAgainMessageDialog.getResponse();
        } finally {
            dialogInUse.set(false);
        }
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

        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(new JLabel(message), cc.xy(1, 1));
        JPanel panel = builder.getPanel();

        return genericDialog(parent, title, panel, options, defaultOption,
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

        GenericDialog dialog = new GenericDialog(parent, title, panel, type,
                options, defaultOption, null);

        return dialog.display();
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
        FormLayout layout = new FormLayout("pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(new JLabel(message), cc.xy(1, 1));
        JPanel panel = builder.getPanel();

        return genericDialog(parent, title, panel, options, defaultOption,
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

        GenericDialog dialog = new GenericDialog(parent, title, panel, type,
                options, defaultOption, neverAskAgainMessage);

        return new NeverAskAgainResponse(dialog.display(),
                dialog.isNeverAskAgain());
    }
}