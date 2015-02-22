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
 * $Id: DialogFactory.java 20080 2012-11-09 13:05:49Z glasgow $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.UIController;
import de.dal33t.powerfolder.ui.dialog.directory.DirectoryChooser;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.util.LinkedTextBuilder;
import de.dal33t.powerfolder.ui.util.NeverAskAgainResponse;
import de.dal33t.powerfolder.util.Translation;

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
     * Opens a DirectoryChooser with the current dir and returns the new
     * selection. Returns null if operation is cancelled.
     *
     * @param uiController
     *            the ui controller, used to get the parent frame
     * @param initialDirectoryName
     *            optional name of the initial selected directory
     * @return the chosen directory
     */
    public static List<Path> chooseDirectory(UIController uiController,
        String initialDirectoryName, boolean multiSelect)
    {
        Path file = initialDirectoryName != null ? Paths.get(
            initialDirectoryName) : null;
        return chooseDirectory(uiController, file, multiSelect);
    }

    /**
     * Opens a DirectoryChooser with the current dir and returns the new
     * selection. Returns null if operation is cancelled.
     *
     * @param uiController
     *            the ui controller, used to get the parent frame
     * @param initialDirectory
     *            optional initial selected directory
     * @return the chosen directory
     */
    public static List<Path> chooseDirectory(UIController uiController,
        Path initialDirectory, boolean multiSelect) {
        return chooseDirectory(uiController, initialDirectory, null, multiSelect);
    }
    /**
     * Opens a DirectoryChooser with the current dir and returns the new
     * selection. Returns null if operation is cancelled. Also displays
     * virtual online folders.
     *
     * @param uiController
     *            the ui controller, used to get the parent frame
     * @param initialDirectory
     *            optional initial selected directory
     * @param onlineFolders
     *            optional list of online folder names that are rendered as
     *            globe icons. These are expected to be online folders in the
     *            PF base dir that a user may want to create.
     * @return the chosen directory
     */
    public static List<Path> chooseDirectory(UIController uiController, Path initialDirectory,
                                             List<String> onlineFolders, boolean multiSelect) {
        DirectoryChooser dc = new DirectoryChooser(uiController.getController(), initialDirectory, onlineFolders,
                multiSelect);
        dc.open();
        if (dc.getSelectedDirs() != null) {
            return dc.getSelectedDirs();
        }

        // Null selectedDirs indicates that user wants to use classic browser.
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(multiSelect);
        if (initialDirectory != null) {
            chooser.setCurrentDirectory(initialDirectory.toFile());
        }
        int i = chooser.showDialog(uiController.getActiveFrame(), Translation.get("general.select"));
        List<Path> selectedDirs = new ArrayList<Path>();
        if (i == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = chooser.getSelectedFiles();

            for (File f : selectedFiles) {
                selectedDirs.add(f.toPath());
            }
        }
        return selectedDirs;
    }

    /**
     * The preferred way to create a FileChooser in PowerFolder.
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
     * @param controller
     * @param title
     *            the title for the dialog
     * @param message
     *            the message to display in the dialog
     * @param type
     *            a {@link GenericDialogType}
     */
    public static void genericDialog(Controller controller, String title,
        String message, GenericDialogType type)
    {

        genericDialog(controller, title, message, new String[]{Translation
            .get("general.ok")}, 0, type);
    }

    /**
     * Generic dialog with message and throwable and OK button. The throwable is
     * only shown in verbose mode.
     *
     * @param controller
     * @param title
     *            the title for the dialog
     * @param message
     *            the message to display in the dialog
     * @param verbose
     *            whether the full stack trace should be displayed if in verbose
     *            mode
     * @param throwable
     *            the throwable that is to be displayed in verbose mode
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
            .get("general.ok")}, 0, GenericDialogType.ERROR);
    }

    /**
     * Generic dialog with message.
     *
     * @param controller
     * @param title
     *            the title for the dialog
     * @param message
     *            the message to display in the dialog
     * @param options
     *            array of strings that will be displayed on a sequential bar of
     *            buttons
     * @param defaultOption
     *            the index of the option that is the default highlighted button
     * @param type
     *            a {@link GenericDialogType}
     * @return the index of the selected option button, -1 if dialog cancelled
     */
    public static int genericDialog(Controller controller, String title,
        String message, String[] options, int defaultOption,
        GenericDialogType type)
    {
        return genericDialog(controller, title, message, options,
            defaultOption, null, type);
    }

    /**
     * Generic dialog with message.
     *
     * @param controller
     * @param title
     *            the title for the dialog
     * @param message
     *            the message to display in the dialog
     * @param options
     *            array of strings that will be displayed on a sequential bar of
     *            buttons
     * @param defaultOption
     *            the index of the option that is the default highlighted button
     * @param helpLink
     *            Help class link
     * @param type
     *            a {@link GenericDialogType}
     * @return the index of the selected option button, -1 if dialog cancelled
     */
    public static int genericDialog(Controller controller, String title,
        String message, String[] options, int defaultOption, String helpLink,
        GenericDialogType type)
    {

        PanelBuilder panelBuilder = LinkedTextBuilder
            .build(controller, message);
        return genericDialog(controller, title, panelBuilder.getPanel(),
            options, defaultOption, helpLink, type);
    }

    /**
     * Generic dialog with custom panel.
     *
     * @param controller
     * @param title
     *            the title for the dialog
     * @param panel
     *            a panel that will be the displayed section of the dialog right
     *            of icon and above buttons
     * @param options
     *            array of strings that will be displayed on a sequential bar of
     *            buttons
     * @param defaultOption
     *            the index of the option that is the default highlighted button
     * @param type
     *            a {@link GenericDialogType}
     * @return the index of the selected option button, -1 if dialog cancelled
     */
    public static int genericDialog(Controller controller, String title,
        JPanel panel, String[] options, int defaultOption,
        GenericDialogType type)
    {
        return genericDialog(controller, title, panel, options, defaultOption,
            null, type);
    }

    /**
     * Generic dialog with custom panel.
     *
     * @param controller
     * @param title
     *            the title for the dialog
     * @param panel
     *            a panel that will be the displayed section of the dialog right
     *            of icon and above buttons
     * @param options
     *            array of strings that will be displayed on a sequential bar of
     *            buttons
     * @param defaultOption
     *            the index of the option that is the default highlighted button
     * @param type
     *            a {@link GenericDialogType}
     * @return the index of the selected option button, -1 if dialog cancelled
     */
    public static int genericDialog(Controller controller, String title,
        JPanel panel, String[] options, int defaultOption, String helpLink,
        GenericDialogType type)
    {
        JButton helpButton = null;
        if (helpLink != null) {
            helpButton = Help.createWikiLinkButton(controller, helpLink);
        }
        GenericDialog dialog = new GenericDialog(controller.getUIController()
            .getActiveFrame(), title, panel, type, options, defaultOption,
            null, helpButton);
        return dialog.display();
    }

    /**
     * Generic dialog with 'never ask again' checkbox.
     *
     * @param controller
     * @param title
     *            the title for the dialog
     * @param message
     *            the message to display in the dialog
     * @param options
     *            array of strings that will be displayed on a sequential bar of
     *            buttons
     * @param defaultOption
     *            the index of the option that is the default highlighted button
     * @param type
     *            a {@link GenericDialogType}
     * @param neverAskAgainMessage
     *            the message to display in the 'never ask again' checkbox
     * @return {@link NeverAskAgainResponse} with 'never ask again' checkbox
     *         selection and selected button index (-1 if dialog cancelled)
     */
    public static NeverAskAgainResponse genericDialog(Controller controller,
        String title, String message, String[] options, int defaultOption,
        GenericDialogType type, String neverAskAgainMessage)
    {

        PanelBuilder panelBuilder = LinkedTextBuilder
            .build(controller, message);
        return genericDialog(controller, title, panelBuilder.getPanel(),
            options, defaultOption, type, neverAskAgainMessage);
    }

    /**
     * Generic dialog with custom panle and 'never ask again' checkbox.
     *
     * @param controller
     * @param title
     *            the title for the dialog
     * @param panel
     *            a panel that will be the displayed section of the dialog right
     *            of icon and above buttons
     * @param options
     *            array of strings that will be displayed on a sequential bar of
     *            buttons
     * @param defaultOption
     *            the index of the option that is the default highlighted button
     * @param type
     *            a {@link GenericDialogType}
     * @param neverAskAgainMessage
     *            the message to display in the 'never ask again' checkbox
     * @return {@link NeverAskAgainResponse} with 'never ask again' checkbox
     *         selection and selected button index (-1 if dialog cancelled)
     */
    public static NeverAskAgainResponse genericDialog(Controller controller,
        String title, JPanel panel, String[] options, int defaultOption,
        GenericDialogType type, String neverAskAgainMessage)
    {
        GenericDialog dialog = new GenericDialog(controller.getUIController()
            .getActiveFrame(), title, panel, type, options, defaultOption,
            neverAskAgainMessage, null);

        return new NeverAskAgainResponse(dialog.display(), dialog
            .isNeverAskAgain());
    }
}