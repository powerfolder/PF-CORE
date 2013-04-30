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
 * $Id: SingleFileTransferDialog.java 18009 2012-02-04 11:46:51Z harry $
 */
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Translation;

/**
 * Dialog for offering a file to transfer to another computer..
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class SingleFileTransferDialog extends BaseDialog {

    private JButton transferButton;
    private JButton cancelButton;

    private Path file;

    private JTextField computersText;
    private JButton computersSelectButton;
    private ValueModel computersTextModel;
    private final Collection<Member> computersMembers = new ArrayList<Member>();
    private JTextArea messageTextArea;

    /**
     * Constructor.
     */
    public SingleFileTransferDialog(Controller controller, Path file,
        Member node)
    {
        super(Senior.NONE, controller, true);
        this.file = file;
        if (node != null) {
            computersMembers.add(node);
        }
    }

    /**
     * Gets the title of the dialog.
     * 
     * @return
     */
    public String getTitle() {
        return Translation.getTranslation("dialog.single_file_transfer.title");
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(transferButton, cancelButton);
    }

    /**
     * Gets the icon for the dialog.
     * 
     * @return
     */
    protected Icon getIcon() {
        return null;
    }

    protected JButton getDefaultButton() {
        return transferButton;
    }

    /**
     * Creates the visual component.
     * 
     * @return
     */
    protected JComponent getContent() {
        initComponents();
        FormLayout layout = new FormLayout("right:pref, 3dlu, pref",
            "pref, 3dlu, pref, 3dlu, pref, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Profile name
        builder.add(new JLabel(Translation
            .getTranslation("dialog.single_file_transfer.file_name")), cc.xy(1,
            1));
        JTextField fileNameTextField = new JTextField(file.toAbsolutePath().toString());
        fileNameTextField.setEnabled(false);
        FormLayout layout2 = new FormLayout("140dlu", "pref");
        PanelBuilder builder2 = new PanelBuilder(layout2);
        builder2.add(fileNameTextField, cc.xy(1, 1));
        JPanel panel2 = builder2.getPanel();
        builder.add(panel2, cc.xy(3, 1));

        builder.add(new JLabel(Translation
            .getTranslation("dialog.single_file_transfer.computer")), cc.xy(1,
            3));
        if (computersMembers.isEmpty()) {
            FormLayout layout4 = new FormLayout("122dlu, 3dlu, pref", "pref");
            PanelBuilder builder4 = new PanelBuilder(layout4);
            builder4.add(computersText, cc.xy(1, 1));
            builder4.add(computersSelectButton, cc.xy(3, 1));
            JPanel panel4 = builder4.getPanel();
            builder.add(panel4, cc.xy(3, 3));
        } else {
            Member node = computersMembers.iterator().next();
            JTextField nodeNameTextField = new JTextField(node.getInfo().nick);
            nodeNameTextField.setEnabled(false);
            FormLayout layout3 = new FormLayout("140dlu", "pref");
            PanelBuilder builder3 = new PanelBuilder(layout3);
            builder3.add(nodeNameTextField, cc.xy(1, 1));
            JPanel panel3 = builder3.getPanel();
            builder.add(panel3, cc.xy(3, 3));
        }

        builder
            .add(
                new JLabel(
                    Translation
                        .getTranslation("dialog.single_file_transfer.friend_message.text")),
                cc.xyw(1, 5, 3));

        JScrollPane scrollPane = new JScrollPane(messageTextArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        builder.add(scrollPane, cc.xyw(1, 6, 3));

        return builder.getPanel();
    }

    /**
     * Initialize the dialog components.
     */
    private void initComponents() {

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        // Buttons
        createTransferButton();

        computersTextModel = new ValueHolder();
        computersTextModel.setValue(Translation
            .getTranslation("dialog.node_select.no_computers"));
        computersTextModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateTransferButton();
            }
        });
        computersText = BasicComponentFactory.createTextField(
            computersTextModel, false);
        computersText.setEnabled(false);
        computersSelectButton = new JButtonMini(Icons
            .getIconById(Icons.NODE_CONNECTED), Translation
            .getTranslation("dialog.single_file_transfer.select_computer.tip"));
        computersSelectButton.addActionListener(new MyActionListener());

        updateTransferButton();

        messageTextArea = new JTextArea();
    }

    private void updateTransferButton() {
        transferButton.setEnabled(!computersMembers.isEmpty());
    }

    private void createTransferButton() {
        SingleFileTransferAction action = new SingleFileTransferAction(
            getController());
        transferButton = new JButton(action);
    }

    private class SingleFileTransferAction extends BaseAction {

        private SingleFileTransferAction(Controller controller) {
            super("action_single_file_transfer", controller);
        }

        public void actionPerformed(ActionEvent e) {
            // getController().getTransferManager().offerSingleFile(file,
            // computersMembers, messageTextArea.getText());
            close();
        }
    }

    /**
     * Listen for activation of the via powerfolder button.
     */
    private class MyActionListener implements ActionListener {

        /**
         * Open a UserSelectDialog
         * 
         * @param e
         */
        public void actionPerformed(ActionEvent e) {
            NodesSelectDialog dialog = new NodesSelectDialog(getController(),
                computersTextModel, computersMembers);
            dialog.open();
        }
    }
}