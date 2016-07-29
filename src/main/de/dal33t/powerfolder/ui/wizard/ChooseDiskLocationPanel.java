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
 * $Id: ChooseDiskLocationPanel.java 20537 2012-12-15 17:11:45Z sprajc $
 */
package de.dal33t.powerfolder.ui.wizard;

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.INITIAL_FOLDER_NAME;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PROMPT_TEXT_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;

import java.awt.Color;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import jwf.WizardPanel;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * A wizard panel for choosing a disk location for a single folder, like when processing a join invite.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
@SuppressWarnings("serial")
public class ChooseDiskLocationPanel extends PFWizardPanel {

    /**
     * Used to hold initial dir and any chooser selection changes.
     */
    private WizardPanel next;
    private final String initialLocation;
    private ValueModel locationModel; // <String (directory absolute path)>
    private JTextField locationTF;
    private JCheckBox backupByOnlineStorageBox;
    private JCheckBox manualSyncCheckBox;
    private JCheckBox sendInviteAfterCB;

    private JComponent locationField;

    private JLabel folderSizeLabel;

    /**
     * Creates a new disk location wizard panel. Name of new folder is
     * automatically generated, folder will be secret
     *
     * @param controller
     * @param initialLocation
     * @param next
     *            the next panel after selecting the directory.
     */
    public ChooseDiskLocationPanel(Controller controller,
        String initialLocation, WizardPanel next)
    {
        super(controller);
        Reject.ifNull(next, "Next wizard panel is null");
        this.initialLocation = initialLocation;
        this.next = next;
    }

    // From WizardPanel *******************************************************

    public WizardPanel next() {
        return next;
    }

    public boolean hasNext() {
        return locationModel.getValue() != null && !StringUtils.isBlank(locationModel.getValue().toString());
    }

    public boolean validateNext() {

        Path location = Paths.get((String) locationModel.getValue());

        // Have to do this here as well as on choose directory
        // in case the incoming dir is bad and user does not change anything!
        if (!validDirectory(location)) {
            return false;
        }

        getWizardContext().setAttribute(
            WizardContextAttributes.FOLDER_LOCAL_BASE, location);
        getWizardContext().setAttribute(
            WizardContextAttributes.BACKUP_ONLINE_STOARGE,
            backupByOnlineStorageBox.isSelected());
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
            sendInviteAfterCB.isSelected());

        getWizardContext().setAttribute(INITIAL_FOLDER_NAME,
            PathUtils.getSuggestedFolderName(location));

