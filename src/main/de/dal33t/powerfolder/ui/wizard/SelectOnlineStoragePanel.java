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
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Translation;
import jwf.WizardPanel;

import javax.swing.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Wizard for selecting folders that are Online Storage and not locally managed.
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.12 $
 */
public class SelectOnlineStoragePanel extends PFWizardPanel {

    private static final Logger log = Logger.getLogger(SelectOnlineStoragePanel.class.getName());

    private List<FolderInfo> possibleFolders;

    public SelectOnlineStoragePanel(Controller controller,
                                 List<FolderInfo> possibleFolders) {
        super(controller);
        this.possibleFolders = possibleFolders;
    }

    public boolean hasNext() {
        return true;
    }

    public boolean validateNext(List<String> errors) {
        return true;
    }

    public WizardPanel next() {
        // Show success panel
        return (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
                "pref, max(pref;140dlu)",
                "pref, 3dlu, pref, 6dlu, pref, 3dlu, pref, 6dlu, pref, 3dlu, " +
                        "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {
    }

    protected JComponent getPictoComponent() {
        return new JLabel(getContextPicto());
    }

    protected String getTitle() {
        return Translation
                .getTranslation("wizard.select_online_storage.title");
    }
}