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
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.CopyOrMoveFileArchiver;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialog for creatigng or editing profile configuration. User can select a
 * default profile and then adjust the configuration.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class CopyOrMoveFileArchiverEditDialog extends BaseDialog {

    private final CopyOrMoveFileArchiver archiver;
    private JButton okButton;

    /**
     * Constructor.
     *
     * @param controller
     * @param syncProfileSelectorPanel
     */
    public CopyOrMoveFileArchiverEditDialog(Controller controller,
        CopyOrMoveFileArchiver archiver) {
        super(controller, true);
        this.archiver = archiver;
    }

    /**
     * Gets the title of the dialog.
     *
     * @return
     */
    public String getTitle() {
        return Translation.getTranslation(
                "dialog.copy_or_move_file_archiver_edit.title");
    }

    protected Icon getIcon() {
        return null;
    }

    /**
     * Creates the visual component.
     *
     * @return
     */
    protected JComponent getContent() {
        initComponents();
        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, pref",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        return builder.getPanel();
    }

    /**
     * Initialize the dialog components.
     */
    private void initComponents() {
    }

    /**
     * The OK / Cancel buttons.
     *
     * @return
     */
    protected Component getButtonBar() {

        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okPressed();
            }
        });

        JButton cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelPressed();
            }
        });

        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    protected JButton getDefaultButton() {
        return okButton;
    }

    private void okPressed() {
        close();
    }

    private void cancelPressed() {
        close();
    }

}