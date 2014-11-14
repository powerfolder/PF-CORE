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
* $Id: NewDirectoryCreator.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.ui.dialog.directory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.dialog.BaseDialog;
import de.dal33t.powerfolder.ui.util.LinkedTextBuilder;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.ActionEvent;

/**
 * NOTE: This class is package-private, not public, because it should only
 * be accessed through DirectoryChooser.
 */
class NewDirectoryCreator extends BaseDialog {

    private String baseDirectory;
    private final JTextField subdirField;
    private final ValueModel valueModel;
    private JButton okButton;

    NewDirectoryCreator(Controller controller, boolean modal,
                               String baseDirectory, ValueModel valueModel) {
        super(Senior.NONE, controller, modal);
        this.baseDirectory = baseDirectory;
        subdirField = new JTextField();
        this.valueModel = valueModel;
    }

    protected Component getButtonBar() {
        okButton = createOKButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                valueModel.setValue(subdirField.getText());
                close();
            }
        });

        JButton cancelButton = createCancelButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    protected JButton getDefaultButton() {
        return okButton;
    }

    protected JComponent getContent() {

        // Panel builder.
        FormLayout layout = new FormLayout("pref, 4dlu, pref:grow", "pref, 4dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        // Set the components.
        CellConstraints cc = new CellConstraints();
        builder.add(LinkedTextBuilder.build(getController(),
                Translation.getTranslation("dialog.new-directory-creator.text",
                baseDirectory)).getPanel(), cc.xyw(1, 1, 3));
        builder.add(new JLabel(Translation.getTranslation("general.directory")), cc.xy(1, 3));
        builder.add(subdirField, cc.xy(3, 3));

        return builder.getPanel();
    }

    protected Icon getIcon() {
        return null;
    }

    public String getTitle() {
        return Translation.getTranslation("dialog.new-directory-creator.title");
    }
}
