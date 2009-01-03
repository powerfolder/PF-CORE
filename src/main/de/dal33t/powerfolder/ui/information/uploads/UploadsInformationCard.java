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
* $Id: UploadsInformationCard.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.uploads;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.HasDetailsPanel;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.ui.information.folder.files.FileDetailsPanel;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Information card for a folder. Includes files, members and settings tabs.
 */
public class UploadsInformationCard extends InformationCard
        implements HasDetailsPanel {

    private JPanel uiComponent;
    private JPanel toolBar;
    private UploadsTablePanel tablePanel;
    private FileDetailsPanel detailsPanel;
    private JCheckBox autoCleanupCB;
    private Action clearCompletedUploadsAction;

    /**
     * Constructor
     *
     * @param controller
     */
    public UploadsInformationCard(Controller controller) {
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
        return Translation.getTranslation("uploads_information_card.title");
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
        tablePanel = new UploadsTablePanel(getController(), clearCompletedUploadsAction);
        detailsPanel = new FileDetailsPanel(getController());
        tablePanel.addTableModelListener(new MyTableModelListener());
        tablePanel.addListSelectionListener(new MyListSelectionListener());
        update();
    }

    /**
     * Build the toolbar component.
     */
    private void buildToolbar() {

        clearCompletedUploadsAction = new ClearCompletedUploadsAction(getController());

        autoCleanupCB = new JCheckBox(Translation
            .getTranslation("uploads_information_card.auto_cleanup.name"));
        autoCleanupCB.setToolTipText(Translation
            .getTranslation("uploads_information_card.auto_cleanup.description"));
        autoCleanupCB.setSelected(ConfigurationEntry.UPLOADS_AUTO_CLEANUP
            .getValueBoolean(getController()));
        autoCleanupCB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getUIController().getTransferManagerModel()
                        .getUploadsAutoCleanupModel().setValue(
                    autoCleanupCB.isSelected());
                ConfigurationEntry.UPLOADS_AUTO_CLEANUP
                    .setValue(getController(), String.valueOf(autoCleanupCB
                        .isSelected()));
                getController().saveConfig();
            }
        });

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JToggleButton(new DetailsAction(getController())));
        bar.addRelatedGap();
        bar.addGridded(new JButton(clearCompletedUploadsAction));
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
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(tablePanel.getUIComponent(), cc.xy(2, 6));
        builder.add(detailsPanel.getPanel(), cc.xy(2, 8));
        uiComponent = builder.getPanel();
    }

    /**
     * Toggle the details panel.
     */
    public void toggleDetails() {
        detailsPanel.getPanel().setVisible(
                !detailsPanel.getPanel().isVisible());
    }

    /**
     * Update the actions and details.
     */
    public void update() {
        clearCompletedUploadsAction.setEnabled(getUIController()
                .getTransferManagerModel().getUploadsTableModel()
                .getRowCount() > 0);

        detailsPanel.setFileInfo(tablePanel.getSelectdFile());
    }

    /**
     * Action to display the details panel.
     */
    private class DetailsAction extends BaseAction {

        DetailsAction(Controller controller) {
            super("action_details", controller);
        }

        public void actionPerformed(ActionEvent e) {
            toggleDetails();
        }
    }

    /**
     * Clears completed uploads. See MainFrame.MyCleanupAction for accelerator
     * functionality
     */
    private class ClearCompletedUploadsAction extends BaseAction {
        ClearCompletedUploadsAction(Controller controller) {
            super("action_clear_completed_uploads", controller);
        }

        public void actionPerformed(ActionEvent e) {
            tablePanel.clearUploads();
        }
    }


    /**
     * Listener to the underlying table model.
     * Detects changes to row details and updates actions.
     */
    private class MyTableModelListener implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            update();
        }
    }

    /**
     * Listener to the underlying table.
     * Detects changes to row selections and updates actions.
     */
    private class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            update();
        }
    }

}