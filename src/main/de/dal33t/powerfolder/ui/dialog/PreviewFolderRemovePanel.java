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
import de.dal33t.powerfolder.ui.actionold.PreviewFolderRemoveAction;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel displayed when wanting to remove a preview folder
 * 
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.00 $
 */
public class PreviewFolderRemovePanel extends BaseDialog {

    private final PreviewFolderRemoveAction action;
    private final Folder folder;

    private JButton okButton;
    private JButton cancelButton;
    private JLabel messageLabel;
    private JCheckBox removeFromServerBox;

    /**
     * Contructor when used on choosen folder
     * 
     * @param action
     * @param controller
     * @param foInfo
     */
    public PreviewFolderRemovePanel(PreviewFolderRemoveAction action,
        Controller controller, Folder folder)
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

        String folerLeaveText = Translation.getTranslation(
            "preview_folder_remove.dialog.text", folder.getInfo().name);
        messageLabel = new JLabel(folerLeaveText);

        removeFromServerBox = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("folder_remove.dialog.remove_from_os"));
        removeFromServerBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getUIController().getServerClientModel()
                    .checkAndSetupAccount();
            }
        });

        // Buttons
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton.setEnabled(false);
                action.confirmedFolderLeave(true, removeFromServerBox
                    .isSelected());
                close();
            }
        });

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    // Methods for BaseDialog *************************************************

    public String getTitle() {
        return Translation.getTranslation("preview_folder_remove.dialog.title",
            folder.getInfo().name);
    }

    protected Icon getIcon() {
        return Icons.REMOVE_FOLDER;
    }

    protected Component getContent() {
        initComponents();

        FormLayout layout = new FormLayout("pref:grow", "pref, 7dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        builder.add(messageLabel, cc.xy(1, 1));

        boolean showRemoveFromServer = !getController().isLanOnly()
            && getController().getOSClient().getAccount().hasAdminPermission(
                folder.getInfo());
        if (showRemoveFromServer) {
            builder.add(removeFromServerBox, cc.xy(1, 3));
        }

        return builder.getPanel();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

}