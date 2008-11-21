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
* $Id: FilesTablePanel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.actionold.HasDetailsPanel;
import de.dal33t.powerfolder.ui.action.BaseAction;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import java.awt.event.ActionEvent;

public class FilesTablePanel extends PFUIComponent implements HasDetailsPanel {

    private JPanel uiComponent;
    private FileDetailsPanel fileDetailsPanel;

    public FilesTablePanel(Controller controller) {
        super(controller);
        fileDetailsPanel = new FileDetailsPanel(getController());
    }

    /**
     * Gets the ui component
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Bulds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("fill:pref:grow",
                "pref, 3dlu, fill:0:grow, 3dlu, pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createToolBar(), cc.xy(1, 1));
        builder.add(new JTable(), cc.xy(1, 3));
        builder.add(fileDetailsPanel.getUiComponent(), cc.xy(1, 5));
        uiComponent = builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JToggleButton(new DetailsAction(getController())));
        return bar.getPanel();
    }

    /**
     * Toggle the details panel visibility.
     */
    public void toggleDetails() {
        fileDetailsPanel.getUiComponent().setVisible(
                !fileDetailsPanel.getUiComponent().isVisible());
    }

    private class DetailsAction extends BaseAction {

        DetailsAction(Controller controller) {
            super("action_details", controller);
        }

        public void actionPerformed(ActionEvent e) {
            toggleDetails();
        }
    }


}
