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
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

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
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.util.SwingWorker;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.UserDirectory;

/**
 * A generally used wizard panel for choosing a disk location for a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
@Deprecated
public class ChooseDiskLocationPanel extends PFWizardPanel {

    /**
     * Used to hold initial dir and any chooser selection changes.
     */
    private String transientDirectory;
    private WizardPanel next;
    private final String initialLocation;
    private ValueModel locationModel;
    private static Map<String, UserDirectory> userDirectories;
    private JTextField locationTF;
    private JRadioButton customRB;
    private JCheckBox backupByOnlineStorageBox;
    private JCheckBox manualSyncCheckBox;
    private JCheckBox sendInviteAfterCB;

    private JComponent locationField;

    private JLabel folderSizeLabel;

    private MyItemListener myItemListener;

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
        if (locationModel.getValue() != null
            && !StringUtils.isBlank(locationModel.getValue().toString()))
        {
            String location = locationModel.getValue().toString();

            // Do not allow user to select folder base dir.
            return !location.equals(getController().getFolderRepository()
                .getFoldersBasedir());
        }
        return false;
    }

    public boolean validateNext() {
        File localBase = new File((String) locationModel.getValue());
        getWizardContext().setAttribute(
            WizardContextAttributes.FOLDER_LOCAL_BASE, localBase);
        getWizardContext().setAttribute(
            WizardContextAttributes.BACKUP_ONLINE_STOARGE,
            backupByOnlineStorageBox.isSelected());
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
            sendInviteAfterCB.isSelected());

        getWizardContext().setAttribute(INITIAL_FOLDER_NAME,
            FileUtils.getSuggestedFolderName(localBase));

        // Change to manual sync if requested.
        if (manualSyncCheckBox.isSelected()) {
            getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                SyncProfile.MANUAL_SYNCHRONIZATION);
        }
        return true;
    }

    protected JPanel buildContent() {

        StringBuilder verticalUserDirectoryLayout = new StringBuilder();
        // Include custom button in size calculations.
        // Four buttons every row.
        for (int i = 0; i < 1 + userDirectories.size() / 4; i++) {
            verticalUserDirectoryLayout.append("pref, 3dlu, ");
        }

        String verticalLayout = verticalUserDirectoryLayout
            + "3dlu, pref, 3dlu, pref, 3dlu, pref, 10dlu, pref, 3dlu, pref, 3dlu, pref";

        FormLayout layout = new FormLayout(
            "pref, 10dlu, pref, 10dlu, pref, 10dlu, pref, 0:grow",
            verticalLayout);

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        int row = 1;

        ButtonGroup bg = new ButtonGroup();

        int col = 1;
        for (String name : userDirectories.keySet()) {
            final File file = userDirectories.get(name).getDirectory();
            JRadioButton button = new JRadioButton(name);
            button.addItemListener(myItemListener);
            button.setOpaque(false);
            bg.add(button);
            builder.add(button, cc.xy(col, row));
            if (col == 1) {
                col = 3;
            } else if (col == 3) {
                col = 5;
            } else if (col == 5) {
                col = 7;
            } else {
                row += 2;
                col = 1;
            }

            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doRadio(file.getAbsolutePath());
                }
            });
        }

        // Custom directory.
        customRB.setOpaque(false);
        bg.add(customRB);
        builder.add(customRB, cc.xy(col, row));
        customRB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doRadio(transientDirectory);
            }
        });
        row += 3;

        String infoText = (String) getWizardContext().getAttribute(
            PROMPT_TEXT_ATTRIBUTE);
        if (infoText == null) {
            infoText = Translation
                .getTranslation("wizard.choose_disk_location.select");
        }
        builder.addLabel(infoText, cc.xyw(1, row, 8));
        row += 2;

        builder.add(locationField, cc.xyw(1, row, 8));

        row += 2;
        builder.add(folderSizeLabel, cc.xyw(1, row, 8));

        if (getController().getOSClient().isBackupByDefault()
            && PreferencesEntry.EXPERT_MODE.getValueBoolean(getController()))
        {
            row += 2;
            builder.add(backupByOnlineStorageBox, cc.xyw(1, row, 8));
        }

        // Send Invite
        if (getController().isBackupOnly()
            || !ConfigurationEntry.SERVER_INVITE_ENABLED
                .getValueBoolean(getController()))
        {
            sendInviteAfterCB.setSelected(false);
        } else {
            row += 2;
            builder.add(sendInviteAfterCB, cc.xyw(1, row, 8));
        }

        // Object object =
        // getWizardContext().getAttribute(SYNC_PROFILE_ATTRIBUTE);
        // if (object != null && object.equals(AUTOMATIC_SYNCHRONIZATION)) {
        // row += 2;
        // builder.add(manualSyncCheckBox, cc.xyw(1, row, 7));
        // }

        return builder.getPanel();
    }

    /**
     * Radio button selection.
     * 
     * @param name
     */
    private void doRadio(String name) {
        locationModel.setValue(name);
    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {

        if (initialLocation == null) {
            userDirectories = UserDirectories.getUserDirectoriesFiltered(
                getController(),
                PreferencesEntry.EXPERT_MODE.getValueBoolean(getController()));
        } else {
            userDirectories = Collections.emptyMap();
        }

        FolderInfo folderInfo = (FolderInfo) getWizardContext().getAttribute(
            FOLDERINFO_ATTRIBUTE);
        if (folderInfo == null) {
            transientDirectory = getController().getFolderRepository()
                .getFoldersBasedir();
        } else {
            Folder folder1 = folderInfo.getFolder(getController());
            if (folder1 == null) {
                transientDirectory = getController().getFolderRepository()
                    .getFoldersBasedir();
            } else {
                transientDirectory = folder1.getLocalBase().getAbsolutePath();
            }
        }
        locationModel = new ValueHolder(transientDirectory);

        if (initialLocation != null) {
            locationModel.setValue(initialLocation);
        }

        myItemListener = new MyItemListener();

        // Create customRB now,
        // so the listener does not see the initial selection later.
        customRB = new JRadioButton(
            Translation.getTranslation("user.dir.custom"));
        customRB.setSelected(true);
        customRB.addItemListener(myItemListener);
        customRB.setVisible(initialLocation == null);

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
            && Boolean.TRUE.equals(getWizardContext().getAttribute(
                WizardContextAttributes.BACKUP_ONLINE_STOARGE));
        backupByOnlineStorageBox = new JCheckBox(
            Translation
                .getTranslation("wizard.choose_disk_location.backup_by_online_storage"));
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
        manualSyncCheckBox = new JCheckBox(
            Translation
                .getTranslation("wizard.choose_disk_location.maual_sync"));

        manualSyncCheckBox.setOpaque(false);

        // Send Invite
        boolean sendInvite = Boolean.TRUE.equals(getWizardContext()
            .getAttribute(SEND_INVIATION_AFTER_ATTRIBUTE));
        sendInviteAfterCB = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("wizard.choose_disk_location.send_invitation"));
        sendInviteAfterCB.setOpaque(false);
        sendInviteAfterCB.setSelected(sendInvite);

    }

    private void startFolderSizeCalculator() {
        SwingWorker worker = new MySwingWorker();
        worker.start();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.choose_disk_location.title");
    }

    /**
     * Try to create the original folder if it does not exists.
     */
    protected void afterDisplay() {

        Object value = locationModel.getValue();
        if (value != null) {
            if (value instanceof String) {
                try {
                    File f = new File((String) value);

                    // Try to create the directory if it does not exist.
                    if (!f.exists()) {
                        if (f.mkdirs()) {
                            startFolderSizeCalculator();
                        }
                    }

                    // If dir does not exist or is not writable,
                    // give user a choice to relocate in an alternate location.
                    boolean ok = true;
                    if (!f.exists()) {
                        ok = false;
                    } else if (!f.canWrite()) {
                        ok = false;
                    }
                    if (!ok) {
                        String baseDir = getController().getFolderRepository()
                            .getFoldersBasedir();
                        String name = f.getName();
                        if (name.length() == 0) { // Like f == 'E:/'
                            name = "new";
                        }
                        File alternate = new File(baseDir, name);

                        // Check alternate is unique.
                        int x = 1;
                        while (alternate.exists()) {
                            alternate = new File(baseDir, name + '-' + x++);
                        }
                        alternate.mkdirs();
                        startFolderSizeCalculator();
                        locationModel.setValue(alternate.getAbsolutePath());
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
        if (value == null) {
            value = transientDirectory;
        }
        locationTF.setText(value);
    }

    /**
     * Creates a pair of location text field and button.
     * 
     * @return
     */
    private JComponent createLocationField() {
        FormLayout layout = new FormLayout("122dlu, 3dlu, 15dlu", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        locationTF = new JTextField();
        locationTF.setEditable(false);
        locationTF.setText((String) locationModel.getValue());
        builder.add(locationTF, cc.xy(1, 1));

        JButton locationButton = new JButton(Icons.getIconById(Icons.DIRECTORY));
        locationButton.setToolTipText(Translation
            .getTranslation("wizard.choose_disk_location.select_file"));
        locationButton.addActionListener(new MyActionListener());
        builder.add(locationButton, cc.xy(3, 1));
        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        return panel;
    }

    private void displayChooseDirectory() {
        String initial = (String) locationModel.getValue();
        List<File> files = DialogFactory.chooseDirectory(getController()
            .getUIController(), initial, false);
        if (!files.isEmpty()) {
            File localFile = files.get(0);
            locationModel.setValue(localFile.getAbsolutePath());

            // Update this so that if the user clicks other user dirs
            // and then 'Custom', the selected dir will show.
            transientDirectory = localFile.getAbsolutePath();
        }
    }

    /**
     * Action listener for the location button. Opens a choose dir dialog and
     * sets the location model with the result.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (customRB.isSelected()) {
                displayChooseDirectory();
            } else {
                // Selecting the radiobutton displays the dir chooser.
                customRB.setSelected(true);
            }
        }
    }

    private class MySwingWorker extends SwingWorker {

        private String initial;
        private long directorySize;
        private boolean nonExistent;
        private boolean noWrite;
        private boolean checkNoWrite = true;

        private MySwingWorker() {
            initial = (String) locationModel.getValue();
        }

        protected void beforeConstruct() {
            folderSizeLabel
                .setText(Translation
                    .getTranslation("wizard.choose_disk_location.calculating_directory_size"));
            folderSizeLabel.setForeground(SystemColor.textText);
        }

        @Override
        public Object construct() {
            try {
                File f = new File(initial);
                if (!f.exists()) {
                    nonExistent = true;
                } else if (checkNoWrite && !canWriteDirectory(f)) {
                    noWrite = true;
                } else {
                    directorySize = FileUtils.calculateDirectorySizeAndCount(f)[0];
                }
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.WARNING, e.toString(), e);
            }
            return null;
        }

        private boolean canWriteDirectory(File dir) {
            File testFile;
            do {
                testFile = new File(dir,
                    FileUtils.removeInvalidFilenameChars(IdGenerator.makeId()));
            } while (testFile.exists());
            try {
                testFile.createNewFile();
                boolean canWrite = testFile.canWrite();
                canWrite = canWrite && testFile.delete();
                return canWrite;
            } catch (IOException e) {
                return false;
            }
        }

        public void finished() {
            try {
                if (initial.equals(locationModel.getValue())) {
                    if (nonExistent) {
                        folderSizeLabel
                            .setText(Translation
                                .getTranslation("wizard.choose_disk_location.directory_non_existent"));
                        folderSizeLabel.setForeground(Color.red);
                    } else if (noWrite) {
                        folderSizeLabel
                            .setText(Translation
                                .getTranslation("wizard.choose_disk_location.directory_no_write"));
                        folderSizeLabel.setForeground(Color.red);
                    } else {
                        folderSizeLabel.setText(Translation.getTranslation(
                            "wizard.choose_disk_location.directory_size",
                            Format.formatBytes(directorySize)));
                        folderSizeLabel.setForeground(SystemColor.textText);
                    }
                }
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.WARNING, e.toString(), e);
            }
        }
    }

    private class MyItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (customRB.isSelected()) {
                if (initialLocation != null
                    && new File(initialLocation).exists())
                {
                    doRadio(initialLocation);
                }
                displayChooseDirectory();
            }
        }
    }
}