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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.toedter.calendar.JDateChooser;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.FolderService;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.wizard.table.RestoreFilesTable;
import de.dal33t.powerfolder.ui.wizard.table.RestoreFilesTableModel;
import de.dal33t.powerfolder.util.DateUtil;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Dialog for selecting a number of users.
 */
public class MultiFileRestorePanel extends PFWizardPanel {

    private static final Logger log = Logger
        .getLogger(MultiFileRestorePanel.class.getName());

    private final Folder folder;
    private final List<FileInfo> filesToRestore;
    private JProgressBar bar;
    private final JLabel infoLabel;
    private boolean hasNext;
    private JScrollPane scrollPane;
    private final RestoreFilesTableModel tableModel;
    private final List<FileInfo> fileInfosToRestore;
    private JDateChooser dateChooser;
    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;
    private JRadioButton latestVersionButton;
    private JRadioButton dateVersionButton;

    private SwingWorker worker;

    /**
     * Constructor
     * 
     * @param controller
     * @param folder
     * @param filesToRestore
     */
    public MultiFileRestorePanel(Controller controller, Folder folder,
        List<FileInfo> filesToRestore)
    {
        super(controller);
        infoLabel = new JLabel();
        this.folder = folder;
        this.filesToRestore = filesToRestore;
        tableModel = new RestoreFilesTableModel(controller);
        fileInfosToRestore = new ArrayList<FileInfo>();
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout(
                "140dlu, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref:grow",
                "pref, 3dlu, pref, 6dlu, pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.add(latestVersionButton, cc.xy(1, row));

        row += 2;
        builder.add(dateVersionButton, cc.xy(1, row));
        builder.add(dateChooser, cc.xy(3, row));
        builder.add(hourSpinner, cc.xy(5, row));
        builder.add(minuteSpinner, cc.xy(7, row));

        row += 2;
        builder.add(infoLabel,
            cc.xy(1, row, CellConstraints.CENTER, CellConstraints.DEFAULT));

        row += 2;

        bar.setIndeterminate(true);

        RestoreFilesTable table = new RestoreFilesTable(tableModel);
        scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        scrollPane.setVisible(false);
        UIUtil.removeBorder(scrollPane);
        UIUtil.setZeroWidth(scrollPane);

        // bar and scrollPane share the same slot.
        builder.add(bar, cc.xy(1, row));
        builder.add(scrollPane, cc.xyw(1, row, 9));

        return builder.getPanel();
    }

    protected String getTitle() {
        return Translation
            .getTranslation("wizard.multi_file_restore_panel.title");
    }

    protected void afterDisplay() {

        // In case user comes back from next screen.
        updateButtons();
        loadVersions();
    }

    protected void initComponents() {
        bar = new JProgressBar();
        latestVersionButton = new JRadioButton(
            Translation
                .getTranslation("wizard.multi_file_restore_panel.button_latest"));
        latestVersionButton
            .setToolTipText(Translation
                .getTranslation("wizard.multi_file_restore_panel.button_latest.tip"));
        latestVersionButton.setSelected(true);
        dateVersionButton = new JRadioButton(
            Translation
                .getTranslation("wizard.multi_file_restore_panel.button_date"));
        dateVersionButton.setToolTipText(Translation
            .getTranslation("wizard.multi_file_restore_panel.button_date.tip"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(latestVersionButton);
        bg.add(dateVersionButton);

        dateChooser = new JDateChooser();
        Calendar cal = new GregorianCalendar();
        hourSpinner = new JSpinner(new SpinnerNumberModel(
            cal.get(Calendar.HOUR_OF_DAY), 0, 23, 1));
        hourSpinner.setToolTipText(Translation.getTranslation("general.hours"));
        minuteSpinner = new JSpinner(new SpinnerNumberModel(
            cal.get(Calendar.MINUTE), 0, 59, 1));
        minuteSpinner.setToolTipText(Translation
            .getTranslation("general.minutes"));

        MyActionListener actionListener = new MyActionListener();
        latestVersionButton.addActionListener(actionListener);
        dateVersionButton.addActionListener(actionListener);
        dateChooser.addPropertyChangeListener("date",
            new MyPropertyChangeListener());
        MyChangeListener changeListener = new MyChangeListener();
        hourSpinner.addChangeListener(changeListener);
        minuteSpinner.addChangeListener(changeListener);

        latestVersionButton.setOpaque(false);
        dateVersionButton.setOpaque(false);

        updateDateChooser();
    }

    public boolean hasNext() {
        return hasNext;
    }

    public WizardPanel next() {
        return new MultiFileRestoringPanel(getController(), folder,
            fileInfosToRestore, latestVersionButton.isSelected());
    }

    private void updateDateChooser() {
        dateChooser.setVisible(dateVersionButton.isSelected());
        hourSpinner.setVisible(dateVersionButton.isSelected());
        minuteSpinner.setVisible(dateVersionButton.isSelected());
    }

    private void loadVersions() {
        hasNext = false;
        updateButtons();
        if (worker != null) {
            worker.cancel(false);
        }
        tableModel.setVersions(new ArrayList<FileInfo>());
        bar.setVisible(true);
        scrollPane.setVisible(false);
        infoLabel.setVisible(true);
        infoLabel.setText(Translation
            .getTranslation("wizard.multi_file_restore_panel.retrieving_none"));

        worker = new VersionLoaderWorker();
        worker.execute();
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class VersionLoaderWorker extends
        SwingWorker<List<FileInfo>, FileInfo>
    {
        int count = 1;

        public List<FileInfo> doInBackground() {
            List<FileInfo> versions = new ArrayList<FileInfo>(
                fileInfosToRestore.size());
            try {
                FileArchiver fileArchiver = folder.getFileArchiver();
                // Also try getting versions from OnlineStorage.
                boolean online = folder.hasMember(getController().getOSClient()
                    .getServer());
                FolderService service = null;
                if (online) {
                    ServerClient client = getController().getOSClient();
                    if (client != null && client.isConnected()
                        && client.isLoggedIn())
                    {
                        service = client.getFolderService();
                    }
                }

                // Get target date.
                Date targetDate = null;
                if (dateVersionButton.isSelected()) {
                    Calendar cal = new GregorianCalendar();
                    cal.setTime(DateUtil.zeroTime(dateChooser.getDate()));
                    cal.set(Calendar.HOUR_OF_DAY,
                        ((SpinnerNumberModel) hourSpinner.getModel())
                            .getNumber().intValue());
                    cal.set(Calendar.MINUTE,
                        ((SpinnerNumberModel) minuteSpinner.getModel())
                            .getNumber().intValue());
                    targetDate = cal.getTime();
                }

                List<FileInfo> fileInfos = new ArrayList<FileInfo>();
                fileInfos.addAll(filesToRestore);

                for (FileInfo fileInfo : fileInfos) {
                    if (isCancelled()) {
                        return Collections.emptyList();
                    }
                    List<FileInfo> infoList = fileArchiver
                        .getArchivedFilesInfos(fileInfo);
                    FileInfo mostRecent = null;
                    for (FileInfo info : infoList) {
                        if (isBetterVersion(mostRecent, info, targetDate)) {
                            mostRecent = info;
                        }
                    }

                    if (service != null) {
                        try {
                            List<FileInfo> serviceList = service
                                .getArchivedFilesInfos(fileInfo);
                            for (FileInfo info : serviceList) {
                                if (isBetterVersion(mostRecent, info,
                                    targetDate))
                                {
                                    mostRecent = info;
                                }
                            }
                        } catch (Exception e) {
                            log.warning("Unable to check server/cloud archive: "
                                + fileInfo + ". " + e);
                        }
                    }

                    if (mostRecent != null) {
                        versions.add(mostRecent);
                        publish(mostRecent);
                    } else if (latestVersionButton.isSelected()) {
                        // No archives? Add fileInfo so it will be redownloaded.
                        versions.add(fileInfo);
                        publish(fileInfo);
                    }
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "Exception", e);
                infoLabel.setText(Translation.getTranslation(
                        "wizard.multi_file_restore_panel.retrieving_failure"));
            }
            return versions;
        }

        @Override
        protected void process(List<FileInfo> versions) {
            scrollPane.setVisible(true);
            bar.setVisible(false);
            tableModel.addVersions(versions);
            hasNext = false;
            if (versions.isEmpty()) {
                infoLabel.setText(Translation.getTranslation(
                        "wizard.multi_file_restore_panel.retrieving_none"));
            } else {
                infoLabel.setText(Translation.getTranslation(
                    "wizard.multi_file_restore_panel.retrieving",
                    Format.formatLong(count++),
                    Format.formatLong(filesToRestore.size())));
            }
            bar.setVisible(false);
            updateButtons();
        }

        private boolean isBetterVersion(FileInfo mostRecent, FileInfo info,
            Date targetDate)
        {
            if (mostRecent == null
                || mostRecent.getVersion() < info.getVersion())
            {
                return targetDate == null
                    || DateUtil.isBeforeEndOfDate(info.getModifiedDate(),
                        targetDate);
            }
            return false;
        }

        protected void done() {
            // No need to push this code into the EDT.
            // SwingWorker.done() gets always executed in EDT.
            scrollPane.setVisible(true);
            bar.setVisible(false);
            boolean empty = tableModel.getRowCount() == 0;
            hasNext = !empty;
            fileInfosToRestore.clear();
            try {
                fileInfosToRestore.addAll(get());
            } catch (CancellationException e) {
                log.log(Level.INFO, "Restore was cancelled");
            } catch (Exception e) {
                log.log(Level.WARNING, "Unable to add files to restore. " + e,
                    e);
            }
            infoLabel
                .setText(Translation
                    .getTranslation("wizard.multi_file_restore_panel.retrieving_success"));
            bar.setVisible(false);
            updateButtons();
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(latestVersionButton)
                || e.getSource().equals(dateVersionButton))
            {
                updateDateChooser();
                loadVersions();
            }
        }
    }

    private class MyPropertyChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            loadVersions();
        }
    }

    private class MyChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            loadVersions();
        }
    }
}