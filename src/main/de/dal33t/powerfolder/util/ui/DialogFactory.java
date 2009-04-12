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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Help;
import de.javasoft.synthetica.addons.DirectoryChooser;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Provides some convenient one method access to some dialogs.
 * <p>
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @author <A HREF="mailto:harry@powerfolder.com">Harry</A>
 * @version $Revision: 1.3 $
 */
public class DialogFactory {

    /**
     * Shows an OK / CANCEL dialog with a long text in a JTextArea.
     *
     * @param parent the parent frame. May safely be null if necessary, but prefer object to be modal on.
     * @param title the dialog title
     * @param message the dialog message
     * @param longText long (scrollable) text
     * @return the index of the selected option button, -1 if dialog cancelled
     */
    public static int showScrollableOkCancelDialog(Controller controller,
        String title, String message,
        String longText)
    {

        JTextArea textArea = new JTextArea(longText, 10, 30);
        textArea.setBackground(Color.WHITE);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        FormLayout layout = new FormLayout("pref", "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();
        builder.add(LinkedTextBuilder.build(controller, message).getPanel(), cc.xy(1, 1));
        builder.add(scrollPane, cc.xy(1, 3));

        return genericDialog(controller, title, builder.getPanel(),
                new String[]{Translation.getTranslation("general.ok"),
                Translation.getTranslation("general.cancel")},
                0, GenericDialogType.QUESTION);
    }

    /**
     * Opens a DirectoryChooser with the current file and returns the new
     * selection. This will return the original value if nothing is selected.
     *
     * @param controller the controller, used to get the parent frame
     * @param initialDirectory
     * @return the chosen directory
     */
    public static String chooseDirectory(Controller controller,
        String initialDirectory)
    {
        DirectoryChooser dc = new DirectoryChooser();
        if (initialDirectory != null &&
                initialDirectory.trim().length() != 0) {
            dc.setCurrentDirectory(new File(initialDirectory));
        }
        int i = dc.showOpenDialog(controller.getUIController()
                .getActiveFrame());
        if (i == JFileChooser.APPROVE_OPTION) {
            return dc.getSelectedFile().getAbsolutePath();
        }
        return initialDirectory;
    }

    /**
     * The prefered way to create a FileChooser in PowerFolder. Will override
     * the default look and feel of the FileChooser component on Windows to
     * windows look and feel. This adds the buttons like "recent", "My
     * Documents" etc.
     *
     * Now uses the Synthetica chooser.
     *
     * @return a file chooser
     */
    public static JFileChooser createFileChooser() {
        return new JFileChooser();
    }

    // //////////////////////////////////////////////////////////////////
    // GenericDialog provides a standard dialog look for PowerFolder. //
    // If possible, use this instead of JOptionPanel, etc //
    // //////////////////////////////////////////////////////////////////

    /**
     * Generic dialog with message and OK button.
     * 
     * @param parent the parent frame. May safely be null if necessary, but prefer object to be modal on.
     * @param title the title for the dialog
     * @param message the message to display in the dialog
     * @param type a {@link GenericDialogType}
     */
    public static void genericDialog(Controller controller, String title,
        String message, GenericDialogType type)
    {

        genericDialog(controller, title, message, new String[]{Translation
            .getTranslation("general.ok")}, 0, type);
    }

    /**
     * Generic dialog with message and throwable and OK button. The throwable is
     * only shown in verbose mode.
     * 
     * @param parent the parent frame. May safely be null if necessary, but prefer object to be modal on.
     * @param title the title for the dialog
     * @param message the message to display in the dialog
     * @param verbose whether the full stack trace should be displayed if in verbose mode
     * @param throwable the throwable that is to be displayed in verbose mode
     */
    public static void genericDialog(Controller controller, String title,
        String message, boolean verbose, Throwable throwable)
    {

        String innerText;
        if (verbose && throwable != null) {
            innerText = message + "\nReason: " + throwable.toString();
        } else {
            innerText = message;
        }

        genericDialog(controller, title, innerText, new String[]{Translation
            .getTranslation("general.ok")}, 0, GenericDialogType.ERROR);
    }

    /**
     * Generic dialog with message.
     *
     * @param parent the parent frame. May safely be null if necessary, but prefer object to be modal on.
     * @param title the title for the dialog
     * @param message the message to display in the dialog
     * @param options array of strings that will be displayed on a sequential bar of buttons
     * @param defaultOption the index of the option that is the default highlighted button
     * @param type a {@link GenericDialogType}
     * @return the index of the selected option button, -1 if dialog cancelled
     */
    public static int genericDialog(Controller controller, String title,
        String message, String[] options, int defaultOption,
        GenericDialogType type) {
        return genericDialog(controller, title, message, options, defaultOption,
                null, type);
    }

    /**
     * Generic dialog with message.
     *
     * @param parent the parent frame. May safely be null if necessary, but prefer object to be modal on.
     * @param title the title for the dialog
     * @param message the message to display in the dialog
     * @param options array of strings that will be displayed on a sequential bar of buttons
     * @param defaultOption the index of the option that is the default highlighted button
     * @param helpLink Help class link
     * @param type a {@link GenericDialogType}
     * @return the index of the selected option button, -1 if dialog cancelled
     */
    public static int genericDialog(Controller controller, String title,
        String message, String[] options, int defaultOption,
        String helpLink, GenericDialogType type)
    {

        PanelBuilder panelBuilder = LinkedTextBuilder.build(controller, message);
        return genericDialog(controller, title, panelBuilder.getPanel(), options,
            defaultOption, helpLink, type);
    }

    /**
     * Generic dialog with custom panel.
     *
     * @param parent the parent frame. May safely be null if necessary, but prefer object to be modal on.
     * @param title the title for the dialog
     * @param panel a panel that will be the displayed section of the dialog right of icon and above buttons
     * @param options array of strings that will be displayed on a sequential bar of buttons
     * @param defaultOption the index of the option that is the default highlighted button
     * @param type a {@link GenericDialogType}
     * @return the index of the selected option button, -1 if dialog cancelled
     */
    public static int genericDialog(Controller controller, String title,
                                    JPanel panel, String[] options,
                                    int defaultOption, GenericDialogType type) {
        return genericDialog(controller, title, panel, options, defaultOption,
                null, type);
    }

    /**
     * Generic dialog with custom panel.
     *
     * @param parent the parent frame. May safely be null if necessary, but prefer object to be modal on.
     * @param title the title for the dialog
     * @param panel a panel that will be the displayed section of the dialog right of icon and above buttons
     * @param options array of strings that will be displayed on a sequential bar of buttons
     * @param defaultOption the index of the option that is the default highlighted button
     * @param type a {@link GenericDialogType}
     * @return the index of the selected option button, -1 if dialog cancelled
     */
    public static int genericDialog(Controller controller, String title, JPanel panel,
        String[] options, int defaultOption, String helpLink, GenericDialogType type)
    {
        JButton helpButton = null;
        if (helpLink != null) {
            helpButton = Help.createWikiLinkButton(controller, helpLink);
        }
        GenericDialog dialog = new GenericDialog(controller
                .getUIController().getActiveFrame(), title, panel,
            type, options, defaultOption, null, helpButton);
        return dialog.display();
    }

    /**
     * Generic dialog with 'never ask again' checkbox.
     * 
     * @param parent the parent frame. May safely be null if necessary, but prefer object to be modal on.
     * @param title the title for the dialog
     * @param message the message to display in the dialog
     * @param options array of strings that will be displayed on a sequential bar of buttons
     * @param defaultOption the index of the option that is the default highlighted button
     * @param type a {@link GenericDialogType}
     * @param neverAskAgainMessage the message to display in the 'never ask again' checkbox
     * @return {@link NeverAskAgainResponse} with 'never ask again' checkbox selection and selected button index (-1 if dialog cancelled)
     */
    public static NeverAskAgainResponse genericDialog(Controller controller,
        String title, String message, String[] options, int defaultOption,
        GenericDialogType type, String neverAskAgainMessage)
    {

        PanelBuilder panelBuilder = LinkedTextBuilder.build(controller, message);
        return genericDialog(controller, title, panelBuilder.getPanel(), options,
            defaultOption, type, neverAskAgainMessage);
    }

    /**
     * Generic dialog with custom panle and 'never ask again' checkbox.
     * 
     * @param parent the parent frame. May safely be null if necessary, but prefer object to be modal on.
     * @param title the title for the dialog
     * @param panel a panel that will be the displayed section of the dialog right of icon and above buttons
     * @param options array of strings that will be displayed on a sequential bar of buttons
     * @param defaultOption the index of the option that is the default highlighted button
     * @param type a {@link GenericDialogType}
     * @param neverAskAgainMessage the message to display in the 'never ask again' checkbox
     * @return {@link NeverAskAgainResponse} with 'never ask again' checkbox selection and selected button index (-1 if dialog cancelled)
     */
    public static NeverAskAgainResponse genericDialog(Controller controller,
        String title, JPanel panel, String[] options, int defaultOption,
        GenericDialogType type, String neverAskAgainMessage)
    {
        GenericDialog dialog = new GenericDialog(controller
                .getUIController().getActiveFrame(), title, panel,
            type, options, defaultOption, neverAskAgainMessage, null);

        return new NeverAskAgainResponse(dialog.display(), dialog
            .isNeverAskAgain());
    }
}