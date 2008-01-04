package de.dal33t.powerfolder.util.ui;

import java.awt.Component;
import java.awt.Frame;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import sun.swing.WindowsPlacesBar;

import com.sun.java.swing.plaf.windows.WindowsFileChooserUI;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.directory.DirectoryChooser;

/**
 * provides some convenient one method access to some dialogs.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class DialogFactory {

    /**
     * Shows a general warning dialog.
     */
    public static void showWarningDialog(Component parent, String title,
        String text) {
        JOptionPane.showMessageDialog(parent, text, title,
            JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Shows a general error dialog.
     */
    public static void showErrorDialog(Component parent, String title,
        String text)  {
        JOptionPane.showMessageDialog(parent, text, title,
            JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows a general information dialog.
     */
    public static void showInfoDialog(Component parent, String title,
        String text)  {
        JOptionPane.showMessageDialog(parent, text, title,
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a yes no (cancel) dialog.
     * 
     * @param parent
     * @param title
     * @param text
     * @param optionType OK_CANCEL_OPTION, YES_NO_OPTION, or YES_NO_CANCEL_OPTION
     * @param messageType ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
     * @return the return value of JOptionPane.
     */
    public static int showConfirmDialog(Component parent, String title,
                                        String text, int optionType, int messageType) {
        return JOptionPane.showConfirmDialog(
                parent, text, title, optionType, messageType);
    }

    /**
     * shows a message dialog, ask if should be shown never again.
     * 
     * @return true if should show this again, false if not
     */
    public static boolean showNeverAskAgainMessageDialog(Frame parent,
        String title, String message,
        String showNeverAgainText)
    {
        NeverAskAgainMessageDialog neverAskAgainMessageDialog = new NeverAskAgainMessageDialog(
            parent, title, message, showNeverAgainText);
        neverAskAgainMessageDialog.setVisible(true);
        return !neverAskAgainMessageDialog.showNeverAgain();
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