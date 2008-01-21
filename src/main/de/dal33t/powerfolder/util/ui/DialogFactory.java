package de.dal33t.powerfolder.util.ui;

import java.awt.Component;
import java.awt.Frame;

import javax.swing.*;

import sun.swing.WindowsPlacesBar;

import com.sun.java.swing.plaf.windows.WindowsFileChooserUI;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.directory.DirectoryChooser;
import de.dal33t.powerfolder.util.Translation;

/**
 * Provides some convenient one method access to some dialogs.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class DialogFactory {

    /**
     * Shows a general message dialog.
     *
     * @param parent
     * @param title
     * @param text
     * @param messageType  ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE,
     * QUESTION_MESSAGE, or PLAIN_MESSAGE
     */
    public static void showMessageDialog(Component parent, String title,
                                         String text, int messageType) {
        JOptionPane.showMessageDialog(parent, text, title, messageType);
    }

    /**
     * Displays an error dialog with the throwable message if verbose mode
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
     * @deprecated Use showOptionDialog.
     * Instead of perhaps 
     * showConfirmDialog(Do you want to delete folder? [Yes / No]),
     * do something like
     * showOptionDialog(Folder will be deleted. [Delete Folder / Cancel]).
     *
     * @param parent
     * @param title
     * @param text
     * @param optionType OK_CANCEL_OPTION, YES_NO_OPTION, or
     * YES_NO_CANCEL_OPTION
     * @param messageType ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE,
     * QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @return the return value of JOptionPane.
     */
    @Deprecated
    public static int showConfirmDialog(Component parent, String title,
                                        String text, int optionType,
                                        int messageType) {
        return JOptionPane.showConfirmDialog(parent, text, title, optionType,
                messageType);
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
     * @param messageType ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE,
     * QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @param optionTexts the text to be displayed on each of the option buttons
     * @param initialValue the default option button number
     * @return an integer indicating the option chosen by the user,
     * or CLOSED_OPTION if the user closed the dialog.
     */
    public static int showOptionDialog(Component parent, String title,
                                        String text, int messageType,
                                        String[] optionTexts, int initialValue) {

        // Show option dialog.
        return JOptionPane.showOptionDialog(parent, text, title,
                JOptionPane.DEFAULT_OPTION, messageType, null, optionTexts,
                optionTexts[initialValue]);
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
        NeverAskAgainMessageDialog neverAskAgainMessageDialog = new NeverAskAgainMessageDialog(
                parent, title, message, showNeverAgainText, optionTexts);
        neverAskAgainMessageDialog.setVisible(true);
        return neverAskAgainMessageDialog.getResponse();
    }

    /**
     * shows a OK / CANCEL dialog with a long text in a JTextArea.
     * 
     * @return a JOptionDialog compatible result either JOptionPane.OK_OPTION or
     *         JOptionPane.CANCEL_OPTION
     */
    public static int showScrollableOkCancelDialog(Controller controller,
        boolean modal, boolean border, String title, String message,
        String longText, Icon icon)
    {
        ScrollableOkCancelDialog scrollableOkCancelDialog = new ScrollableOkCancelDialog(
            controller, modal, border, title, message, longText, icon);
        scrollableOkCancelDialog.open();
        return scrollableOkCancelDialog.getChoice();
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
        ValueModel valueModel = new ValueHolder(initialDirectory);
        DirectoryChooser dc = new DirectoryChooser(controller, valueModel);
        dc.open();
        return (String) valueModel.getValue();
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
}