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
package de.dal33t.powerfolder.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.widget.FilterTextField;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Holds a box with for entering keywords and 3 checkboxes (showNormal,
 * showDeleted and showExpected). The checkboxes are optional displayed. The
 * checkboxes labels also hold a count of the number of files that matches the
 * criteria in the FileFilterModel.
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.4 $
 */
public class FolderInfoFilterPanel extends PFUIComponent {
    private JPanel panel;

    private JCheckBox showEmptyBox;
    private FolderInfoFilterModel folderInfoFilterModel;

    /**
     * @param showCheckBoxes
     *            set to false to hide them
     */
    public FolderInfoFilterPanel(Controller controller,
        FolderInfoFilterModel folderInfoFilterModel)
    {
        super(controller);
        this.folderInfoFilterModel = folderInfoFilterModel;
    }

    public JComponent getUIComponent() {
        if (panel == null) {
            initComponents();
        }
        return panel;
    }

    /**
     * Initalize all nessesary components
     */
    private void initComponents() {
        // First initalize filterbar components
        showEmptyBox = new JCheckBox(new AbstractAction(Translation
            .getTranslation("public_folders.filter.show_empty_folders"))
        {
            public void actionPerformed(ActionEvent e) {
                folderInfoFilterModel.setShowEmpty(showEmptyBox.isSelected());
            }
        });

        FilterTextField filterTextField = new FilterTextField(12, "", "");
        folderInfoFilterModel.setSearchField(filterTextField.getValueModel());

        // Now build panel and align items
        FormLayout layout = new FormLayout("150dlu, 7dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(filterTextField.getUIComponent(), cc.xy(1, 1));
        builder.add(showEmptyBox, cc.xy(3, 1));
        panel = builder.getPanel();
        panel.setBorder(Borders.createEmptyBorder("3dlu, 3dlu, 3dlu, 3dlu"));
    }
}
