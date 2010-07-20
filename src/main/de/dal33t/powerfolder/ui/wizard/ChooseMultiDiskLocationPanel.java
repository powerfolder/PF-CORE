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
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FILE_COUNT;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_CREATE_ITEMS;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.PROMPT_TEXT_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;

import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.OnlineStorageSubscription;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.LinkFolderOnlineDialog;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.SwingWorker;
import de.dal33t.powerfolder.util.ui.UserDirectories;

/**
 * A generally used wizard panel for choosing a disk location for a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class ChooseMultiDiskLocationPanel extends PFWizardPanel {

    private Map<String, File> userDirectories;
    private WizardPanel next;
    private JLabel folderSizeLabel;
    private JLabel osWarningLabel;

    private JList customDirectoryList;
    private DefaultListModel customDirectoryListModel;
    private JCheckBox backupByOnlineStorageBox;
    private JCheckBox manualSyncCheckBox;
    private JCheckBox sendInviteAfterCB;
    private Action addAction;
    private Action removeAction;
    private JButton removeButton;
    private Action linkAction;
    private JButton linkButton;
    private String initialDirectory;
    private List<JCheckBox> boxes;
    private ServerClientListener listener;

    // Map<file, folderName>
    private Map<File, String> links;

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
        links = new HashMap<File, String>();
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
            // Ignore disabled boxes - these are already set up.
            if (box.isSelected() && box.isEnabled()) {
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
                    // Ignore disabled boxes - these are already set up.
                    if (box.isSelected() && box.isEnabled()) {
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

        // Add links
        for (Map.Entry<File, String> entry : links.entrySet()) {
            entry.getKey();
            for (FolderCreateItem item : folderCreateItems) {
                if (item.getLocalBase().equals(entry.getKey())) {
                    item.setLinkToOnlineFolder(entry.getValue());
                }
            }
        }

        getWizardContext().setAttribute(FOLDER_CREATE_ITEMS, folderCreateItems);

        getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE,
            backupByOnlineStorageBox.isSelected());
        getWizardContext().setAttribute(CREATE_DESKTOP_SHORTCUT, false);

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
        // Create boxes.
        JCheckBox allBox = new JCheckBox(Translation.getTranslation("wizard.choose_multi_disk_location.all_files"));
        allBox.setOpaque(false);
        allBox.addActionListener(new MyAllActionListner());
        boxes.add(allBox);
        for (String name : userDirectories.keySet()) {
            JCheckBox box = new JCheckBox(name);
            box.setOpaque(false);
            box.addActionListener(new MyActionListener());
            boxes.add(box);
        }

        StringBuilder verticalUserDirectoryLayout = new StringBuilder();
        // Four buttons every row.
        for (int i = 0; i < 1 + boxes.size() / 4; i++) {
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

        for (JCheckBox box : boxes) {
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

        builder.add(createCustomButtons(), cc.xyw(1, row, 7));
        row += 2;

        if (!getController().isLanOnly()
            && PreferencesEntry.USE_ONLINE_STORAGE
                .getValueBoolean(getController())
            && !ConfigurationEntry.BACKUP_ONLY_CLIENT
                .getValueBoolean(getController()))
        {
            builder.add(backupByOnlineStorageBox, cc.xyw(1, row, 3));
        }
        Object object = getWizardContext().getAttribute(SYNC_PROFILE_ATTRIBUTE);
        if (object != null && object.equals(AUTOMATIC_SYNCHRONIZATION)) {
            builder.add(manualSyncCheckBox, cc.xyw(5, row, 3));
        }
        row += 2;

        // Send Invite
        if (getController().isBackupOnly()) {
            // Cannot invite in backup only mode
            sendInviteAfterCB.setSelected(false);
        } else {
            builder.add(sendInviteAfterCB, cc.xyw(1, row, 3));
        }
        row += 2;

        builder.add(osWarningLabel, cc.xyw(1, row, 6));

        return builder.getPanel();
    }

    private JPanel createCustomButtons() {
        FormLayout layout = new FormLayout(
            "pref, 3dlu, pref, 3dlu, pref, 0:grow, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(new JButtonMini(addAction), cc.xy(1, 1));
        builder.add(removeButton, cc.xy(3, 1));
        builder.add(linkButton, cc.xy(5, 1));
        builder.add(folderSizeLabel, cc.xy(8, 1));
        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Initalizes all required components
     */
    protected void initComponents() {

        initialDirectory = getController().getFolderRepository()
            .getFoldersBasedir();

        userDirectories = UserDirectories.getUserDirectories();

        folderSizeLabel = new JLabel();
        osWarningLabel = new JLabel();

        boxes = new ArrayList<JCheckBox>();

        addAction = new MyAddAction(getController());
        removeAction = new MyRemoveAction(getController());
        removeButton = new JButtonMini(removeAction);
        linkAction = new MyLinkAction(getController());
        linkButton = new JButtonMini(linkAction);

        customDirectoryListModel = new DefaultListModel();
        customDirectoryList = new JList(customDirectoryListModel);
        customDirectoryList
            .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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

        enableRemoveLinkAction();

        listener = new MyServerClientListener();
    }

    @Override
    protected void afterDisplay() {
        super.afterDisplay();
        if (listener != null) {
            getController().getOSClient().addListener(listener);
        }
        startFolderSizeCalculator();
        startConfigureCheckboxes();
    }

    @Override
    public void finish() {
        if (listener != null) {
            getController().getOSClient().removeListener(listener);
            super.finish();
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
        SwingWorker worker = new MyFolderSizeSwingWorker();
        worker.start();
    }

    private void enableRemoveLinkAction() {
        boolean enabled = !customDirectoryList.getSelectionModel()
            .isSelectionEmpty();
        removeAction.setEnabled(enabled);
        removeButton.setVisible(removeAction.isEnabled());
        linkAction.setEnabled(enabled);
        linkButton.setVisible(linkAction.isEnabled());
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

    /**
     * Start process to configure userDirectory checkboxes.
     */
    private void startConfigureCheckboxes() {
        SwingWorker worker = new MyConfigureCBSwingWorker();
        worker.start();
    }

    private static FolderInfo createFolderInfo(String name) {
        // Create new folder info
        String folderId = '[' + IdGenerator.makeId() + ']';
        return new FolderInfo(name, folderId);
    }

    public void link(File file, String folderName) {
        if (StringUtils.isBlank(folderName)) {
            links.remove(file);
        } else {
            links.put(file, folderName);
            backupByOnlineStorageBox.setSelected(false);
        }
        backupByOnlineStorageBox.setVisible(links.isEmpty());
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class MyFolderSizeSwingWorker extends SwingWorker {

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
                            // Ignore disabled boxes - these are already set up.
                            if (box.isSelected() && box.isEnabled()) {
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
                            // Ignore disabled boxes - these are already set up.
                            if (box.isSelected() && box.isEnabled()) {
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

    /**
     * Worker to gray out checkboxes for synchronized folders and bold
     * checkboxes for non-sync online folders.
     */
    private class MyConfigureCBSwingWorker extends SwingWorker {
        public Object construct() throws Throwable {
            Collection<Folder> folders = getController().getFolderRepository()
                .getFolders();
            ServerClient client = getController().getOSClient();
            for (String name : userDirectories.keySet()) {
                File file = userDirectories.get(name);
                boolean local = false;
                for (Folder folder : folders) {
                    if (folder.getLocalBase().equals(file)) {
                        // Locally synchronized.
                        local = true;
                        break;
                    }
                }

                boolean onlineOnly = false;
                if (!local) {
                    if (client != null && client.isConnected()
                        && client.isLoggedIn())
                    {
                        Collection<FolderInfo> accountFolders = client
                            .getAccountFolders();
                        for (FolderInfo accountFolder : accountFolders) {
                            if (accountFolder.getName().equals(name)) {
                                onlineOnly = true;
                                break;
                            }
                        }
                    }
                }

                // Find checkbox
                for (JCheckBox box : boxes) {
                    if (box.getText().equals(name)) {
                        Font font = new Font(box.getFont().getName(), !local
                            && onlineOnly ? Font.BOLD : Font.PLAIN, box
                            .getFont().getSize());
                        box.setFont(font);
                        box.setEnabled(!local);
                        if (local) {
                            box
                                .setToolTipText(Translation
                                    .getTranslation("wizard.choose_disk_location.already_synchronized"));
                            box.setSelected(true);
                        } else if (onlineOnly) {
                            box
                                .setToolTipText(Translation
                                    .getTranslation("wizard.choose_disk_location.already_online"));
                        } else {
                            box.setToolTipText(null);
                        }
                        break;
                    }
                }
            }
            return null;
        }
    }

    private class MyLinkAction extends BaseAction {

        MyLinkAction(Controller controller) {
            super("action_link_directory", controller);
            putValue(NAME, "");
        }

        public void actionPerformed(ActionEvent e) {
            String fileName = (String) customDirectoryList.getSelectedValue();
            File file = new File(fileName);
            LinkFolderOnlineDialog dialog = new LinkFolderOnlineDialog(
                getController(), ChooseMultiDiskLocationPanel.this, file, links
                    .get(file));
            dialog.open();
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            enableRemoveLinkAction();
        }
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updateButtons();
            startFolderSizeCalculator();
        }
    }

    private class MyAllActionListner implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            boolean selected = ((JCheckBox) e.getSource()).isSelected();
            for (JCheckBox box : boxes) {
                if (box.isEnabled()) {
                    box.setSelected(selected);
                }
            }
            updateButtons();
            startFolderSizeCalculator();
        }
    }

    private class MyServerClientListener implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
            startFolderSizeCalculator();
            startConfigureCheckboxes();
        }

        public void login(ServerClientEvent event) {
            startFolderSizeCalculator();
            startConfigureCheckboxes();
        }

        public void serverConnected(ServerClientEvent event) {
            startConfigureCheckboxes();
        }

        public void serverDisconnected(ServerClientEvent event) {
            startConfigureCheckboxes();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }

    }

    private class MyAddAction extends BaseAction {

        MyAddAction(Controller controller) {
            super("action_add_directory", controller);
            putValue(NAME, "");
        }

        public void actionPerformed(ActionEvent e) {

            List<String> onlineFolders = new ArrayList<String>();
            ServerClient client = getController().getOSClient();
            if (client.isConnected()) {
                for (FolderInfo folderInfo : client.getAccountFolders()) {
                    onlineFolders.add(folderInfo.getName());
                }
            }

            File file = DialogFactory.chooseDirectory(getUIController(),
                initialDirectory == null ? null : new File(initialDirectory),
                onlineFolders);
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
            initialDirectory = file.getAbsolutePath();
            if (!customDirectoryListModel.contains(file.getAbsolutePath())) {
                customDirectoryListModel.addElement(file.getAbsolutePath());
                customDirectoryList.setSelectedIndex(customDirectoryListModel
                    .size() - 1);
                updateButtons();
                startFolderSizeCalculator();
            }
        }
    }

    private class MyRemoveAction extends BaseAction {

        MyRemoveAction(Controller controller) {
            super("action_remove_directory", controller);
            putValue(NAME, "");
        }

        public void actionPerformed(ActionEvent e) {
            customDirectoryListModel.remove(customDirectoryList
                .getSelectedIndex());
            updateButtons();
            startFolderSizeCalculator();
        }
    }

}