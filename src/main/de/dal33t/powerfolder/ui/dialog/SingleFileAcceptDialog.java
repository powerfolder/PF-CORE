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
import com.jgoodies.binding.value.ValueHolder;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.message.SingleFileOffer;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.DialogFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Dialog for accepting a file transfer offer from another computer.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class SingleFileAcceptDialog extends BaseDialog {

    private JButton acceptButton;
    private JButton cancelButton;

    private SingleFileOffer offer;
    private JTextField locationTF;
    private ValueModel locationModel;

    /**
     * Constructor.
     */
    public SingleFileAcceptDialog(Controller controller, SingleFileOffer offer) {
        super(controller, true);
        this.offer = offer;
    }

    /**
     * Gets the title of the dialog.
     *
     * @return
     */
    public String getTitle() {
        return Translation.getTranslation("dialog.single_file_accept.title");
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(acceptButton, cancelButton);
    }

    /**
     * Gets the icon for the dialog.
     *
     * @return
     */
    protected Icon getIcon() {
        return Icons.getIconById(Icons.SYNC_FOLDER_48);
    }

    /**
     * Creates the visual component.
     *
     * @return
     */
    protected Component getContent() {
        initComponents();                              
        FormLayout layout;
        if (offer.getMessage() != null && offer.getMessage().length() > 0) {
            layout = new FormLayout(
                "pref:grow",
                "pref, 3dlu, pref, 6dlu, pref, pref, 6dlu, pref, 3dlu, pref");
              // line 1      line 2      'Msg' msg         line 3      dir
        } else {
            layout = new FormLayout(
                "pref:grow, 3dlu, pref:grow",
                "pref, 3dlu, pref, 6dlu, pref, 3dlu, pref");
        }     // line 1      line 2      line 3      dir
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Profile name
        builder.add(new JLabel(Translation.getTranslation(
                "dialog.single_file_accept.line_1")), cc.xy(1, 1));

        builder.add(new JLabel(Translation.getTranslation(
                "dialog.single_file_accept.line_2",
                offer.getOfferingMemberInfo().nick,
                offer.getFile().getName())), cc.xy(1, 3));

        int row = 5;
        if (offer.getMessage() != null && offer.getMessage().length() > 0) {
            builder.add(new JLabel(Translation.getTranslation("general.message")),
                    cc.xy(1, row));
            row++;
            JTextArea messageTextArea = new JTextArea();
            messageTextArea.setEditable(false);
            messageTextArea.setText(offer.getMessage());
            JScrollPane scrollPane = new JScrollPane(messageTextArea);
            scrollPane.setPreferredSize(new Dimension(400, 200));
            builder.add(scrollPane, cc.xy(1, row));
            row += 2;
        }

        builder.add(new JLabel(Translation.getTranslation(
                "dialog.single_file_accept.line_3",
                offer.getOfferingMemberInfo().nick,
                offer.getFile().getName())), cc.xy(1, row));
        row += 2;

        builder.add(createLocationField(), cc.xy(1, row));

        row += 2;

        return builder.getPanel();
    }

    /**
     * Creates a pair of location text field and button.
     *
     * @param folderInfo
     * @return
     */
    private JComponent createLocationField() {
        FormLayout layout = new FormLayout("100dlu, 3dlu, 15dlu", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(locationTF, cc.xy(1, 1));

        JButton locationButton = new JButton(Icons.getIconById(Icons.DIRECTORY));
        locationButton.setToolTipText(Translation
            .getTranslation("dialog.single_file_accept.select_destination"));
        locationButton.addActionListener(new MyActionListener());
        builder.add(locationButton, cc.xy(3, 1));
        return builder.getPanel();
    }

    private void displayChooseDirectory() {
        String initial = (String) locationModel.getValue();
        String file = DialogFactory.chooseDirectory(getController(), initial);
        locationModel.setValue(file);
    }

    /**
     * Initialize the dialog components.
     */
    private void initComponents() {

        locationTF = new JTextField();
        locationTF.setEditable(false);
        locationModel = new ValueHolder();
        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateLocationTF();
            }
        });
        locationModel.setValue(System.getProperty("user.home"));

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        // Buttons
        createAcceptButton();

    }

    private void updateLocationTF() {
        locationTF.setText((String) locationModel.getValue());
    }

    private void createAcceptButton() {
        SingleFileAcceptAction action =
                new SingleFileAcceptAction(getController());
        acceptButton = new JButton(action);
        acceptButton.addActionListener(new MyAcceptListener());
    }

    private class SingleFileAcceptAction extends BaseAction {

        private SingleFileAcceptAction(Controller controller) {
            super("action_single_file_transfer_accept", controller);
        }

        public void actionPerformed(ActionEvent e) {
            close();
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            displayChooseDirectory();
        }
    }

    /**
     * Class to process acceptance of a single file transfer offer.
     */
    private class MyAcceptListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
//            getController().getTransferManager().acceptSingleFile(offer);
//            close();
        }
    }
}