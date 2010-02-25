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
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.BACKUP_ONLINE_STOARGE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.CREATE_DESKTOP_SHORTCUT;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_CREATE_ITEMS;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PROMPT_TEXT_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FILE_COUNT;

import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.OnlineStorageSubscription;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SwingWorker;
import de.dal33t.powerfolder.util.ui.UIUtil;
import de.dal33t.powerfolder.util.ui.UserDirectories;

/**
 * A generally used wizard panel for choosing a disk location for a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class ChooseMultiDiskLocationPanel extends PFWizardPanel {

    private static Map<String, File> userDirectories;
    private WizardPanel next;
    private JLabel folderSizeLabel;
    private JLabel osWarningLabel;

    private JList customDirectoryList;
    private DefaultListModel customDirectoryListModel;
    private JCheckBox backupByOnlineStorageBox;
    private JCheckBox createDesktopShortcutBox;
    private JCheckBox manualSyncCheckBox;
    private JCheckBox sendInviteAfterCB;
    private Action addAction;
    private Action removeAction;
    private JButton removeButton;
    private String initialDirectory;
    private List<JCheckBox> boxes;
    private ServerClientListener listener;

    /**
     * Creates a new disk location wizard panel. Name of new folder is
     * automatically generated, folder will be secret
     * 
     * @param controller
     * @param next
     *            the next panel after selecting the directory.
     */
    public ChooseMultiDiskLocationPanel(Controller controller, WizardPanel next)
    {
        super(controller);
        Reject.ifNull(next, "Next wizard panel is null");
        this.next = next;
    }

    // From WizardPanel *******************************************************

    public WizardPanel next() {
        return next;
    }

    public boolean hasNext() {
        return countSelectedFolders() > 0;
    }

    private int countSelectedFolders() {
        int count = customDirectoryListModel.getSize();

        for (JCheckBox box : boxes) {
            if (box.isSelected()) {
                count++;
            }
        }
        return count;
    }

    public boolean validateNext() {

        List<FolderCreateItem> folderCreateItems = new ArrayList<FolderCreateItem>();

        SyncProfile syncProfile = (SyncProfile) getWizardContext()
            .getAttribute(SYNC_PROFILE_ATTRIBUTE);
        Reject.ifNull(syncProfile, "No default sync profile");

        // Check boxes
        for (String boxName : userDirectories.keySet()) {
            for (JCheckBox box : boxes) {
                if (box.getText().equals(boxName)) {
                    if (box.isSelected()) {
                        FolderCreateItem item = new FolderCreateItem(
                            userDirectories.get(boxName));
                        item.setSyncProfile(syncProfile);
                        item.setFolderInfo(createFolderInfo(boxName));
                        folderCreateItems.add(item);
                    }
                }
            }
        }

        // Additional folders
        for (int i = 0; i < customDirectoryListModel.size(); i++) {
            String dir = (String) customDirectoryListModel.getElementAt(i);
            File file = new File(dir);
            FolderCreateItem item = new FolderCreateItem(file);
            item.setSyncProfile(syncProfile);
            folderCreateItems.add(item);
        }

        getWizardContext().setAttribute(FOLDER_CREATE_ITEMS, folderCreateItems);

        getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE,
            backupByOnlineStorageBox.isSelected());
        getWizardContext().setAttribute(CREATE_DESKTOP_SHORTCUT,
            createDesktopShortcutBox.isSelected());

        // Don't allow send after if 2 or more folders.
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
            sendInviteAfterCB.isSelected() && countSelectedFolders() <= 1);

        // Change to manual sync if requested.
        if (manualSyncCheckBox.isSelected()) {
            getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                SyncProfile.MANUAL_SYNCHRONIZATION);
        }
        return true;
    }

    protected JPanel buildContent() {

        StringBuilder verticalUserDirectoryLayout = new StringBuilder();
        // Four buttons every row.
        for (int i = 0; i < 1 + userDirectories.size() / 4; i++) {
            verticalUserDirectoryLayout.append("pref, 3dlu, ");
        }

        String verticalLayout = verticalUserDirectoryLayout
            + "9dlu, pref, 3dlu, 40dlu, 3dlu, pref, 12dlu, pref, 3dlu, max(16dlu;pref), 12dlu, pref";
        // info custom add size os w
        // Fixed (60dlu) sizing used so that other components display okay if
        // there is only 1 or two (or even zero) check boxes displayed.
        FormLayout layout = new FormLayout(
            "60dlu, 15dlu, 60dlu, 15dlu, 60dlu, 15dlu, 60dlu, 0:grow",
            verticalLayout);
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        int row = 1;
        int col = 1;

        for (String name : userDirectories.keySet()) {
            JCheckBox box = new JCheckBox(name);
            box.setOpaque(false);
            box.addActionListener(new MyActionListener());
            boxes.add(box);
            builder.add(box, cc.xy(col, row));
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
        }
        row += 3;

        builder
            .addLabel(
                Translation
                    .getTranslation("wizard.choose_multi_disk_location.select_additional"),
                cc.xyw(1, row, 6));
        row += 2;

        builder.add(new JScrollPane(customDirectoryList), cc.xyw(1, row, 7));
        row += 2;

        builder.add(new JButton(addAction), cc.xy(1, row));
        builder.add(removeButton, cc.xy(3, row));
        builder.add(folderSizeLabel, cc.xyw(5, row, 3));
        row += 2;

        if (!getController().isLanOnly()
            && PreferencesEntry.USE_ONLINE_STORAGE
                .getValueBoolean(getController()))
        {
            builder.add(backupByOnlineStorageBox, cc.xyw(1, row, 3));
        }
        if (OSUtil.isWindowsSystem()) {
            builder.add(createDesktopShortcutBox, cc.xyw(5, row, 3));
        }
        row += 2;

        // Send Invite
        if (getController().isBackupOnly()) {
            // Cannot invite in backup only mode
            sendInviteAfterCB.setSelected(false);
        } else {
            builder.add(sendInviteAfterCB, cc.xyw(1, row, 3));
        }
        Object object = getWizardContext().getAttribute(SYNC_PROFILE_ATTRIBUTE);
        if (object != null && object.equals(AUTOMATIC_SYNCHRONIZATION)) {
            builder.add(manualSyncCheckBox, cc.xyw(5, row, 3));
        }
        row += 2;

        builder.add(osWarningLabel, cc.xyw(1, row, 6));

        return builder.getPanel();
    }

    /**
     * Initalizes all required components
     */
    protected void initComponents() {

        initialDirectory = getController().getFolderRepository()
            .getFoldersBasedir();

        userDirectories = UserDirectories
            .getUserDirectoriesFiltered(getController());

        folderSizeLabel = new JLabel();
        osWarningLabel = new JLabel();
        startFolderSizeCalculator();

        boxes = new ArrayList<JCheckBox>();

        addAction = new MyAddAction(getController());
        removeAction = new MyRemoveAction(getController());
        removeButton = new JButton(removeAction);

        customDirectoryListModel = new DefaultListModel();
        customDirectoryList = new JList(customDirectoryListModel);
        customDirectoryList
            .setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        customDirectoryList
            .addListSelectionListener(new MyListSelectionListener());

        // Online Storage integration
        boolean backupByOS = !getController().isLanOnly()
            && PreferencesEntry.USE_ONLINE_STORAGE
                .getValueBoolean(getController())
            && Boolean.TRUE.equals(getWizardContext().getAttribute(
                BACKUP_ONLINE_STOARGE));
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
                startFolderSizeCalculator();
            }
        });
        backupByOnlineStorageBox.setOpaque(false);

        // Create desktop shortcut
        createDesktopShortcutBox = new JCheckBox(
            Translation
                .getTranslation("wizard.choose_disk_location.create_desktop_shortcut"));

        createDesktopShortcutBox.setOpaque(false);

        // Create manual sync cb
        manualSyncCheckBox = new JCheckBox(Translation
            .getTranslation("wizard.choose_disk_location.maual_sync"));

        manualSyncCheckBox.setOpaque(false);

        // Send Invite
        boolean sendInvite = Boolean.TRUE.equals(getWizardContext()
            .getAttribute(SEND_INVIATION_AFTER_ATTRIBUTE));
        sendInviteAfterCB = SimpleComponentFactory.createCheckBox(Translation
            .getTranslation("wizard.choose_disk_location.send_invitation"));
        sendInviteAfterCB.setOpaque(false);
        sendInviteAfterCB.setSelected(sendInvite);

        enableRemoveAction();

        listener = new MyServerClientListener();
    }

    @Override
    protected void afterDisplay() {
        super.afterDisplay();
        if (listener != null) {
            getController().getOSClient().addListener(listener);
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (listener != null) {
            getController().getOSClient().removeListener(listener);
        }
    }

    protected String getTitle() {
        String title = (String) getWizardContext().getAttribute(
            PROMPT_TEXT_ATTRIBUTE);
        if (StringUtils.isNotBlank(title)) {
            return title;
        }
        return Translation
            .getTranslation("wizard.choose_multi_disk_location.title");
    }

    private void startFolderSizeCalculator() {
        SwingWorker worker = new MySwingWorker();
        worker.start();
    }

    private void enableRemoveAction() {
        removeAction.setEnabled(!customDirectoryList.getSelectionModel()
            .isSelectionEmpty());
        removeButton.setVisible(removeAction.isEnabled());
    }

    protected void updateButtons() {
        super.updateButtons();
        if (countSelectedFolders() <= 1) {
            sendInviteAfterCB.setEnabled(true);
            sendInviteAfterCB.setVisible(true);
        } else {
            sendInviteAfterCB.setSelected(false);
            sendInviteAfterCB.setEnabled(false);
            sendInviteAfterCB.setVisible(false);
        }
    }

    private class MyAddAction extends BaseAction {

        MyAddAction(Controller controller) {
            super("action_add_directory", controller);
        }

        public void actionPerformed(ActionEvent e) {
            File file = DialogFactory.chooseDirectory(getUIController(),
                    initialDirectory);
            if (file == null) {
                return;
            }
            File localBase = new File(getController().getFolderRepository()
                .getFoldersBasedir());
            if (file.equals(localBase)) {
                DialogFactory
                    .genericDialog(
                        getController(),
                        Translation
                            .getTranslation("wizard.choose_disk_location.local_base.title"),
                        Translation
                            .getTranslation("wizard.choose_disk_location.local_base.text"),
                        GenericDialogType.ERROR);
                return;
            }
            if (file.exists()) {
                initialDirectory = file.getAbsolutePath();
                if (!customDirectoryListModel.contains(file.getAbsolutePath())) {
                    customDirectoryListModel.addElement(file.getAbsolutePath());
                    updateButtons();
                    startFolderSizeCalculator();
                }
            }
        }
    }

    private class MyRemoveAction extends BaseAction {

        MyRemoveAction(Controller controller) {
            super("action_remove_directory", controller);
        }

        public void actionPerformed(ActionEvent e) {
            customDirectoryListModel.removeRange(customDirectoryList
                .getSelectionModel().getMinSelectionIndex(),
                customDirectoryList.getSelectionModel().getMaxSelectionIndex());
            updateButtons();
            startFolderSizeCalculator();
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            enableRemoveAction();
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updateButtons();
            startFolderSizeCalculator();
        }
    }

    private class MyServerClientListener implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
            startFolderSizeCalculator();
        }

        public void login(ServerClientEvent event) {
            startFolderSizeCalculator();
        }

        public void serverConnected(ServerClientEvent event) {
        }

        public void serverDisconnected(ServerClientEvent event) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    private class MySwingWorker extends SwingWorker {

        private int recursiveFileCount = 0;
        private long totalDirectorySize = 0;
        private boolean valid = true;

        protected void beforeConstruct() {
            folderSizeLabel
                .setText(Translation
                    .getTranslation("wizard.choose_disk_location.calculating_directory_size"));
            folderSizeLabel.setForeground(SystemColor.textText);
        }

        public Object construct() {
            try {
                recursiveFileCount = 0;
                totalDirectorySize = 0;
                List<File> originalList = new ArrayList<File>();
                for (String boxName : userDirectories.keySet()) {
                    for (JCheckBox box : boxes) {
                        if (box.getText().equals(boxName)) {
                            if (box.isSelected()) {
                                File file = userDirectories.get(boxName);
                                originalList.add(file);
                            }
                        }
                    }
                }

                for (int i = 0; i < customDirectoryListModel.getSize(); i++) {
                    String dir = (String) customDirectoryListModel.elementAt(i);
                    File file = new File(dir);
                    originalList.add(file);
                }

                for (File file : originalList) {
                    Long[] longs = FileUtils
                        .calculateDirectorySizeAndCount(file);
                    totalDirectorySize += longs[0];
                    recursiveFileCount += longs[1];
                }
                getWizardContext().setAttribute(FILE_COUNT, recursiveFileCount);

                List<File> finalList = new ArrayList<File>();
                for (String boxName : userDirectories.keySet()) {
                    for (JCheckBox box : boxes) {
                        if (box.getText().equals(boxName)) {
                            if (box.isSelected()) {
                                File file = userDirectories.get(boxName);
                                finalList.add(file);
                            }
                        }
                    }
                }
                for (int i = 0; i < customDirectoryListModel.getSize(); i++) {
                    String dir = (String) customDirectoryListModel.elementAt(i);
                    File file = new File(dir);
                    finalList.add(file);
                }

                // Any selection changes during size calculations?
                if (originalList.size() != finalList.size()
                    || !originalList.containsAll(finalList))
                {
                    valid = false;
                }

            } catch (Exception e) {
                Logger.getAnonymousLogger().log(Level.WARNING, e.toString(), e);
            }
            return null;
        }

        public void finished() {
            if (valid) {
                try {
                    folderSizeLabel.setText(Translation.getTranslation(
                        "wizard.choose_disk_location.total_directory_size",
                        Format.formatBytes(totalDirectorySize), Format
                            .formatLong(recursiveFileCount)));
                    osWarningLabel.setText("");
                    osWarningLabel.setIcon(null);
                    if (backupByOnlineStorageBox.isSelected()) {
                        ServerClient client = getController().getOSClient();
                        OnlineStorageSubscription storageSubscription = client
                            .getAccount().getOSSubscription();
                        if (client.isConnected()) {
                            long totalStorage = storageSubscription
                                .getStorageSize();
                            long spaceUsed = client.getAccountDetails()
                                .getSpaceUsed();
                            if (spaceUsed + totalDirectorySize > totalStorage) {
                                osWarningLabel
                                    .setText(Translation
                                        .getTranslation("wizard.choose_disk_location.os_over_size"));
                                osWarningLabel.setIcon(Icons
                                    .getIconById(Icons.WARNING));
                            }
                        }
                    }

                } catch (Exception e) {
                    Logger.getAnonymousLogger().log(Level.WARNING,
                        e.toString(), e);
                }
            }
        }
    }

    private static FolderInfo createFolderInfo(String name) {
        // Create new folder info
        String folderId = '[' + IdGenerator.makeId() + ']';
        return new FolderInfo(name, folderId).intern();
    }
}