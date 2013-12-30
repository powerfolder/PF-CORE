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

import static de.dal33t.powerfolder.ui.dialog.GenericDialogType.DEFAULT;
import static de.dal33t.powerfolder.ui.dialog.GenericDialogType.ERROR;
import static de.dal33t.powerfolder.ui.dialog.GenericDialogType.INFO;
import static de.dal33t.powerfolder.ui.dialog.GenericDialogType.QUESTION;
import static de.dal33t.powerfolder.ui.dialog.GenericDialogType.WARN;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Replacement class for JOptionPane.
 */
public class GenericDialog {

    /**
     * Result if user simply clicks the close button top-right on dialog
     */
    public static final int NO_BUTTON_CLICKED_INDEX = -1;

    private static final Icon INFO_ICON_ICON;
    private static final Icon WARN_ICON_ICON;
    private static final Icon ERROR_ICON_ICON;
    private static final Icon QUESTION_ICON_ICON;

    private Window parent;
    private boolean neverAskAgainMode;
    private JCheckBox neverAskAgainCheckBox;
    private String title;
    private Icon icon;
    private JPanel innerPanel;
    private int initialSelection;
    private JButton[] buttons;
    private JButton helpButton;
    private ValueModel buttonModel;
    private JDialog dialog;
    private String[] mnemonics;
    private String neverAskAgainMnemonic;

    // Cache icons from the local system.
    static {
        INFO_ICON_ICON = UIManager.getIcon("OptionPane.informationIcon");
        WARN_ICON_ICON = UIManager.getIcon("OptionPane.warningIcon");
        ERROR_ICON_ICON = UIManager.getIcon("OptionPane.errorIcon");
        QUESTION_ICON_ICON = UIManager.getIcon("OptionPane.questionIcon");
    }

    /**
     * Generic dialog.
     * Flexible replacement for JOptionPane.
     * Should only be accessed via DialogFactory.
     *
     * @param parent
     * @param title
     * @param innerPanel
     * @param type
     * @param options
     * @param initialSelection
     * @param neverAskAgainText
     * @param helpLink
     */
    public GenericDialog(Window parent,
                         String title,
                         JPanel innerPanel,
                         GenericDialogType type,
                         String[] options,
                         int initialSelection,
                         String neverAskAgainText,
                         JButton helpButton) {

        this.parent = parent;
        this.title = title;
        this.innerPanel = innerPanel;
        this.initialSelection = initialSelection;
        this.helpButton = helpButton;

        validateArgs(options);

        resolveMnemonics(options, neverAskAgainText);

        initComponents(neverAskAgainText, type, options);
    }

    private void initComponents(String neverAskAgainText,
                                GenericDialogType type,
                                String[] options) {

        buttonModel = new ValueHolder(NO_BUTTON_CLICKED_INDEX);

        neverAskAgainMode = neverAskAgainText != null &&
                neverAskAgainText.trim().length() > 0;

        if (DEFAULT == type) {
            icon = null;
        } else if (INFO == type) {
            icon = INFO_ICON_ICON;
        } else if (WARN == type) {
            icon = WARN_ICON_ICON;
        } else if (ERROR == type) {
            icon = ERROR_ICON_ICON;
        } else if (QUESTION == type) {
            icon = QUESTION_ICON_ICON;
        } else {
            throw new IllegalArgumentException("Invalid type. Could not find icon for " + type);
        }

        buttons = new JButton[options.length];
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            String mnemonic = mnemonics[i];
            final int j = i;
            Action a = new AbstractAction(option) {
                public void actionPerformed(ActionEvent e) {
                    buttonModel.setValue(j);
                    dispose();
                }
            };
            JButton b = new JButton(a);
            if (mnemonic != null && mnemonic.length() > 0) {
                a.putValue(Action.MNEMONIC_KEY, (int) mnemonic.charAt(0));
            }
            buttons[i] = b;
        }

