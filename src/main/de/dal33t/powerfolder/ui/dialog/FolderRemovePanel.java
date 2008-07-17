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
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.FolderRemoveAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel displayed when wanting to remove a folder
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.00 $
 */
public class FolderRemovePanel extends BaseDialog {

    private final FolderRemoveAction action;
    private final Folder folder;

    private JButton leaveButton;
    private JButton cancelButton;

    private JLabel messageLabel;

    private JCheckBox removeFromLocalBox;
    private JCheckBox convertToPreviewBox;
    private JCheckBox deleteSystemSubFolderBox;
    private JCheckBox removeFromServerBox;

    /**
     * Contructor when used on choosen folder
     * 
     * @param action
     * @param controller
     * @param foInfo
     */
    public FolderRemovePanel(FolderRemoveAction action, Controller controller,
        Folder folder)
    {
        super(controller, true);
        this.action = action;
        this.folder = folder;
    }

    // UI Building ************************************************************

    /**
     * Initalizes all ui components
     */
    private void initComponents() {

        // Create folder leave dialog message
        boolean syncFlag = folder.isTransferring();
        String folerLeaveText;
        if (syncFlag) {
            folerLeaveText = Translation.getTranslation(
                "folder_remove.dialog.text", folder.getInfo().name)
                + '\n'
                + Translation
                    .getTranslation("folder_remove.dialog.sync_warning");
        } else {
            folerLeaveText = Translation.getTranslation(
                "folder_remove.dialog.text", folder.getInfo().name);
        }
        messageLabel = new JLabel(folerLeaveText);

        removeFromLocalBox = SimpleComponentFactory
            .createCheckBox(Translation.getTranslation(
                    "folder_remove.dialog.remove_from_local"));
        removeFromLocalBox.setSelected(true);
        removeFromLocalBox.addActionListener(new ConvertActionListener());

        convertToPreviewBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("folder_remove.dialog.preview"));
        convertToPreviewBox.addActionListener(new ConvertActionListener());

        deleteSystemSubFolderBox = SimpleComponentFactory.createCheckBox(
                Translation.getTranslation("folder_remove.dialog.delete"));

        removeFromServerBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("folder_remove.dialog.remove_from_os"));
        removeFromServerBox.addActionListener(new ConvertActionListener());
        removeFromServerBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getUIController().getServerClientModel()
                    .checkAndSetupAccount();
            }
        });

        // Buttons
        createLeaveButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                leaveButton.setEnabled(false);
                action.confirmedFolderLeave(removeFromLocalBox.isSelected(),
                        deleteSystemSubFolderBox.isSelected(),
                        convertToPreviewBox.isSelected(),
                        removeFromServerBox.isSelected());
                close();
            }
        });

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    private void createLeaveButton(ActionListener listener) {
        leaveButton = new JButton(Translation
            .getTranslation("folder_remove.dialog.button.name"));
        leaveButton.setMnemonic(Translation.getTranslation(
            "folder_remove.dialog.button.key").trim().charAt(0));
        leaveButton.addActionListener(listener);
    }

    // Methods for BaseDialog *************************************************

    public String getTitle() {
        return Translation.getTranslation("folder_remove.dialog.title", folder
            .getInfo().name);
    }

    protected Icon getIcon() {
        return Icons.REMOVE_FOLDER;
    }

    protected Component getContent() {
        initComponents();

        FormLayout layout = new FormLayout("pref:grow, 5dlu, pref:grow",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        builder.add(messageLabel, cc.xyw(1, 1, 3));

        boolean showRemoves = !getController().isLanOnly()
            && getController().getOSClient().getAccount().hasAdminPermission(
                folder.getInfo());

        int row = 3;

        if (showRemoves) {
            builder.add(removeFromLocalBox, cc.xyw(1, row, 3));
            row += 2;
        }

        builder.add(convertToPreviewBox, cc.xyw(1, row, 3));
        row += 2;

        builder.add(deleteSystemSubFolderBox, cc.xyw(1, row, 3));
        row += 2;

        if (showRemoves) {
            builder.add(removeFromServerBox, cc.xyw(1, row, 3));
            row += 2;
        }

        configureComponents();

        return builder.getPanel();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(leaveButton, cancelButton);
    }

    private void configureComponents() {
        convertToPreviewBox.setEnabled(removeFromLocalBox.isSelected());
            if (!removeFromLocalBox.isSelected()) {
                convertToPreviewBox.setSelected(false);
            }

            deleteSystemSubFolderBox.setEnabled(!convertToPreviewBox.isSelected()
                    && removeFromLocalBox.isSelected());
            leaveButton.setEnabled(!convertToPreviewBox.isSelected()
                    && removeFromLocalBox.isSelected());
            if (convertToPreviewBox.isSelected()
                    || !removeFromLocalBox.isSelected()) {
                deleteSystemSubFolderBox.setSelected(false);
            }

            leaveButton.setEnabled(removeFromLocalBox.isSelected()
                    || removeFromServerBox.isSelected());
    }

    private class ConvertActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            configureComponents();
        }
    }
}