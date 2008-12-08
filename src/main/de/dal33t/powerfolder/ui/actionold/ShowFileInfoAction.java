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
package de.dal33t.powerfolder.ui.actionold;

import com.jgoodies.forms.factories.ButtonBarFactory;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.SelectionModel;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Action to show the currently selected file
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class ShowFileInfoAction extends BaseAction {
    private BaseDialog dialog;

    /**
     * Initalizes a new showfileinfo action. displays a dialog with information
     * about the selected file
     * 
     * @param controller
     * @param selectionModel
     */
    public ShowFileInfoAction(Controller controller, SelectionModel selectionModel)
    {
        super("show_file_info", controller);
    }

    public void actionPerformed(ActionEvent e) {
        // Show dialog
        show();
    }

    /**
     * Opens the dialog
     */
    private void show() {
        if (dialog == null) {
            initDialog();
        }
        dialog.open();
    }

    /**
     * Initalizes the dialog around the file info panel
     */
    private void initDialog() {
        dialog = new BaseDialog(getController(), false) {

            public String getTitle() {
                // TODO Auto-generated method stub
                return "TODO";
            }

            protected Icon getIcon() {
                // TODO Auto-generated method stub
                return null;
            }

            protected Component getContent() {
                return new JPanel();
            }

            protected Component getButtonBar() {
                JButton okButton = createOKButton(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dialog.setVisible(false);
                    }
                });
                return ButtonBarFactory.buildCenteredBar(okButton);
            }
        };
    }
}