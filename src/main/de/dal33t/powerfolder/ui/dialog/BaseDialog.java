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
package de.dal33t.powerfolder.ui.dialog;

import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.CursorUtils;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Translation;

/**
 * A basic class for all dialogs. It offers the posibility to set and icon and
 * create a customized ButtonBar.
 * <p>
 * Overwrite the abstract methods. A graphical help, how the elemnts will be
 * aligned is available at the developer documentation.
 * <p>
 * Link: http://download.powerfolder.com/development-docs/BaseDialogHelp.GIF
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public abstract class BaseDialog extends PFUIComponent {

    private static final AtomicInteger NUMBER_OF_OPEN_DIALOGS =
            new AtomicInteger();

    /**
     * Used to decide which owner the dialog gets.
     */
    public enum Senior {
        NONE,
        MAIN_FRAME
    }

    private final Senior senior;
    private final boolean modal;
    private boolean resizable;

    protected JDialog dialog;

    // Make sure open / close count change fires exactly once per instance.
    private final AtomicBoolean doneWizardClose = new AtomicBoolean();

    /**
     * Are there any open dilogs?
     *
     * @return
     */
    public static boolean isDialogOpen() {
        return NUMBER_OF_OPEN_DIALOGS.get() > 0;
    }


    /**
     * Initializes the base dialog.
     *
     * @param senior
     *            the component to be modal to and to locate on
     * @param controller
     *            the controller
     * @param modal
     *            if dialog should be modal
     */
    protected BaseDialog(Senior senior, Controller controller, boolean modal) {
        super(controller);
        this.senior = senior;
        this.modal = modal;
        NUMBER_OF_OPEN_DIALOGS.incrementAndGet();
        //controller.getUIController().getMainFrame().checkOnTop();
    }

    /**
     * Initializes the base dialog.
     *
     * @param senior
     *            the component to be modal to and to locate on\
     * @param controller
     *            the controller
     * @param modal
     *            if dialog should be modal
     * @param resizable
     *           if dialog is resizable
     */
    protected BaseDialog(Senior senior, Controller controller, boolean modal,
                         boolean resizable) {
        this(senior, controller, modal);
        this.resizable = resizable;
    }

    /**
     * Make absolutely sure decrementOpenDialogs() gets called.
     * Should have been called by Window closed / closing.
     *
     * @throws Throwable
     */
    protected void finalize() throws Throwable {
        try{
            decrementOpenDialogCount();
        } finally {
            super.finalize();
        }
    }

    private void decrementOpenDialogCount() {
        if (!doneWizardClose.getAndSet(true)) {
            NUMBER_OF_OPEN_DIALOGS.decrementAndGet();
            //getController().getUIController().getMainFrame().checkOnTop();
        }
    }

    // Abstract methods *******************************************************

    /**
     * The title of this base dialog
     *
     * @return
     */
    public abstract String getTitle();

    /**
     * Returns the icon of this dialog Mehod may return null, no icon will be
     * displayed
     *
     * @return
     */
    protected abstract Icon getIcon();

    /**
     * Method should return the main content of this panel displayed on the
     * right side over the buttonbar.
     *
     * @return component
     */
    protected abstract JComponent getContent();

    /**
     * This is the button to make the default button for the dialog.
     *
     * @return default button
     */
    protected abstract JButton getDefaultButton();

    /**
     * Method should return the button bar on the lower side of the dialog
     *
     * @return the component
     */
    protected abstract Component getButtonBar();

    // Helper methods *********************************************************

    /**
     * Creates an internationlaized ok button
     *
     * @param listener
     *            the listener to be put on the button
     * @return
     */
    protected static JButton createOKButton(ActionListener listener) {
        JButton okButton = new JButton(Translation.get("general.ok"));
        okButton.setMnemonic(Translation.get("general.ok.key")
            .trim().charAt(0));
        okButton.addActionListener(listener);
        return okButton;
    }

    /**
     * Creates an internationlaized cancel button
     *
     * @param listener
     *            the listener to be put on the button
     * @return
     */
    protected static JButton createCancelButton(ActionListener listener) {
        JButton cancelButton = new JButton(Translation
            .get("general.cancel"));
        cancelButton.setMnemonic(Translation.get(
            "general.cancel.key").trim().charAt(0));
        cancelButton.addActionListener(listener);
        return cancelButton;
    }

    /**
     * Creates an internationlaized close button
     *
     * @param listener
     *            the listener to be put on the button
     * @return
     */
    protected static JButton createCloseButton(ActionListener listener) {
        JButton closeButton = new JButton(Translation
            .get("general.close"));
        closeButton.setMnemonic(Translation.get("general.close.key")
            .trim().charAt(0));
        closeButton.addActionListener(listener);
        return closeButton;
    }

    /**
     * Shows (and builds) the dialog.
     */
    public JDialog open() {
        Window window = getUIController().getActiveFrame();
        Cursor c = CursorUtils.setWaitCursor(window);
        try {
            createUIComponent();
            UIUtil.putOnScreen(dialog);
            dialog.setVisible(true);
            return dialog;
        } finally {
            CursorUtils.returnToOriginal(window, c);
        }
    }

    /**
     * Disposes the dialog.
     */
    public void close() {
        if (dialog != null) {
            dialog.dispose();
            dialog = null;
        }
    }

    private class CloseAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            close();
        }
    }

    protected void rePack() {
        if (dialog == null) {
            throw new IllegalStateException("Must be open to rePack");
        }
        dialog.pack();
    }

    /**
     * Build
     *
     * @return
     */
    private void createUIComponent() {
        Window owner = null;
        if (senior == Senior.MAIN_FRAME) {
            owner = getUIController().getMainFrame().getUIComponent();
        }
        dialog = new JDialog(owner,
                getTitle(), modal
                        ? ModalityType.APPLICATION_MODAL
                        : ModalityType.MODELESS);
        dialog.setResizable(resizable);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        FormLayout layout = new FormLayout("pref, pref:grow",
                "fill:pref:grow, 10dlu, fill:pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        Icon icon = getIcon();
        JLabel iconLabel = icon != null ? new JLabel(getIcon()) : null;
        if (iconLabel != null) {
            iconLabel.setBorder(Borders
                    .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            builder.add(iconLabel, cc.xywh(1, 1, 1, 1, "right, top"));
        }

        JComponent content = getContent();
        content.setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
        builder.add(content, cc.xy(2, 1));
        Component buttonBar = getButtonBar();
        ((JComponent) buttonBar).setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
        builder.add(buttonBar, cc.xyw(1, 3, 2));

        // Add panel to component
        dialog.getContentPane().add(builder.getPanel());

        dialog.getRootPane().setDefaultButton(getDefaultButton());

        // Add escape key as close
        KeyStroke strokeEsc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JComponent rootPane = dialog.getRootPane();
        rootPane.registerKeyboardAction(new CloseAction(), strokeEsc,
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.pack();
        int ownerX;
        int ownerY;
        int ownerWidth;
        int ownerHeight;

        if (owner != null && owner.isVisible() &&
                senior == Senior.MAIN_FRAME) {
            ownerX = owner.getX();
            ownerY = owner.getY();
            ownerWidth = owner.getWidth();
            ownerHeight = owner.getHeight();
        } else {
            // Senior.NONE centers dialog on the screen.
            ownerX = 0;
            ownerY = 0;
            ownerWidth =
                    (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
            ownerHeight =
                    (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        }
        int x = ownerX + (ownerWidth - dialog.getWidth()) / 2;
        int y = ownerY + (ownerHeight  - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);

        // Decrement open dialog count on close.
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                decrementOpenDialogCount();
            }
            public void windowClosing(WindowEvent e) {
                decrementOpenDialogCount();
            }
        });
    }
}