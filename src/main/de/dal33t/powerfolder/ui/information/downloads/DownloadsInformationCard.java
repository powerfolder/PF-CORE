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
import de.dal33t.powerfolder.transfer.DownloadManager;
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
public class DownloadsInformationCard extends InformationCard implements
    HasDetailsPanel
{

    private JPanel uiComponent;
    private JPanel toolBar;
    private DownloadsTablePanel tablePanel;
    private Action abortDownloadsAction;
    private Action openDownloadAction;
    private JPanel detailsPanel;
    private FileDetailsPanel fileDetailsPanel;
    private FileVersionsPanel fileVersionsPanel;
    private Action clearCompletedDownloadsAction;
    private JSlider cleanupSlider;
    private JLabel cleanupLabel;
    private JPanel statsPanel;
    private JLabel downloadCounterLabel;
    private JLabel activeDownloadCountLabel;
    private JLabel completedDownloadCountLabel;
    private DelayedUpdater updater;
    private Action addIgnoreAction;

    /**
     * Constructor
     *
     * @param controller
     */
    public DownloadsInformationCard(Controller controller) {
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
        return Translation.get("exp.downloads_information_card.title");
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
        cleanupLabel.setToolTipText(Translation.get(
                "exp.downloads_information_card.auto_cleanup.frequency_tip"));
        buildToolbar();
        tablePanel = new DownloadsTablePanel(getController(),
            openDownloadAction, abortDownloadsAction,
            clearCompletedDownloadsAction, addIgnoreAction);
        tablePanel.addTableModelListener(new MyTableModelListener());
        tablePanel.addListSelectionListener(new MyListSelectionListener());
        fileDetailsPanel = new FileDetailsPanel(getController(), true);
        fileVersionsPanel = new FileVersionsPanel(getController());
        detailsPanel = createDetailsPanel();
        detailsPanel.setVisible(false);
        buildStatsPanel();
        update0();
    }

    private void buildStatsPanel() {
        FormLayout layout = new FormLayout(
            "3dlu, pref:grow, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref",
            "pref"); // active sep1 comp sep2 count
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        activeDownloadCountLabel = new JLabel();
        builder.add(activeDownloadCountLabel, cc.xy(3, 1));
        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(2, 12));
        builder.add(sep1, cc.xy(5, 1));
        completedDownloadCountLabel = new JLabel();
        builder.add(completedDownloadCountLabel, cc.xy(7, 1));
        JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
        sep2.setPreferredSize(new Dimension(2, 12));
        builder.add(sep2, cc.xy(9, 1));
        downloadCounterLabel = new JLabel();
        builder.add(downloadCounterLabel, cc.xy(11, 1));

        statsPanel = builder.getPanel();
    }

    /**
     * Build the toolbar component.
     */
    private void buildToolbar() {

        abortDownloadsAction = new AbortDownloadAction();
        openDownloadAction = new OpenFileAction();
        clearCompletedDownloadsAction = new ClearCompletedDownloadsAction(
            getController());
        addIgnoreAction = new AddIgnoreAction(getController());

        // NOTE true cleanup days dereferenced through Constants.CLEANUP_VALUES
        Integer x = ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY
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
        cleanupSlider.setToolTipText(Translation.get(
                "exp.downloads_information_card.auto_cleanup.frequency_tip"));

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        JToggleButton detailsBtn = new JToggleButton(new DetailsAction(
            getController()));
        detailsBtn.setIcon(null);
        bar.addGridded(detailsBtn);
        bar.addRelatedGap();
        bar.addGridded(createButton(openDownloadAction));
        bar.addRelatedGap();
        bar.addGridded(createButton(abortDownloadsAction));
        bar.addRelatedGap();
        bar.addGridded(createButton(clearCompletedDownloadsAction));
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
     * Toggle the details panel visibility.
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
        boolean singleCompleteSelected = tablePanel.isSingleCompleteSelected();
        boolean rowsExist = tablePanel.isRowsExist();
        boolean incompleteSelected = tablePanel.isIncompleteSelected();

        openDownloadAction.setEnabled(singleCompleteSelected);
        abortDownloadsAction.setEnabled(incompleteSelected);
        clearCompletedDownloadsAction.setEnabled(rowsExist);
        addIgnoreAction.setEnabled(tablePanel.getSelectedRows().length > 0);

        fileDetailsPanel.setFileInfo(tablePanel.getSelectdFile());
        fileVersionsPanel.setFileInfo(tablePanel.getSelectdFile());
    }

    private void updateCleanupLabel() {
        ConfigurationEntry.DOWNLOAD_AUTO_CLEANUP_FREQUENCY.setValue(
            getController(), String.valueOf(cleanupSlider.getValue()));
        getController().saveConfig();
        if (cleanupSlider.getValue() == 0) {
            cleanupLabel.setText(Translation.get(
                    "exp.downloads_information_card.auto_cleanup.immediate"));
        } else if (cleanupSlider.getValue() >= 4) {
            cleanupLabel.setText(Translation.get(
                    "exp.downloads_information_card.auto_cleanup.never"));
        } else {
            int trueCleanupDays = Constants.CLEANUP_VALUES[cleanupSlider.getValue()];
            cleanupLabel.setText(Translation.get(
                "exp.downloads_information_card.auto_cleanup.days",
                    String.valueOf(trueCleanupDays)));
        }
    }

    private void displayStats() {

        int activeDownloadCount = tablePanel.countActiveDownloadCount();
        activeDownloadCountLabel.setText(Translation
            .get("status.active_download_count", String
                .valueOf(activeDownloadCount)));

        int completedDownloadCount = tablePanel.countCompletedDownloadCount();
        completedDownloadCountLabel.setText(Translation.get(
            "status.completed_download_count", String
                .valueOf(completedDownloadCount)));

        double kbs = getController().getTransferManager().getDownloadCounter()
            .calculateCurrentKBS();
        downloadCounterLabel.setText(Translation.get(
            "status.download", Format.formatDecimal(kbs)));
    }

    // /////////////////
    // Inner Classes //
    // /////////////////

    private class OpenFileAction extends BaseAction {
        OpenFileAction() {
            super("action_open_file", DownloadsInformationCard.this
                .getController());
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
            super("exp.action_abort_download", DownloadsInformationCard.this
                .getController());
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
            .get("files_table_panel.file_details_tab.text"));
        tabbedPane.setToolTipTextAt(0, Translation
            .get("files_table_panel.file_details_tab.tip"));

        tabbedPane.add(fileVersionsPanel.getPanel(), Translation
            .get("files_table_panel.file_versions_tab.text"));
        tabbedPane.setToolTipTextAt(1, Translation
            .get("files_table_panel.file_versions_tab.tip"));

        return builder.getPanel();
    }

    /**
     * Clears completed uploads. See MainFrame.MyCleanupAction for accelerator
     * functionality
     */
    private class ClearCompletedDownloadsAction extends BaseAction {
        ClearCompletedDownloadsAction(Controller controller) {
            super("exp.action_clear_completed_downloads", controller);
        }

        public void actionPerformed(ActionEvent e) {
            tablePanel.clearDownloads();
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
            for (DownloadManager manager : tablePanel.getSelectedRows()) {
                if (manager != null) {
                    FileInfo fileInfo = manager.getFileInfo();
                    Folder folder = getController().getFolderRepository()
                        .getFolder(fileInfo.getFolderInfo());
                    folder.addPattern(fileInfo.getRelativeName());
                    if (manager.isStarted()) {
                        manager.abort();
                    }
                    getController().getTransferManager()
                        .checkActiveTranfersForExcludes();
                }
            }
        }
    }

}
