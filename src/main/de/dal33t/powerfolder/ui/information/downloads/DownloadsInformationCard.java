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
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.HasDetailsPanel;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.ui.information.folder.files.FileDetailsPanel;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
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
    private Action abortDownloadsAction;
    private Action openDownloadAction;
    private FileDetailsPanel detailsPanel;
    private JCheckBox autoCleanupCB;
    private Action clearCompletedDownloadsAction;
    private JSlider cleanupSlider;
    private JLabel cleanupLabel;

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
        return Icons.getImageById(Icons.FOLDER);
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
        cleanupLabel = new JLabel();
        buildToolbar();
        tablePanel = new DownloadsTablePanel(getController(), 
                openDownloadAction, abortDownloadsAction,
                clearCompletedDownloadsAction);
        tablePanel.addTableModelListener(new MyTableModelListener());
        tablePanel.addListSelectionListener(new MyListSelectionListener());
        detailsPanel = new FileDetailsPanel(getController());
        update();
    }

    /**
     * Build the toolbar component.
     */
    private void buildToolbar() {

        abortDownloadsAction = new AbortDownloadAction();
        openDownloadAction = new OpenFileAction();

        clearCompletedDownloadsAction = new ClearCompletedDownloadsAction(getController());

        autoCleanupCB = new JCheckBox(Translation
            .getTranslation("downloads_information_card.auto_cleanup.name"));
        autoCleanupCB.setToolTipText(Translation
            .getTranslation("downloads_information_card.auto_cleanup.description"));
        autoCleanupCB.setSelected(ConfigurationEntry.DOWNLOADS_AUTO_CLEANUP
            .getValueBoolean(getController()));
        autoCleanupCB.addActionListener(new MyActionListener());

        cleanupSlider = new JSlider(0, 10,
                PreferencesEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY
                        .getValueInt(getController())) {
            public Dimension getPreferredSize() {
                return new Dimension(20, (int) super.getPreferredSize()
                        .getSize().getHeight());
            }
        };
        cleanupSlider.setMinorTickSpacing(1);
        cleanupSlider.setMajorTickSpacing(5);
        cleanupSlider.setPaintTicks(true);
        cleanupSlider.setSnapToTicks(true);
        cleanupSlider.addChangeListener(new MyChangeListener());

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JToggleButton(new DetailsAction(getController())));
        bar.addRelatedGap();
        bar.addGridded(new JButton(openDownloadAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(abortDownloadsAction));
        bar.addRelatedGap();
        bar.addGridded(new JButton(clearCompletedDownloadsAction));
        bar.addRelatedGap();
        bar.addGridded(autoCleanupCB);
        bar.addRelatedGap();
        bar.addGridded(cleanupSlider);
        toolBar = bar.getPanel();
        updateCleanupLabel();
    }

    /**
     * Build the ui component pane.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, pref, 3dlu, pref:grow, 3dlu",
                "3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow, 3dlu, pref");
                //     tools       sep         table                 details
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(toolBar, cc.xy(2, 2));
        builder.add(cleanupLabel, cc.xy(4, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 5));
        builder.add(tablePanel.getUIComponent(), cc.xyw(2, 6, 3));
        builder.add(detailsPanel.getPanel(), cc.xyw(2, 8, 3));
        uiComponent = builder.getPanel();
        enableCleanupComponents();
    }

    /**
     * Toggle the details panel visibility.
     */
    public void toggleDetails() {
        detailsPanel.getPanel().setVisible(!detailsPanel.getPanel().isVisible());
    }

    /**
     * Update actions and details.
     */
    public void update() {

        boolean singleCompleteSelected = tablePanel.isSingleCompleteSelected();
        boolean rowsExist = tablePanel.isRowsExist();
        boolean incompleteSelected = tablePanel.isIncompleteSelected();

        openDownloadAction.setEnabled(singleCompleteSelected);
        abortDownloadsAction.setEnabled(incompleteSelected);
        clearCompletedDownloadsAction.setEnabled(rowsExist);

        detailsPanel.setFileInfo(tablePanel.getSelectdFile());
    }

    private void updateCleanupLabel() {
        PreferencesEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY
                .setValue(getController(), cleanupSlider.getValue());
        if (cleanupSlider.getValue() == 0) {
            cleanupLabel.setText(Translation.getTranslation(
                    "downloads_information_card.auto_cleanup.immediate"));
        } else {
            cleanupLabel.setText(Translation.getTranslation(
                    "downloads_information_card.auto_cleanup.days",
                    cleanupSlider.getValue()));
        }
    }

    private void enableCleanupComponents() {
        cleanupSlider.setEnabled(autoCleanupCB.isSelected());
        cleanupLabel.setEnabled(autoCleanupCB.isSelected());
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    private class OpenFileAction extends BaseAction {
        OpenFileAction() {
            super("action_open_file",
                    DownloadsInformationCard.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            tablePanel.openSelectedDownload();
        }
    }

    /**
     * Aborts the selected downloads
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     * @version $Revision: 1.3 $
     */
    private class AbortDownloadAction extends BaseAction {

        AbortDownloadAction() {
            super("action_abort_download", 
                    DownloadsInformationCard.this.getController());
        }

        public void actionPerformed(ActionEvent e) {
            tablePanel.abortSelectedDownloads();
        }
    }

    /**
     * Action to toggle the details panel.
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
    private class ClearCompletedDownloadsAction extends BaseAction {
        ClearCompletedDownloadsAction(Controller controller) {
            super("action_clear_completed_downloads", controller);
        }

        public void actionPerformed(ActionEvent e) {
            tablePanel.clearDownloads();
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

    private class MyChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            updateCleanupLabel();
        }
    }

    private class MyActionListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                getUIController().getTransferManagerModel()
                        .getDownloadsAutoCleanupModel().setValue(
                    autoCleanupCB.isSelected());
                ConfigurationEntry.DOWNLOADS_AUTO_CLEANUP
                    .setValue(getController(), String.valueOf(autoCleanupCB
                        .isSelected()));
                getController().saveConfig();
                enableCleanupComponents();
            }
    }
}
