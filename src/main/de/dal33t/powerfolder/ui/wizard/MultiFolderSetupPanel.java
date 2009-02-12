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
import de.dal33t.powerfolder.ui.Icons;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_LOCAL_BASES;
import de.dal33t.powerfolder.util.Translation;
import jwf.WizardPanel;

import javax.swing.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Class to do folder creations for optional specified FolderCreateItems.
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 1.11 $
 */
public class MultiFolderSetupPanel extends PFWizardPanel {

    private List<FolderCreateItem> folderCreateItems;
    private JComboBox localBaseCombo;
    private DefaultComboBoxModel localBaseComboModel;

    /**
     * Constuctor
     *
     * @param controller
     * @param folderName
     *            the recommended folder name.
     */
    public MultiFolderSetupPanel(Controller controller) {
        super(controller);

    }

    /**
     * Can procede if an invitation exists.
     */
    public boolean hasNext() {
        return true;
    }

    public WizardPanel next() {

//        // Set FolderInfo
//        FolderInfo folderInfo = new FolderInfo(folderNameTextField.getText()
//            .trim(), '[' + IdGenerator.makeId() + ']');
//        getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, folderInfo);
//
//        // Set sync profile
//        getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
//            syncProfileSelectorPanel.getSyncProfile());
//
//        // Setup choose disk location panel
//        getWizardContext().setAttribute(PROMPT_TEXT_ATTRIBUTE,
//            Translation.getTranslation("wizard.what_to_do.invite.select_local"));
//
//        // Setup sucess panel of this wizard path
//        TextPanelPanel successPanel = new TextPanelPanel(getController(),
//            Translation.getTranslation("wizard.setup_success"), Translation
//                .getTranslation("wizard.success_join"));
//        getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL, successPanel);
//
//        getWizardContext().setAttribute(SAVE_INVITE_LOCALLY,
//            Boolean.TRUE);
//
        return new FolderCreatePanel(getController());
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("right:pref, 3dlu, 140dlu, pref:grow",
            "pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation.getTranslation("general.directory"),
                cc.xy(1, 1));
        builder.add(localBaseCombo, cc.xy(3, 1));

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {

        folderCreateItems = new ArrayList<FolderCreateItem>();

        localBaseComboModel = new DefaultComboBoxModel();
        localBaseCombo = new JComboBox(localBaseComboModel);

        Object attribute = getWizardContext().getAttribute(FOLDER_LOCAL_BASES);
        if (attribute != null && attribute instanceof List) {
            List list = (List) attribute;
            for (Object o : list) {
                if (o instanceof FolderCreateItem) {
                    FolderCreateItem item = (FolderCreateItem) o;
                    folderCreateItems.add(item);
                    localBaseComboModel.addElement(item.getLocalBase().getAbsolutePath());
                }
            }
        }

        getWizardContext().setAttribute(PFWizard.PICTO_ICON,
            Icons.FILE_SHARING_PICTO);
    }

    protected JComponent getPictoComponent() {
        return new JLabel(getContextPicto());
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.multi_folder_setup.title");
    }
}