        // Change to manual sync if requested.
        if (manualSyncCheckBox.isSelected()) {
            getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                SyncProfile.MANUAL_SYNCHRONIZATION);
        }
        return true;
    }

    protected JPanel buildContent() {

        String verticalLayout = "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref";

        FormLayout layout = new FormLayout("pref, 0:grow", verticalLayout);

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        String infoText = (String) getWizardContext().getAttribute(PROMPT_TEXT_ATTRIBUTE);
        if (infoText == null) {
            infoText = Translation.get("exp.wizard.choose_disk_location.select");
        }
        int row = 2;
        builder.addLabel(infoText, cc.xy(1, row));
        row += 2;

        builder.add(locationField, cc.xy(1, row));

        row += 2;
        builder.add(folderSizeLabel, cc.xy(1, row));

        if (getController().getOSClient().isBackupByDefault()
            && PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
            row += 2;
            builder.add(backupByOnlineStorageBox, cc.xy(1, row));
        }

        // Send Invite
        if (getController().isBackupOnly()
            || !ConfigurationEntry.SERVER_INVITE_ENABLED.getValueBoolean(getController())) {
            sendInviteAfterCB.setSelected(false);
        } else {
            row += 2;
            builder.add(sendInviteAfterCB, cc.xy(1, row));
        }

        return builder.getPanel();
    }

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {

        FolderInfo folderInfo = (FolderInfo) getWizardContext().getAttribute(
            FOLDERINFO_ATTRIBUTE);
        String dir;
        if (folderInfo == null) {
            dir = getController().getFolderRepository().getFoldersBasedirString();
        } else {
            Folder folder1 = folderInfo.getFolder(getController());
            if (folder1 == null) {
                dir = getController().getFolderRepository()
                    .getFoldersBasedirString();
            } else {
                dir = folder1.getLocalBase().toAbsolutePath().toString();
            }
        }

        locationModel = new ValueHolder(dir);

        if (initialLocation != null) {
            locationModel.setValue(initialLocation);
        }

        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateLocationComponents();
                updateButtons();
                startFolderSizeCalculator();
            }
        });

        locationField = createLocationField();
        locationField.setBackground(Color.WHITE);

        folderSizeLabel = new JLabel();
        startFolderSizeCalculator();

        // Online Storage integration
        boolean backupByOS = getController().getOSClient().isBackupByDefault()
            && Boolean.TRUE.equals(getWizardContext().getAttribute(WizardContextAttributes.BACKUP_ONLINE_STOARGE));
        backupByOnlineStorageBox = new JCheckBox(Translation.get(
                "exp.wizard.choose_disk_location.backup_by_online_storage"));

        // Is backup suggested?
        if (backupByOS) {
            backupByOnlineStorageBox.setSelected(true);
        }

        backupByOnlineStorageBox.getModel().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (backupByOnlineStorageBox.isSelected()) {
                    getController().getUIController().getApplicationModel()
                        .getServerClientModel().checkAndSetupAccount();
                }
            }
        });
        backupByOnlineStorageBox.setOpaque(false);

        // Create manual sync cb
        manualSyncCheckBox = new JCheckBox(Translation.get(
                "exp.wizard.choose_disk_location.maual_sync"));

        manualSyncCheckBox.setOpaque(false);

        // Send Invite
        boolean sendInvite = Boolean.TRUE.equals(getWizardContext()
            .getAttribute(SEND_INVIATION_AFTER_ATTRIBUTE));
        sendInviteAfterCB = SimpleComponentFactory.createCheckBox(Translation
            .get("exp.wizard.choose_disk_location.send_invitation"));
        sendInviteAfterCB.setOpaque(false);
        sendInviteAfterCB.setSelected(sendInvite);

    }

    private void startFolderSizeCalculator() {
        SwingWorker<Object, Object> worker = new MySwingWorker((String) locationModel.getValue());
        worker.execute();
    }

    protected String getTitle() {
        return Translation.get("exp.wizard.choose_disk_location.title");
    }

    /**
     * Try to create the original folder if it does not exists.
     */
    protected void afterDisplay() {

        Object value = locationModel.getValue();
        if (value != null) {
            if (value instanceof String) {
                try {
                    Path f = Paths.get((String) value);

                    // Try to create the directory if it does not exist.
                    if (Files.notExists(f)) {
                        Files.createDirectories(f);
                        startFolderSizeCalculator();
                    }

                    // If dir does not exist or is not writable,
                    // give user a choice to relocate in an alternate location.
                    boolean ok = true;
                    if (Files.notExists(f)) {
                        ok = false;
                    } else if (!Files.isWritable(f)) {
                        ok = false;
                    }
                    if (!ok) {
                        String baseDir = getController().getFolderRepository()
                            .getFoldersBasedirString();
                        String name = f.getFileName().toString();
                        if (name.length() == 0) { // Like f == 'E:/'
                            name = "new";
                        }
                        Path alternate = Paths.get(baseDir, name);

                        // Check alternate is unique.
                        int x = 1;
                        while (Files.exists(alternate)) {
                            alternate = Paths.get(baseDir, name + '-' + x++);
                        }

                        Files.createDirectories(alternate);
                        startFolderSizeCalculator();
                        locationModel.setValue(alternate.toAbsolutePath().toString());
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Called when the location model changes value. Sets the location text
     * field value and enables the location button.
     */
    private void updateLocationComponents() {
        String value = (String) locationModel.getValue();
        locationTF.setText(value);
    }

    /**
     * Creates a pair of location text field and button.
     *
     * @return
     */
    private JComponent createLocationField() {
        FormLayout layout = new FormLayout("140dlu, 3dlu, pref", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        locationTF = new JTextField();
        locationTF.setEditable(false);
        locationTF.setText((String) locationModel.getValue());
        builder.add(locationTF, cc.xy(1, 1));

        JButtonMini locationButton = new JButtonMini(Icons.getIconById(Icons.DIRECTORY),
                Translation.get("exp.wizard.choose_disk_location.select_directory"));
        locationButton.addActionListener(new MyActionListener());
        builder.add(locationButton, cc.xy(3, 1));
        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        return panel;
    }

    private void displayChooseDirectory() {
        String initial = (String) locationModel.getValue();
        List<Path> files = DialogFactory.chooseDirectory(getController().getUIController(), initial, false);
        if (!files.isEmpty()) {
            Path location = files.get(0);

            if (!validDirectory(location)) {
                return;
            }

            locationModel.setValue(location.toAbsolutePath().toString());
        }
    }

    private boolean validDirectory(Path location) {
        // Do not allow user to select folder base dir.
        if (location.equals(getController().getFolderRepository().getFoldersBasedir())) {
            String title = Translation.get("general.directory");
            String message = Translation.get("general.basedir_error.text");
            DialogFactory.genericDialog(getController(), title, message, GenericDialogType.ERROR);
            return false;
        }

        // Don't allow non-user dir folders if not allowed.
        if (ConfigurationEntry.FOLDER_CREATE_IN_BASEDIR_ONLY
            .getValueBoolean(getController()))
        {
            if (!location.getParent().equals(
                getController().getFolderRepository().getFoldersBasedir()))
            {
                String title = Translation.get("general.directory");
                String message = Translation.get(
                    "general.outside_basedir_error.text", getController()
                        .getFolderRepository().getFoldersBasedirString());
                DialogFactory.genericDialog(getController(), title, message,
                    GenericDialogType.ERROR);
                return false;
            }
        }

        return true;
    }

    /**
     * Action listener for the location button. Opens a choose dir dialog and
     * sets the location model with the result.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            displayChooseDirectory();
        }
    }

    /**
     * Worker to display info of the folder in the background.
     */
    private class MySwingWorker extends SwingWorker<Object, Object> {

        private final String initial;
        private long directorySize;
        private boolean nonExistent;
        private boolean noWrite;
        private boolean checkNoWrite = true;

        private MySwingWorker(String initial) {
            this.initial = initial;
        }

        protected Object doInBackground() throws Exception {

            // Show something while working.
            folderSizeLabel.setText(Translation.get("exp.wizard.choose_disk_location.calculating_directory_size"));
            folderSizeLabel.setForeground(SystemColor.textText);

            try {
                Path f = Paths.get(initial);
                if (Files.notExists(f)) {
                    nonExistent = true;
                } else if (checkNoWrite && !canWriteDirectory(f)) {
                    noWrite = true;
                } else {
                    directorySize = PathUtils.calculateDirectorySizeAndCount(f)[0];
                }
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.WARNING, e.toString(), e);
            }
            return null;
        }

        @Override
        protected void done() {
            try {
                // Has the value changed?
                // In that case, another worker is going to update the value later.
                if (initial.equals(locationModel.getValue())) {
                    if (nonExistent) {
                        folderSizeLabel.setText(Translation.get(
                                "exp.wizard.choose_disk_location.directory_non_existent"));
                        folderSizeLabel.setForeground(Color.red);
                    } else if (noWrite) {
                        folderSizeLabel.setText(Translation.get(
                                "exp.wizard.choose_disk_location.directory_no_write"));
                        folderSizeLabel.setForeground(Color.red);
                    } else if (PreferencesEntry.EXPERT_MODE.getValueBoolean(getController())) {
                        folderSizeLabel.setText(Translation.get(
                            "exp.wizard.choose_disk_location.directory_size",
                            Format.formatBytes(directorySize)));
                        folderSizeLabel.setForeground(SystemColor.textText);
                    } else {
                        folderSizeLabel.setText("");
                        folderSizeLabel.setForeground(SystemColor.textText);
                    }
                }
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.WARNING, e.toString(), e);
            }
        }

        private boolean canWriteDirectory(Path dir) {
            Path testFile;
            do {
                testFile = dir.resolve(PathUtils.removeInvalidFilenameChars(IdGenerator.makeId()));
            } while (Files.exists(testFile));
            try {
                Files.createFile(testFile);
                boolean canWrite = Files.isWritable(testFile);
                Files.delete(testFile);
                return canWrite;
            } catch (IOException e) {
                return false;
            }
        }
    }
}