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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
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
    private JDialog dialog;
    private boolean modal;
    private boolean border;

    /**
     * Initializes the base dialog.
     * 
     * @param controller
     *            the controller
     * @param modal
     *            if dialog should be modal
     */
    protected BaseDialog(Controller controller, boolean modal) {
        // Default = with border
        this(controller, modal, true);
    }

    /**
     * Initializes the base dialog.
     * 
     * @param controller
     *            the controller
     * @param modal
     *            if dialog should be modal
     * @param border
     *            if a border should be added to the content
     */
    protected BaseDialog(Controller controller, boolean modal, boolean border) {
        super(controller);
        this.modal = modal;
        this.border = border;
    }

    // Abstract methods *******************************************************

    /**
     * The title of this basedialog
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
    protected abstract Component getContent();

    /**
     * Method should return the button bar on the lower side of the dialog
     * 
     * @return the component
     */
    protected abstract Component getButtonBar();

    /** overwrite amnd return null to allow resize */
    protected boolean allowResize() {
        return false;
    }

    // Helper methods *********************************************************

    /**
     * Creates an internationlaized ok button
     * 
     * @param listener
     *            the listener to be put on the button
     * @return
     */
    protected JButton createOKButton(ActionListener listener) {
        JButton okButton = new JButton(Translation.getTranslation("general.ok"));
        okButton.setMnemonic(Translation.getTranslation("general.ok.key")
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
    protected JButton createCancelButton(ActionListener listener) {
        JButton cancelButton = new JButton(Translation
            .getTranslation("general.cancel"));
        cancelButton.setMnemonic(Translation.getTranslation(
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
    protected JButton createCloseButton(ActionListener listener) {
        JButton closeButton = new JButton(Translation
            .getTranslation("general.close"));
        closeButton.setMnemonic(Translation.getTranslation("general.close.key")
            .trim().charAt(0));
        closeButton.addActionListener(listener);
        return closeButton;
    }

    // Own methods ************************************************************

    /**
     * Shows (and builds) the dialog
     */
    public final void open() {
        Window window = getUIController().getActiveFrame();
        Cursor c = window.getCursor();
        try {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            getUIComponent().setVisible(true);
        } finally {
            window.setCursor(c);
        }
    }

    /**
     * Disposes the dialog.
     */
    public final void close() {
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

    /**
     * Enables/disables visibility of dialog
     * 
     * @param vis
     *            if the dialog should be visible
     */
    public final void setVisible(boolean vis) {
        if (dialog != null) {
            dialog.setVisible(vis);
        }
    }

    /**
     * Answers if this dialog is open
     * 
     * @return
     */
    protected final boolean isOpen() {
        return dialog != null;
    }

    protected void rePack() {
        if (!isOpen()) {
            throw new IllegalStateException("Must be open to rePack");
        }
        dialog.pack();
    }

    // Internal methods *******************************************************

    /**
     * Build (if nessesary) and returns the ui component
     * 
     * @return
     */
    protected final JDialog getUIComponent() {
        if (dialog == null) {
            dialog = new JDialog(getUIController().getActiveFrame(),
                    getTitle(), modal ? Dialog.ModalityType.APPLICATION_MODAL :
                    Dialog.ModalityType.MODELESS);
            dialog.setResizable(allowResize());
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            FormLayout layout = new FormLayout("pref, pref:grow",
                "pref:grow, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            Icon icon = getIcon();
            JLabel iconLabel = icon != null ? new JLabel(getIcon()) : null;
            if (iconLabel != null) {
                iconLabel.setBorder(Borders
                    .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
                builder.add(iconLabel, cc.xywh(1, 1, 1, 1, "right, top"));
            }

            Component content = getContent();
            if (border) {
                ((JComponent) content).setBorder(Borders
                    .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            }
            builder.add(content, cc.xy(2, 1));

            Component buttonBar = getButtonBar();
            ((JComponent) buttonBar).setBorder(Borders
                .createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
            builder.add(buttonBar, cc.xyw(1, 2, 2));

            // Add panel to component
            dialog.getContentPane().add(builder.getPanel());

            // Add escape key as close
            KeyStroke strokeEsc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            JComponent rootPane = dialog.getRootPane();
            rootPane.registerKeyboardAction(new CloseAction(), strokeEsc,
                JComponent.WHEN_IN_FOCUSED_WINDOW);

            dialog.pack();
            int x = ((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth()
                    - dialog.getWidth()) / 2;
            int y = ((int) Toolkit.getDefaultToolkit().getScreenSize().getHeight()
                    - dialog.getHeight())
                / 2;
            dialog.setLocation(x, y);

        }
        return dialog;
    }
}