        if (neverAskAgainText != null) {
            neverAskAgainCheckBox = new JCheckBox(neverAskAgainText);
            if (neverAskAgainMnemonic != null) {
                neverAskAgainCheckBox.setMnemonic(neverAskAgainMnemonic.charAt(0));
            }
        }
    }

    public int display() {
        dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);

        FormLayout layout = new FormLayout("3dlu, pref, 3dlu, pref:grow, 3dlu",
                "3dlu, pref:grow, 3dlu, pref, 3dlu, pref, 3dlu");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        if (icon != null) {
            builder.add(new JLabel(icon), cc.xy(2, 2, "center, top"));
        }
        builder.add(innerPanel, cc.xy(4, 2, "center, center"));

        if (neverAskAgainCheckBox != null) {
            builder.add(neverAskAgainCheckBox, cc.xywh(2, 4, 3, 1));
        }

        FormLayout barLayout;
        if (helpButton == null) {
            barLayout = new FormLayout("pref", "pref");
        } else {
            barLayout = new FormLayout("pref, 3dlu, pref", "pref");
        }
        PanelBuilder barBuilder = new PanelBuilder(barLayout);
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        int i = 0;
        for (JButton button : buttons) {
            bar.addRelatedGap();
            bar.addGridded(button);
            if (initialSelection == i++) {
                dialog.getRootPane().setDefaultButton(button);
            }
        }
        barBuilder.add(bar.getPanel(), cc.xy(1, 1));
        if (helpButton != null) {
            barBuilder.add(helpButton, cc.xy(3, 1));
        }
        builder.add(barBuilder.getPanel(), cc.xywh(2, 6, 3, 1, "center, center"));

        dialog.getContentPane().add(builder.getPanel());
        dialog.getContentPane().setSize(innerPanel.getPreferredSize().width,
                innerPanel.getPreferredSize().height);
        dialog.pack();
        if (parent != null && parent.isVisible()) {
            int x = parent.getX() + (parent.getWidth() - dialog.getWidth()) / 2;
            int y = parent.getY() + (parent.getHeight() - dialog.getHeight()) / 2;
            dialog.setLocation(x, y);
        } else {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (int) (screenSize.getWidth() - dialog.getWidth()) / 2;
            int y = (int) (screenSize.getHeight() - dialog.getHeight()) / 2;
            dialog.setLocation(x, y);
        }

        dialog.setVisible(true);

        return (Integer) buttonModel.getValue();
    }

    public boolean isNeverAskAgain() throws IllegalStateException {
        if (!neverAskAgainMode) {
            throw new IllegalStateException(
                    "You cannot request the NeverAskAgain state " +
                            "for GenericDialogs that did not set " +
                            "neverAskAgainText in the constructor");
        }
        return neverAskAgainCheckBox.isSelected();
    }

    private void validateArgs(String[] options) {

        if (innerPanel == null) {
            throw new IllegalArgumentException("Expected a innerPanel to display");
        }

        if (title == null || title.trim().length() == 0) {
            throw new IllegalArgumentException("Expected a title to display");
        }

        if (options == null) {
            throw new IllegalArgumentException("Missing options");
        } else if (initialSelection < 0 || initialSelection >= options.length) {
            throw new IllegalArgumentException("Invalid initialSelection");
        }
    }

    private void dispose() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }

    /**
     * Automatically create mnemonics for the options.
     * Prevernts the nightmare of i18n mnemonic collisions.
     * First tries to find the first available capital leter,
     * then lower case letters.
     *
     * @param options
     */
    private void resolveMnemonics(String[] options, String neverAskAgainText) {

        mnemonics = new String[options.length];

        // First pass, look for capitals.
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            for (int j = 0; j < option.length(); j++) {
                String c = option.substring(j, j + 1);
                if (!c.equals(" ") && c.toUpperCase().equals(c)) {
                    // See if this is in the mnemonics.
                    if (!inMnemonics(c)) {
                        mnemonics[i] = c;
                        break;
                    }
                }
            }
        }

        // Second pass, look for any case.
        for (int i = 0; i < options.length; i++) {
            if (mnemonics[i] == null) {
                String option = options[i];
                for (int j = 0; j < option.length(); j++) {
                    String c = option.substring(j, j + 1);
                    // See if this is in the mnemonics.
                    if (!c.equals(" ") && !inMnemonics(c)) {
                        mnemonics[i] = c;
                        break;
                    }
                }
            }
        }

        // Try to set mnemonic for neverAsk.
        if (neverAskAgainText != null && neverAskAgainText.trim().length() > 0) {
            for (int j = 0; j < neverAskAgainText.length(); j++) {
                String c = neverAskAgainText.substring(j, j + 1);
                if (!c.equals(" ") && c.toUpperCase().equals(c)) {
                    // See if this is in the mnemonics.
                    if (!inMnemonics(c)) {
                        neverAskAgainMnemonic = c;
                        return;
                    }
                }
            }
            for (int j = 0; j < neverAskAgainText.length(); j++) {
                String c = neverAskAgainText.substring(j, j + 1);
                // See if this is in the mnemonics.
                if (!c.equals(" ") && !inMnemonics(c)) {
                    neverAskAgainMnemonic = c;
                    return;
                }
            }
        }
    }

    private boolean inMnemonics(String c) {
        for (String mnemonic : mnemonics) {
            if (mnemonic != null && mnemonic.equals(c)) {
                return true;
            }
        }
        return false;
    }

}
