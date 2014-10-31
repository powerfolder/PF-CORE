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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.net.ErrorManager;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/*
 * This class creates a dialog box which is designed to display
 * given DynDns error message
 *
 * @author Albena Roshelova
 *
 */
public class ErrorDialog extends PFUIComponent {

    private JButton okButton;
    private JButton detailsButton;
    private JDialog uiComponent;
    private boolean modal;
    private String errorTxt;
    private JPanel downPanel;
    private TitledBorder title;
    private JTextArea errorsField;
    // private Dimension oldSize, newSize ;
    private int oldWidth, oldHeight;
    private int kindOfError;

    // panel size w = 310,
    private int h = 120;

    public ErrorDialog(Controller controller, boolean modal) {
        super(controller);
        this.modal = modal;
    }

    /**
     * Shows (and builds) the dialog
     */
    public final void open(String errorTxt, int kindOfError) {
        this.errorTxt = errorTxt;
        this.kindOfError = kindOfError;
        logFiner("Open called: " + this);
        if (isOpen()) {
            close();
        }
        getUIComponent().setVisible(true);
    }

    /**
     * Disposes the dialog.
     */
    public final void close() {
        logFiner("Close called: " + this);
        if (uiComponent != null) {
            uiComponent.dispose();
            uiComponent = null;
        }
    }

    /**
     * Disposes the dialog.
     */
    public final boolean isOpen() {
        logFiner("Close called: " + this);
        if (uiComponent != null) {
            return true;
        }
        return false;
    }

    /**
     * create OK button
     */
    protected JButton createOKButton(ActionListener listener) {
        JButton okButton = new JButton(Translation.getTranslation("general.ok"));
        okButton.addActionListener(listener);
        return okButton;
    }

    /**
     * create error details button
     */

    protected JButton createDetailsButton(ActionListener listener) {
        JButton detailsButton = new JButton(Translation
            .getTranslation("exp.preferences.dyn_dns.error_expand_button"));
        detailsButton.addActionListener(listener);
        return detailsButton;
    }

    /**
     * Initalizes needed ui components
     */
    private void initComponents() {

        errorsField = new JTextArea(errorTxt);

        // Enable line-wrapping and word-wrapping
        errorsField.setLineWrap(true);
        errorsField.setWrapStyleWord(true);

        detailsButton = createDetailsButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    public void run() {
                        setDetailsPanelVisible(!downPanel.isVisible());
                        errorsField.setSize(uiComponent.getWidth() - 40,
                            uiComponent.getHeight());
                        uiComponent.validate();
                        uiComponent.repaint();
                    }
                }.start();
            }
        });
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    /**
     * Build the ui error details panel
     */
    private JPanel createErrorPanel() {
        FormLayout layout = new FormLayout("max(0;pref):grow,pref,pref",
            "max(0;pref):grow, pref, pref, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(errorsField, cc.xywh(1, 1, 2, 3));
        JPanel panel = builder.getPanel();
        if (kindOfError == ErrorManager.ERROR) {
            title = BorderFactory.createTitledBorder("Error Details");
        }
        if (kindOfError == ErrorManager.WARN) {
            title = BorderFactory.createTitledBorder("Warning Details");
        }
        panel.setBorder(title);
        return panel;
    }

    private final Component getErrorContent() {
        downPanel = new JPanel();
        downPanel.add(createErrorPanel());
        return downPanel;
    }

    /**
     * Build the ui general panel
     */
    private JPanel createGeneralPanel() {
        FormLayout layout = new FormLayout("max(0;pref):grow, pref",
            "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        if (kindOfError == ErrorManager.WARN) {
            builder.add(new JLabel(Translation
                .getTranslation("exp.preferences.dyn_dns_update_warning")
                + "  "
                + getController().getDynDnsManager().activeDynDns
                    .getErrorShortText() + "  "), cc.xywh(1, 1, 1, 1));

        }
        if (kindOfError == ErrorManager.ERROR) {
            builder.add(new JLabel(Translation
                .getTranslation("exp.preferences.dyn_dns.update_error")
                + "  "
                + getController().getDynDnsManager().activeDynDns
                    .getErrorShortText() + "  "), cc.xywh(1, 1, 1, 1));
        }
        JPanel panel = builder.getPanel();
        return panel;
    }

    protected Component getContent() {
        JPanel topPanel = new JPanel();
        topPanel.add(createGeneralPanel());
        return topPanel;
    }

    /**
     * Get the component title
     *
     * @return the title
     */
    private String getTitle() {
        return Translation
            .getTranslation("preferences.dyn_dns.update_title");
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildRightAlignedBar(okButton, detailsButton);
    }

    private Dimension setPrefferedSize(int w, int h) {
        return new Dimension(w, h);
    }

    private void shrink() {
        uiComponent.setSize(oldWidth, oldHeight);
    }

    private void expand() {
        // oldSize = uiComponent.getSize();
        oldWidth = uiComponent.getWidth();
        oldHeight = uiComponent.getHeight();
        uiComponent.setSize(uiComponent.getWidth(),
            uiComponent.getHeight() + 110);
    }

    private void setDetailsPanelVisible(boolean visible) {
        downPanel.setVisible(visible);
        if (downPanel.isVisible()) {
            expand();
            detailsButton.setText(Translation
                .getTranslation("exp.preferences.dyn_dns.error_shrink_button"));
        } else {
            shrink();
            detailsButton.setText(Translation
                .getTranslation("exp.preferences.dyn_dns.error_expand_button"));
        }
    }

    /**
     * Build the ui component
     *
     * @return
     */
    protected final JDialog getUIComponent() {
        initComponents();

        if (uiComponent == null) {
            logFiner("Building ui component for " + this);
            uiComponent = new JDialog(getUIController().getMainFrame()
                .getUIComponent(), getTitle(), modal);
            uiComponent.setResizable(false);
            uiComponent.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            FormLayout layout = new FormLayout(
                "max(0;pref):grow, 15dlu, pref, 6dlu, pref, pref",// max(0;pref):grow,
                                                                    // pref,
                                                                    // pref,pref,pref",
                "pref, 25dlu, pref, 25dlu, pref, 25dlu, pref, 25dlu, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.DLU7_BORDER);

            final CellConstraints cc = new CellConstraints();

            // Build
            builder.add(getContent(), cc.xywh(1, 1, 4, 1));
            builder.add(getButtonBar(), cc.xywh(1, 2, 4, 1));
            builder.add(getErrorContent(), cc.xywh(1, 4, 4, 5));
            setDetailsPanelVisible(false);

            // Add panel to component
            uiComponent.getContentPane().add(builder.getPanel());

            uiComponent.pack();
            Component parent = uiComponent.getOwner();
            int x = parent.getX()
                + (parent.getWidth() - uiComponent.getWidth()) / 2;
            int y = parent.getY()
                + (parent.getHeight() - uiComponent.getHeight()) / 2;

            uiComponent.setSize(setPrefferedSize(uiComponent.getWidth(), h));
            uiComponent.setLocation(x, y);
        }
        return uiComponent;
    }
}
