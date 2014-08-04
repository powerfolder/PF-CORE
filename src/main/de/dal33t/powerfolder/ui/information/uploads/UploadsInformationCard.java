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

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.TimerTask;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.information.HasDetailsPanel;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.ui.information.InformationCardType;
import de.dal33t.powerfolder.ui.information.folder.files.FileDetailsPanel;
import de.dal33t.powerfolder.ui.information.folder.files.versions.FileVersionsPanel;
import de.dal33t.powerfolder.ui.util.DelayedUpdater;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Information card for a folder. Includes files, members and settings tabs.
 */
public class UploadsInformationCard extends InformationCard implements
    HasDetailsPanel
{

    private JPanel uiComponent;
    private JPanel toolBar;
    private UploadsTablePanel tablePanel;
    private JPanel detailsPanel;
    private FileDetailsPanel fileDetailsPanel;
    private FileVersionsPanel fileVersionsPanel;
    private Action clearCompletedUploadsAction;
    private JSlider cleanupSlider;
    private JLabel cleanupLabel;
    private JPanel statsPanel;
    private JLabel uploadCounterLabel;
    private JLabel activeUploadCountLabel;
    private JLabel completedUploadCountLabel;
    private DelayedUpdater updater;
    private Action addIgnoreAction;

    /**
     * Constructor
     *
     * @param controller
     */
    public UploadsInformationCard(Controller controller) {
        super(controller);
        updater = new DelayedUpdater(controller);
    }

    public InformationCardType getInformationCardType() {
        return InformationCardType.TRANSFERS;
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
        return Translation.getTranslation("exp.uploads_information_card.title");
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
        cleanupLabel
            .setToolTipText(Translation
                .getTranslation("exp.uploads_information_card.auto_cleanup.frequency_tip"));
        buildToolbar();
        tablePanel = new UploadsTablePanel(getController(),
            clearCompletedUploadsAction, addIgnoreAction);
        fileDetailsPanel = new FileDetailsPanel(getController(), true);
        fileVersionsPanel = new FileVersionsPanel(getController());
        detailsPanel = createDetailsPanel();
        detailsPanel.setVisible(false);
        tablePanel.addTableModelListener(new MyTableModelListener());
        tablePanel.addListSelectionListener(new MyListSelectionListener());
        buildStatsPanel();
        update0();
    }

    private void buildStatsPanel() {
        FormLayout layout = new FormLayout(
            "3dlu, pref:grow, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref",
            "pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        activeUploadCountLabel = new JLabel();
        builder.add(activeUploadCountLabel, cc.xy(3, 1));
        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(2, 12));
        builder.add(sep1, cc.xy(5, 1));
        completedUploadCountLabel = new JLabel();
        builder.add(completedUploadCountLabel, cc.xy(7, 1));
        JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
        sep2.setPreferredSize(new Dimension(2, 12));
        builder.add(sep2, cc.xy(9, 1));
        uploadCounterLabel = new JLabel();
        builder.add(uploadCounterLabel, cc.xy(11, 1));

        statsPanel = builder.getPanel();
    }

    private JPanel createDetailsPanel() {
        FormLayout layout = new FormLayout("fill:pref:grow", "pref, 3dlu, pref");
        // spacer, tabs
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Spacer
        builder.addSeparator(null, cc.xy(1, 1));

        JTabbedPane tabbedPane = new JTabbedPane();
        builder.add(tabbedPane, cc.xy(1, 3));

        tabbedPane.add(fileDetailsPanel.getPanel(), Translation
            .getTranslation("files_table_panel.file_details_tab.text"));
        tabbedPane.setToolTipTextAt(0, Translation
            .getTranslation("files_table_panel.file_details_tab.tip"));

        tabbedPane.add(fileVersionsPanel.getPanel(), Translation
            .getTranslation("files_table_panel.file_versions_tab.text"));
        tabbedPane.setToolTipTextAt(1, Translation
            .getTranslation("files_table_panel.file_versions_tab.tip"));

        return builder.getPanel();
    }

    /**
     * Build the toolbar component.
     */
    private void buildToolbar() {

        clearCompletedUploadsAction = new ClearCompletedUploadsAction(
            getController());
        addIgnoreAction = new AddIgnoreAction(getController());

        // NOTE true cleanup days dereferenced through Constants.CLEANUP_VALUES
        Integer x = ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY
            .getValueInt(getController());
        if (x > 4) {
            x = 4;
        }

        cleanupSlider = new JSlider(0, 4, x) {
            public Dimension getPreferredSize() {
                return new Dimension(20, (int) super.getPreferredSize()
                    .getSize().getHeight());
            }
        };
        cleanupSlider.setMinorTickSpacing(1);
        cleanupSlider.setPaintTicks(true);
        cleanupSlider.setSnapToTicks(true);
        cleanupSlider.addChangeListener(new MyChangeListener());
        cleanupSlider
            .setToolTipText(Translation
                .getTranslation("exp.uploads_information_card.auto_cleanup.frequency_tip"));

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        JToggleButton detailsBtn = new JToggleButton(new DetailsAction(
            getController()));
        detailsBtn.setIcon(null);
        bar.addGridded(detailsBtn);
        bar.addRelatedGap();
        bar.addGridded(createButton(clearCompletedUploadsAction));
        bar.addRelatedGap();
        bar.addGridded(cleanupSlider);
        toolBar = bar.getPanel();
        updateCleanupLabel();
    }

    private static JButton createButton(Action action) {
        JButton b = new JButton(action);
        b.setIcon(null);
        return b;
    }

    /**
     * Build the ui component pane.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, pref, 3dlu, pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow, 3dlu, pref, pref, pref");
        // tools sep table dets sep stats
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(toolBar, cc.xy(2, 2));
        builder.add(cleanupLabel, cc.xy(4, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 5));
        builder.add(tablePanel.getUIComponent(), cc.xyw(2, 6, 3));
        builder.add(detailsPanel, cc.xyw(2, 8, 3));
        builder.addSeparator(null, cc.xyw(1, 9, 5));
        builder.add(statsPanel, cc.xyw(2, 10, 3));
        uiComponent = builder.getPanel();
        initStatsTimer();
    }

    private void initStatsTimer() {
        getController().scheduleAndRepeat(new MyStatsTask(), 100, 1000);
    }

    /**
     * Toggle the details panel.
     */
    public void toggleDetails() {
        detailsPanel.setVisible(!detailsPanel.isVisible());
    }

    public void update() {
        updater.schedule(new Runnable() {
            public void run() {
                update0();
            }
        });
    }

    /**
     * Update actions and details.
     */
    private void update0() {
        clearCompletedUploadsAction
            .setEnabled(getUIController().getTransferManagerModel()
                .getUploadsTableModel().getRowCount() > 0);
        addIgnoreAction.setEnabled(tablePanel.getSelectedRows().length > 0);

        fileDetailsPanel.setFileInfo(tablePanel.getSelectdFile());
        fileVersionsPanel.setFileInfo(tablePanel.getSelectdFile());
    }

    private void updateCleanupLabel() {
        ConfigurationEntry.UPLOAD_AUTO_CLEANUP_FREQUENCY.setValue(
            getController(), String.valueOf(cleanupSlider.getValue()));
        getController().saveConfig();
        if (cleanupSlider.getValue() == 0) {
            cleanupLabel
                .setText(Translation
                    .getTranslation("exp.uploads_information_card.auto_cleanup.immediate"));
        } else if (cleanupSlider.getValue() >= 4) {
            cleanupLabel.setText(Translation
                .getTranslation("exp.uploads_information_card.auto_cleanup.never"));
        } else {
            int trueCleanupDays = Constants.CLEANUP_VALUES[cleanupSlider
                .getValue()];
            cleanupLabel.setText(Translation.getTranslation(
                "exp.uploads_information_card.auto_cleanup.days",
                String.valueOf(trueCleanupDays)));
        }
    }

    private void displayStats() {

        int activeUploadCount = tablePanel.countActiveUploadCount();
        activeUploadCountLabel.setText(Translation.getTranslation(
            "status.active_upload_count", String.valueOf(activeUploadCount)));

        int completedUploadCount = tablePanel.countCompletedUploadCount();
        completedUploadCountLabel.setText(Translation.getTranslation(
            "status.completed_upload_count",
            String.valueOf(completedUploadCount)));

        double kbs = getController().getTransferManager().getUploadCounter()
            .calculateCurrentKBS();
        uploadCounterLabel.setText(Translation.getTranslation("status.upload",
            Format.formatDecimal(kbs)));
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

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
            super("exp.action_clear_completed_uploads", controller);
        }

        public void actionPerformed(ActionEvent e) {
            tablePanel.clearUploads();
        }
    }

    /**
     * Listener to the underlying table model. Detects changes to row details
     * and updates actions.
     */
    private class MyTableModelListener implements TableModelListener {
        public void tableChanged(TableModelEvent e) {
            update();
        }
    }

    /**
     * Listener to the underlying table. Detects changes to row selections and
     * updates actions.
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

    private class MyStatsTask extends TimerTask {

        public void run() {
            displayStats();
        }
    }

    private class AddIgnoreAction extends BaseAction {

        private AddIgnoreAction(Controller controller) {
            super("action_add_ignore", controller);
        }

        public void actionPerformed(ActionEvent e) {
            for (Upload upload : tablePanel.getSelectedRows()) {
                if (upload != null) {
                    FileInfo fileInfo = upload.getFile();
                    Folder folder = getController().getFolderRepository()
                        .getFolder(fileInfo.getFolderInfo());
                    folder.addPattern(fileInfo.getRelativeName());
                    getController().getTransferManager()
                        .checkActiveTranfersForExcludes();
                }
            }
        }
    }
}