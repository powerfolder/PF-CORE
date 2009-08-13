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
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Display a dialog accepting a personalized message
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.00 $
 */
public class AttachPersonalizedMessageDialog extends BaseDialog {

    private JButton okButton;
    private JButton cancelButton;
    private ValueModel messageModel; // <String>
    private JTextArea messageArea;

    /**
     * Contructor
     *
     * @param controller
     * @param messageModel
     *                 ValueModel<String> that holds the message.
     */
    public AttachPersonalizedMessageDialog(Controller controller,
                                        ValueModel messageModel) {
        super(controller, true);
        this.messageModel = messageModel;
    }

    /**
     * Initalizes all ui components
     */
    private void initComponents() {

        // Buttons
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                messageModel.setValue(messageArea.getText());
                close();
            }
        });

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        messageArea = new JTextArea();

    }

    public String getTitle() {
        return Translation.getTranslation("dialog.personalized_message.title");
    }

    protected Icon getIcon() {
        return null;
    }

    protected JComponent getContent() {
        initComponents();

        FormLayout layout = new FormLayout("pref:grow",
            "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        int row = 1;

        builder.addLabel(Translation.getTranslation(
                "dialog.personalized_message.hint"), cc.xy(1, row));

        row += 2;
        
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setPreferredSize(new Dimension(200, 100));
        builder.add(scrollPane, cc.xy(1, row));

        return builder.getPanel();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    protected JButton getDefaultButton() {
        return okButton;
    }

}