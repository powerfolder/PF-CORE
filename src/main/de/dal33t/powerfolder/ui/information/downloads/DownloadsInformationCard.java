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
* $Id: DownloadsInformationCard.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.downloads;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.actionold.HasDetailsPanel;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Information card for a folder. Includes files, members and settings tabs.
 */
public class DownloadsInformationCard extends InformationCard
        implements HasDetailsPanel {

    private JPanel uiComponent;
    private JPanel toolBar;
    private DownloadsTablePanel tablePanel;
    private DownloadsDetailsPanel detailsPanel;
    private JCheckBox autoCleanupCB;

    /**
     * Constructor
     *
     * @param controller
     */
    public DownloadsInformationCard(Controller controller) {
        super(controller);
    }

    /**
     * Gets the image for the card.
     *
     * @return
     */
    public Image getCardImage() {
        return Icons.FOLDER_IMAGE;
    }

    /**
     * Gets the title for the card.
     *
     * @return
     */
    public String getCardTitle() {
        return Translation.getTranslation("downloads_information_card.title");
    }

    /**
     * Gets the ui component after initializing and building if necessary
     *
     * @return
     */
    public JComponent getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Initialize components
     */
    private void initialize() {
        buildToolbar();
        tablePanel = new DownloadsTablePanel(getController());
        detailsPanel = new DownloadsDetailsPanel(getController());
    }

    private void buildToolbar() {

        autoCleanupCB = new JCheckBox(Translation
            .getTranslation("downloads_information_card.auto_cleanup.name"));
        autoCleanupCB.setToolTipText(Translation
            .getTranslation("downloads_information_card.auto_cleanup.description"));
        autoCleanupCB.setSelected(ConfigurationEntry.DOWNLOADS_AUTO_CLEANUP
            .getValueBoolean(getController()));
        autoCleanupCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getUIController().getTransferManagerModel()
                        .getDownloadsAutoCleanupModel().setValue(
                    autoCleanupCB.isSelected());
                ConfigurationEntry.DOWNLOADS_AUTO_CLEANUP
                    .setValue(getController(), String.valueOf(autoCleanupCB
                        .isSelected()));
                getController().saveConfig();
            }
        });
        
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JToggleButton(new DetailsAction(getController())));
        bar.addRelatedGap();
        bar.addGridded(autoCleanupCB);
        
        toolBar = bar.getPanel();
    }

    /**
     * Build the ui component pane.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
                "3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow, 3dlu, pref");
                //     tools       sep         table                 details
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(toolBar, cc.xy(2, 2));
        builder.addSeparator(null, cc.xy(2, 4));
        builder.add(tablePanel.getUIComponent(), cc.xy(2, 6));
        builder.add(detailsPanel.getUiComponent(), cc.xy(2, 8));
        uiComponent = builder.getPanel();
    }

    public void toggleDetails() {
        detailsPanel.getUiComponent().setVisible(
                !detailsPanel.getUiComponent().isVisible());
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