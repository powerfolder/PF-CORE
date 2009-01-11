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

import static de.dal33t.powerfolder.disk.SyncProfile.AUTOMATIC_SYNCHRONIZATION;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.INITIAL_FOLDER_NAME;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PROMPT_TEXT_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
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
import com.jgoodies.forms.layout.Sizes;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SwingWorker;

/**
 * A generally used wizard panel for choosing a disk location for a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class ChooseDiskLocationPanel extends PFWizardPanel {

    // Some standard user directory names from various OS.
    private static final String USER_DIR_CONTACTS = "Contacts";
    private static final String USER_DIR_DESKTOP = "Desktop";
    private static final String USER_DIR_DOCUMENTS = "Documents";
    // Ubuntu mail client
    private static final String USER_DIR_EVOLUTION = ".evolution";
    private static final String USER_DIR_FAVORITES = "Favorites";
    private static final String USER_DIR_LINKS = "Links";
    private static final String USER_DIR_MUSIC = "Music";
    private static final String USER_DIR_PICTURES = "Pictures";
    private static final String USER_DIR_RECENT_DOCUMENTS = "Recent Documents";
    private static final String USER_DIR_VIDEOS = "Videos";

    // Vista has issues with these, so instantiate separately
    private static String userDirMyDocuments;
    private static String userDirMyMusic;
    private static String userDirMyPictures;
    private static String userDirMyVideos;
    private static String appsDirOutlook;

    private static final String APPS_DIR_FIREFOX = "Mozilla" + File.separator
        + "Firefox";
    private static final String APPS_DIR_SUNBIRD = "Mozilla" + File.separator
        + "Sunbird";
    private static final String APPS_DIR_THUNDERBIRD = "Thunderbird";
    private static final String APPS_DIR_FIREFOX2 = "firefox"; // Linux
    private static final String APPS_DIR_SUNBIRD2 = "sunbird"; // Linux
    private static final String APPS_DIR_THUNDERBIRD2 = "thunderbird"; // Linux

    static {
        if (WinUtils.getInstance() != null) {
            appsDirOutlook = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_LOCAL_SETTINGS_APP_DATA, false)
                + File.separator + "Microsoft" + File.separator + "Outlook";
            userDirMyDocuments = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_PERSONAL, false);
            userDirMyMusic = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_MYMUSIC, false);
            userDirMyPictures = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_MYPICTURES, false);
            userDirMyVideos = WinUtils.getInstance().getSystemFolderPath(
                WinUtils.CSIDL_MYVIDEO, false);
        }
    }

    /**
     * Used to hold initial dir and any chooser selection changes.
     */
    private String transientDirectory;
    private WizardPanel next;
    private final String initialLocation;
    private ValueModel locationModel;
    private Map<String, File> userDirectories = new TreeMap<String, File>();
    private JTextField locationTF;
    private JRadioButton customRB;
    private JCheckBox backupByOnlineStorageBox;
    private JCheckBox createDesktopShortcutBox;
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

    public boolean validateNext(List<WizardPanel> list) {
        File localBase = new File((String) locationModel.getValue());
        getWizardContext().setAttribute(
            WizardContextAttributes.FOLDER_LOCAL_BASE, localBase);
        getWizardContext().setAttribute(
            WizardContextAttributes.BACKUP_ONLINE_STOARGE,
            backupByOnlineStorageBox.isSelected());
        getWizardContext().setAttribute(
            WizardContextAttributes.CREATE_DESKTOP_SHORTCUT,
            createDesktopShortcutBox.isSelected());
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
            sendInviteAfterCB.isSelected());

        String nick = getController().getMySelf().getNick();
        String lastPart = localBase.getName();
        getWizardContext().setAttribute(INITIAL_FOLDER_NAME,
            nick + '-' + lastPart);

        // Change to manual sync if requested.
        if (manualSyncCheckBox.isSelected()) {
            getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                SyncProfile.MANUAL_SYNCHRONIZATION);
        }
        return true;
    }

    protected JPanel buildContent() {

        StringBuilder verticalUserDirectoryLayout = new StringBuilder();
        // Include cutom button in size calculations.
        // Two buttons every row.
        for (int i = 0; i < 1 + userDirectories.size() / 3; i++) {
            verticalUserDirectoryLayout.append("pref, 5dlu, ");
        }

        String verticalLayout = verticalUserDirectoryLayout
            + "5dlu, pref, 5dlu, pref, 5dlu, pref, 15dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref";

        FormLayout layout = new FormLayout(
            "pref, 15dlu, pref, 15dlu, pref, 0:grow", verticalLayout);

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        int row = 1;

        ButtonGroup bg = new ButtonGroup();

        int col = 1;
        for (String name : userDirectories.keySet()) {
            final File file = userDirectories.get(name);
            JRadioButton button = new JRadioButton(name);
            button.addItemListener(myItemListener);
            button.setOpaque(false);
            bg.add(button);
            builder.add(button, cc.xy(col, row));
            if (col == 1) {
                col = 3;
            } else if (col == 3) {
                col = 5;
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
                .getTranslation("choose_disk_location_panel.select");
        }
        builder.addLabel(infoText, cc.xyw(1, row, 6));
        row += 2;

        builder.add(locationField, cc.xyw(1, row, 6));

        row += 2;
        builder.add(folderSizeLabel, cc.xyw(1, row, 6));

        if (!getController().isLanOnly()) {
            row += 2;
            builder.add(backupByOnlineStorageBox, cc.xyw(1, row, 6));
        }

        if (OSUtil.isWindowsSystem()) {
            row += 2;
            builder.add(createDesktopShortcutBox, cc.xyw(1, row, 5));
        }

        Object object = getWizardContext().getAttribute(SYNC_PROFILE_ATTRIBUTE);
        if (object != null && object.equals(AUTOMATIC_SYNCHRONIZATION)) {
            row += 2;
            builder.add(manualSyncCheckBox, cc.xyw(1, row, 5));
        }

        // Send Invite
        row += 2;
        builder.add(sendInviteAfterCB, cc.xyw(1, row, 5));

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

        findUserDirectories();

        FolderInfo folderInfo = (FolderInfo) getWizardContext().getAttribute(
            FOLDERINFO_ATTRIBUTE);
        if (folderInfo == null) {
            transientDirectory = ConfigurationEntry.FOLDER_BASEDIR
                .getValue(getController());
        } else {
            Folder folder1 = folderInfo.getFolder(getController());
            if (folder1 == null) {
                transientDirectory = ConfigurationEntry.FOLDER_BASEDIR
                    .getValue(getController());
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
        customRB = new JRadioButton(Translation
            .getTranslation("user.dir.custom"));
        customRB.setSelected(true);
        customRB.addItemListener(myItemListener);

        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateLocationComponents();
                updateButtons();
                startFolderSizeCalculator();
            }
        });

        locationField = createLocationField();
        Dimension dims = locationField.getPreferredSize();
        dims.width = Sizes.dialogUnitXAsPixel(147, locationField);
        locationField.setPreferredSize(dims);
        locationField.setBackground(Color.WHITE);

        folderSizeLabel = new JLabel();
        startFolderSizeCalculator();

        // Online Storage integration
        boolean backupByOS = !getController().isLanOnly()
            && Boolean.TRUE.equals(getWizardContext().getAttribute(
                WizardContextAttributes.BACKUP_ONLINE_STOARGE));
        backupByOnlineStorageBox = new JCheckBox(Translation
            .getTranslation("choose_disk_location_panel.backup_by_online_storage"));
        backupByOnlineStorageBox.setSelected(backupByOS);
        backupByOnlineStorageBox.getModel().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (backupByOnlineStorageBox.isSelected()) {
                    getController().getUIController().getApplicationModel()
                        .getServerClientModel().checkAndSetupAccount();
                }
            }
        });
        backupByOnlineStorageBox.setOpaque(false);

        // Create desktop shortcut
        createDesktopShortcutBox = new JCheckBox(Translation
            .getTranslation("choose_disk_location_panel.create_desktop_shortcut"));

        createDesktopShortcutBox.setOpaque(false);

        // Create manual sync cb
        manualSyncCheckBox = new JCheckBox(Translation
            .getTranslation("choose_disk_location_panel.maual_sync"));

        manualSyncCheckBox.setOpaque(false);

        // Send Invite
        boolean sendInvite = Boolean.TRUE.equals(getWizardContext()
            .getAttribute(SEND_INVIATION_AFTER_ATTRIBUTE));
        sendInviteAfterCB = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("choose_disk_location_panel.send_invitation"));
        sendInviteAfterCB.setOpaque(false);
        sendInviteAfterCB.setSelected(sendInvite);

    }

    private void startFolderSizeCalculator() {
        SwingWorker worker = new MySwingWorker();
        worker.start();
    }

    protected Icon getPicto() {
        return getContextPicto();
    }

    protected String getTitle() {
        return Translation.getTranslation("choose_disk_location_panel.select");
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
                    // give user a choice to locate in an alternate location.
                    boolean ok = true;
                    String messageKey = null;

                    if (!f.exists()) {
                        ok = false;
                        messageKey = "choose_disk_location_panel.directory_non_existent";
                    } else if (!f.canWrite()) {
                        ok = false;
                        messageKey = "choose_disk_location_panel.directory_no_write";
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

                        // Ask user.
                        int i = DialogFactory
                            .genericDialog(
                                getController(),
                                Translation
                                    .getTranslation("choose_disk_location_panel.target_dir"),
                                Translation.getTranslation(messageKey, f
                                    .getAbsolutePath(), alternate
                                    .getAbsolutePath()), new String[]{
                                    Translation
                                        .getTranslation("general.accept"),
                                    Translation
                                        .getTranslation("general.cancel")}, 0,
                                GenericDialogType.QUESTION);
                        if (i == 0) { // Accept
                            locationModel.setValue(alternate.getAbsolutePath());
                            if (!alternate.exists()) {
                                if (alternate.mkdirs()) {
                                    startFolderSizeCalculator();
                                }
                            }
                        }
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
        // locationButton.setEnabled(customRB.isSelected());
    }

    /**
     * Creates a pair of location text field and button.
     * 
     * @param folderInfo
     * @return
     */
    private JComponent createLocationField() {
        FormLayout layout = new FormLayout("100dlu, 4dlu, 15dlu", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        locationTF = new JTextField();
        locationTF.setEditable(false);
        locationTF.setText((String) locationModel.getValue());
        builder.add(locationTF, cc.xy(1, 1));

        JButton locationButton = new JButton(Icons.DIRECTORY);
        locationButton.setToolTipText(Translation
            .getTranslation("choose_disk_location_panel.select_file"));
        locationButton.addActionListener(new MyActionListener());
        builder.add(locationButton, cc.xy(3, 1));
        return builder.getPanel();
    }

    /**
     * Find some generic user directories. Not all will be valid for all os, but
     * that is okay.
     */
    private void findUserDirectories() {
        File userHome = new File(System.getProperty("user.home"));
        addTargetDirectory(userHome, USER_DIR_CONTACTS, Translation
            .getTranslation("user.dir.contacts"), false);
        addTargetDirectory(userHome, USER_DIR_DESKTOP, Translation
            .getTranslation("user.dir.desktop"), false);
        addTargetDirectory(userHome, USER_DIR_DOCUMENTS, Translation
            .getTranslation("user.dir.documents"), false);
        addTargetDirectory(userHome, USER_DIR_EVOLUTION, Translation
            .getTranslation("user.dir.evolution"), true);
        addTargetDirectory(userHome, USER_DIR_FAVORITES, Translation
            .getTranslation("user.dir.favorites"), false);
        addTargetDirectory(userHome, USER_DIR_LINKS, Translation
            .getTranslation("user.dir.links"), false);
        addTargetDirectory(userHome, USER_DIR_MUSIC, Translation
            .getTranslation("user.dir.music"), false);

        // Hidden by Vista.
        if (userDirMyDocuments != null && !OSUtil.isWindowsVistaSystem()) {
            addTargetDirectory(new File(userDirMyDocuments), Translation
                .getTranslation("user.dir.my_documents"), false);
        }
        if (userDirMyMusic != null && !OSUtil.isWindowsVistaSystem()) {
            addTargetDirectory(new File(userDirMyMusic), Translation
                .getTranslation("user.dir.my_music"), false);
        }
        if (userDirMyPictures != null && !OSUtil.isWindowsVistaSystem()) {
            addTargetDirectory(new File(userDirMyPictures), Translation
                .getTranslation("user.dir.my_pictures"), false);
        }
        if (userDirMyVideos != null && !OSUtil.isWindowsVistaSystem()) {
            addTargetDirectory(new File(userDirMyVideos), Translation
                .getTranslation("user.dir.my_videos"), false);
        }

        addTargetDirectory(userHome, USER_DIR_PICTURES, Translation
            .getTranslation("user.dir.pictures"), false);
        addTargetDirectory(userHome, USER_DIR_RECENT_DOCUMENTS, Translation
            .getTranslation("user.dir.recent_documents"), false);
        addTargetDirectory(userHome, USER_DIR_VIDEOS, Translation
            .getTranslation("user.dir.videos"), false);
        if (OSUtil.isWindowsSystem()) {
            String appDataname = System.getenv("APPDATA");
            if (appDataname == null && WinUtils.getInstance() != null) {
                appDataname = WinUtils.getInstance().getSystemFolderPath(
                    WinUtils.CSIDL_APP_DATA, false);
            }
            if (appDataname != null) {
                File appData = new File(appDataname);
                addTargetDirectory(appData, APPS_DIR_FIREFOX, Translation
                    .getTranslation("apps.dir.firefox"), false);
                addTargetDirectory(appData, APPS_DIR_SUNBIRD, Translation
                    .getTranslation("apps.dir.sunbird"), false);
                addTargetDirectory(appData, APPS_DIR_THUNDERBIRD, Translation
                    .getTranslation("apps.dir.thunderbird"), false);
                if (appsDirOutlook != null) {
                    addTargetDirectory(appData, appsDirOutlook, Translation
                        .getTranslation("apps.dir.outlook"), false);
                }
            } else {
                Logger.getAnonymousLogger().severe(
                    "Application data directory not found.");
            }
        } else if (OSUtil.isLinux()) {
            File appData = new File("/etc");
            addTargetDirectory(appData, APPS_DIR_FIREFOX2, Translation
                .getTranslation("apps.dir.firefox"), false);
            addTargetDirectory(appData, APPS_DIR_SUNBIRD2, Translation
                .getTranslation("apps.dir.sunbird"), false);
            addTargetDirectory(appData, APPS_DIR_THUNDERBIRD2, Translation
                .getTranslation("apps.dir.thunderbird"), false);
        } else {
            // @todo Anyone know Mac???
        }
    }

    /**
     * Adds a generic user directory if if exists for this os.
     * 
     * @param root
     * @param subdir
     * @param translation
     * @param allowHidden
     *            allow display of hidden dirs
     */
    private void addTargetDirectory(File root, String subdir,
        String translation, boolean allowHidden)
    {
        File directory = joinFile(root, subdir);
        addTargetDirectory(directory, translation, allowHidden);
    }

    private static File joinFile(File root, String subdir) {
        return new File(root + File.separator + subdir);
    }

    /**
     * Adds a generic user directory if if exists for this os.
     * 
     * @param translation
     * @param allowHidden
     *            allow display of hidden dirs
     */
    private void addTargetDirectory(File directory, String translation,
        boolean allowHidden)
    {

        // See if any folders already exists for this directory.
        // No reason to show if already subscribed.
        for (Folder folder1 : getController().getFolderRepository()
            .getFolders())
        {
            if (folder1.getDirectory().getFile().getAbsoluteFile().equals(
                directory))
            {
                return;
            }
        }

        if (directory.exists() && directory.isDirectory()
            && (allowHidden || !directory.isHidden()))
        {
            userDirectories.put(translation, directory);
        }
    }

    private void displayChooseDirectory() {
        String initial = (String) locationModel.getValue();
        String file = DialogFactory.chooseDirectory(getController(), initial);
        locationModel.setValue(file);

        // Update this so that if the user clicks other user dirs
        // and then 'Custom', the selected dir will show.
        transientDirectory = file;
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
                    .getTranslation("choose_disk_location_panel.calculating_directory_size"));
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
                    directorySize = FileUtils.calculateDirectorySize(f, 0);
                }
            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.WARNING, e.toString(), e);
            }
            return null;
        }

        private boolean canWriteDirectory(File dir) {
            File testFile;
            do {
                testFile = new File(dir, Util
                    .removeInvalidFilenameChars(IdGenerator.makeId()));
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
                                .getTranslation("choose_disk_location_panel.directory_non_existent"));
                        folderSizeLabel.setForeground(Color.red);
                    } else if (noWrite) {
                        folderSizeLabel
                            .setText(Translation
                                .getTranslation("choose_disk_location_panel.directory_no_write"));
                        folderSizeLabel.setForeground(Color.red);
                    } else {
                        folderSizeLabel.setText(Translation.getTranslation(
                                "choose_disk_location_panel.directory_size", Format
                                .formatBytes(directorySize)));
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
                if (initialLocation != null && new File(initialLocation).exists()) {
                    doRadio(initialLocation);
                }
                displayChooseDirectory();
            }
        }
    }
}