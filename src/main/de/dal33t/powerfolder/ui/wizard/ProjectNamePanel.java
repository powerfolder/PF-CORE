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

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.INITIAL_FOLDER_NAME;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;
import jwf.WizardPanel;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Panel where user may choose the name of the project
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class ProjectNamePanel extends PFWizardPanel {

    private JTextField nameField;
    private ValueModel nameModel;

    public ProjectNamePanel(Controller controller) {
        super(controller);
    }

    protected JPanel buildContent() {

        FormLayout layout = new FormLayout("$wlabel, $lcg, $wfield", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addLabel(Translation
            .getTranslation("wizard.projectname.entername"), cc.xy(1, 1));
        builder.add(nameField, cc.xy(3, 1));

        return builder.getPanel();
    }

    // From WizardPanel *******************************************************

    public boolean hasNext() {
        return !StringUtils.isBlank((String) nameModel.getValue());
    }

    public WizardPanel next() {
        // Create new folder info
        String name = "Project-" + nameField.getText();
        String folderId = '[' + IdGenerator.makeId() + ']';
        FolderInfo folder = new FolderInfo(name, folderId);
        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, folder);
        getWizardContext().setAttribute(INITIAL_FOLDER_NAME, folder.name);

        FolderSetupPanel setupPanel = new FolderSetupPanel(getController());

        // Choose location...
        return new ChooseDiskLocationPanel(getController(), null, setupPanel);
    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {

        nameModel = new ValueHolder();

        nameModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });

        nameField = BasicComponentFactory.createTextField(nameModel, false);

        // Ensure minimum dimension
        UIUtil.ensureMinimumWidth(107, nameField);
    }

    protected Icon getPicto() {
        return getContextPicto();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.projectname.choose");
    }

}