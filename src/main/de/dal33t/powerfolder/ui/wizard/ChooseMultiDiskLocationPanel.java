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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.OnlineStorageSubscription;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.dialog.LinkFolderOnlineDialog;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.ProUtil;
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
public class ChooseMultiDiskLocationPanel extends PFWizardPanel {

    private Map<String, UserDirectory> userDirectories;
    private WizardPanel next;
    private JLabel folderSizeLabel;
    private LinkLabel warningLabel;

    private JComponent customDirectoryComp;
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

    private final boolean cancelNotFinish;

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
    public ChooseMultiDiskLocationPanel(Controller controller, WizardPanel next,
                                        boolean cancelNotFinish)
    {
        super(controller);
        Reject.ifNull(next, "Next wizard panel is null");
        this.next = next;
        links = new HashMap<File, String>();
        this.cancelNotFinish = cancelNotFinish;
    }

    // From WizardPanel *******************************************************

    public WizardPanel next() {
        return next;
    }

    public boolean hasNext() {
        boolean limitedLicense = getGBsAllowed() > 0
            || ProUtil.isTrial(getController());
        return countSelectedFolders() > 0
            && !(limitedLicense && warningLabel.getUIComponent().isVisible());
    }

    private int getGBsAllowed() {
        try {
            return (Integer) getController().getUIController()
                .getApplicationModel().getLicenseModel().getGbAllowedModel()
                .getValue();
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.WARNING, e.toString(), e);
            return -1;
        }
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
                            userDirectories.get(boxName).getDirectory());
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
        JCheckBox allBox = new JCheckBox(
            Translation
                .getTranslation("wizard.choose_multi_disk_location.all_files"));
        allBox.setOpaque(false);
        allBox.addActionListener(new MyAllActionListner());
        boxes.add(allBox);

        boolean showAppData = PreferencesEntry.EXPERT_MODE
            .getValueBoolean(getController());

        for (String name : userDirectories.keySet()) {
            if (!showAppData && "APP DATA".equalsIgnoreCase(name)) {
                continue;
            }
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
            + "9dlu, min(40dlu;pref), 3dlu, pref, 12dlu, pref, 3dlu, max(16dlu;pref), 12dlu, pref";
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
        builder.add(customDirectoryComp, cc.xyw(1, row, 7));
        row += 2;
        builder.add(createCustomButtons(), cc.xyw(1, row, 7));
        row += 2;

        if (getController().getOSClient().isBackupByDefault()
            && PreferencesEntry.EXPERT_MODE.getValueBoolean(getController()))
        {
            builder.add(backupByOnlineStorageBox, cc.xyw(1, row, 3));
        }
        Object object = getWizardContext().getAttribute(SYNC_PROFILE_ATTRIBUTE);
        if (object != null && object.equals(AUTOMATIC_SYNCHRONIZATION)) {
            builder.add(manualSyncCheckBox, cc.xyw(5, row, 3));
        }
        row += 2;

        // Send Invite
        if (getController().isBackupOnly() ||
                !ConfigurationEntry.SERVER_INVITE_ENABLED.getValueBoolean(
                        getController())) {
            sendInviteAfterCB.setSelected(false);
        } else {
            builder.add(sendInviteAfterCB, cc.xyw(1, row, 3));
        }
        row += 2;

        builder.add(warningLabel.getUIComponent(), cc.xyw(1, row, 6));

        return builder.getPanel();
    }

    private JPanel createCustomButtons() {
        FormLayout layout = new FormLayout(
            "pref, pref, pref, pref, 0:grow, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(new JButtonMini(addAction), cc.xy(1, 1));
        builder.add(removeButton, cc.xy(2, 1));
        builder.add(linkButton, cc.xy(3, 1));
        ActionLabel additionalLabel = new ActionLabel(getController(),
                addAction);
        builder.add(additionalLabel.getUIComponent(), cc.xy(4, 1));
        builder.add(folderSizeLabel, cc.xy(6, 1));
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
        warningLabel = new LinkLabel(getController(),
            Translation.getTranslation("pro.wizard.activation.order_now"),
            ProUtil.getBuyNowURL(getController()));
        warningLabel.setVisible(false);

        boxes = new ArrayList<JCheckBox>();

        addAction = new MyAddAction(getController());
        removeAction = new MyRemoveAction(getController());
        removeButton = new JButtonMini(removeAction);
        linkAction = new MyLinkAction(getController());
        linkButton = new JButtonMini(linkAction);

        customDirectoryListModel = new DefaultListModel();
        customDirectoryList = new JList(customDirectoryListModel);
        customDirectoryComp = new JScrollPane(customDirectoryList);
        customDirectoryList
            .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customDirectoryList
            .addListSelectionListener(new MyListSelectionListener());
        customDirectoryList.getModel().addListDataListener(
            new ListDataListener() {
                public void intervalRemoved(ListDataEvent e) {
                    customDirectoryComp.setVisible(customDirectoryList
                        .getModel().getSize() > 0);
                }

                public void intervalAdded(ListDataEvent e) {
                    customDirectoryComp.setVisible(customDirectoryList
                        .getModel().getSize() > 0);
                }

                public void contentsChanged(ListDataEvent e) {
                    customDirectoryComp.setVisible(customDirectoryList
                        .getModel().getSize() > 0);
                }
            });
        customDirectoryComp.setVisible(false);

        // Online Storage integration
        boolean backupByOS = getController().getOSClient().isBackupByDefault()
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

    @Override
    public boolean canFinish() {
        return !cancelNotFinish;
    }

    @Override
    public boolean canCancel() {
        return cancelNotFinish;
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
        folderSizeLabel.setText(Translation.getTranslation(
                "wizard.choose_disk_location.calculating_directory_size"));
        folderSizeLabel.setForeground(SystemColor.textText);
        new MyFolderSizeSwingWorker().execute();
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
        new MyConfigureCBSwingWorker().execute();
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

    private class MyFolderSizeSwingWorker extends SwingWorker<Void, Void> {
        private int nDirectories = 0;
        private int recursiveFileCount = 0;
        private long totalDirectorySize = 0;
        private boolean valid = true;

        @Override
        protected Void doInBackground() throws Exception {
            try {
                recursiveFileCount = 0;
                totalDirectorySize = 0;
                List<File> originalList = new ArrayList<File>();
                for (String boxName : userDirectories.keySet()) {
                    for (JCheckBox box : boxes) {
                        if (box.getText().equals(boxName)) {
                            // Ignore disabled boxes - these are already set up.
                            if (box.isSelected() && box.isEnabled()) {
                                File file = userDirectories.get(boxName)
                                    .getDirectory();
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
                nDirectories = originalList.size();
                getWizardContext().setAttribute(FILE_COUNT, recursiveFileCount);

                List<File> finalList = new ArrayList<File>();
                for (String boxName : userDirectories.keySet()) {
                    for (JCheckBox box : boxes) {
                        if (box.getText().equals(boxName)) {
                            // Ignore disabled boxes - these are already set up.
                            if (box.isSelected() && box.isEnabled()) {
                                File file = userDirectories.get(boxName)
                                    .getDirectory();
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

        @Override
        public void done() {
            if (valid) {
                try {
                    folderSizeLabel.setText(Translation.getTranslation(
                        "wizard.choose_disk_location.total_directory_size",
                        Format.formatBytes(totalDirectorySize),
                        Format.formatLong(recursiveFileCount)));
                    warningLabel.setText("");
                    warningLabel.setIcon(null);
                    warningLabel.setVisible(false);

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
                                warningLabel.setVisible(true);
                                warningLabel
                                    .setText(Translation
                                        .getTranslation("wizard.choose_disk_location.os_over_size"));
                                warningLabel.setIcon(Icons
                                    .getIconById(Icons.WARNING));
                            }
                        }
                    }

                    // Trial licenses
                    long bytesAllowed = getGBsAllowed() * 1024 * 1024 * 1024;
                    boolean licenseOverUse = bytesAllowed > 0
                        && calculateTotalLocalSharedSize() + totalDirectorySize
                            > bytesAllowed;
                    boolean trialOverUse = ProUtil.isTrial(getController())
                        && nDirectories
                            + getController().getFolderRepository()
                                .getFoldersCount() > 3;
                    if (licenseOverUse || trialOverUse) {
                        warningLabel.setVisible(true);
                        warningLabel
                            .setText(Translation
                                .getTranslation("wizard.choose_disk_location.os_over_size"));
                        warningLabel.setIcon(Icons.getIconById(Icons.WARNING));
                    }

                } catch (Exception e) {
                    Logger.getAnonymousLogger().log(Level.WARNING,
                        e.toString(), e);
                } finally {
                    updateButtons();
                }
            }
            updateButtons();
        }

    }

    private long calculateTotalLocalSharedSize() {
        long totalSize = 0L;
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            totalSize += folder.getStatistic().getSize(
                getController().getMySelf());
        }
        return totalSize;
    }

    /**
     * Worker to gray out checkboxes for synchronized folders and bold
     * checkboxes for non-sync online folders.
     */
    private class MyConfigureCBSwingWorker extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() throws Exception {
            Collection<Folder> folders = getController().getFolderRepository()
                .getFolders();
            ServerClient client = getController().getOSClient();
            for (String name : userDirectories.keySet()) {
                File file = userDirectories.get(name).getDirectory();
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
                            box.setToolTipText(Translation
                                .getTranslation("wizard.choose_disk_location.already_synchronized"));
                            box.setSelected(true);
                        } else if (onlineOnly) {
                            box.setToolTipText(Translation
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

    @SuppressWarnings("serial")
    private class MyLinkAction extends BaseAction {

        MyLinkAction(Controller controller) {
            super("action_link_directory", controller);
            putValue(NAME, "");
        }

        public void actionPerformed(ActionEvent e) {
            String fileName = (String) customDirectoryList.getSelectedValue();
            File file = new File(fileName);
            LinkFolderOnlineDialog dialog = new LinkFolderOnlineDialog(
                getController(), ChooseMultiDiskLocationPanel.this, file,
                links.get(file));
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
        
        public void nodeServerStatusChanged(ServerClientEvent event) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    @SuppressWarnings("serial")
    private class MyAddAction extends BaseAction {

        MyAddAction(Controller controller) {
            super("action_add_directory", controller);
            putValue(NAME, Translation.getTranslation(
                    "wizard.choose_multi_disk_location.select_additional"));
        }

        public void actionPerformed(ActionEvent e) {

            List<String> onlineFolders = new ArrayList<String>();
            ServerClient client = getController().getOSClient();
            if (client.isConnected()) {
                for (FolderInfo folderInfo : client.getAccountFolders()) {
                    onlineFolders.add(folderInfo.getName());
                }
            }

            List<File> files = DialogFactory.chooseDirectory(getUIController(),
                initialDirectory == null ? null : new File(initialDirectory),
                onlineFolders, true);
            if (files.isEmpty()) {
                return;
            }
            File localBase = new File(getController().getFolderRepository()
                .getFoldersBasedir());
            // Check none are local base, that's bad.
            for (File file1 : files) {
                if (file1.equals(localBase)) {
                    DialogFactory.genericDialog(getController(),
                            Translation.getTranslation(
                                    "wizard.choose_disk_location.local_base.title"),
                            Translation.getTranslation(
                                    "wizard.choose_disk_location.local_base.text"),
                            GenericDialogType.ERROR);
                    return;
                }
            }
            // Remember the first as the initial for next time.
            initialDirectory = files.get(0).getAbsolutePath();
            // Update the list model.
            boolean changed = false;
            for (File file1 : files) {
                if (!customDirectoryListModel.contains(file1.getAbsolutePath())) {
                    customDirectoryListModel.addElement(file1.getAbsolutePath());
                    customDirectoryList.setSelectedIndex(customDirectoryListModel
                        .size() - 1);
                    changed = true;
                }
            }
            if (changed) {
                updateButtons();
                startFolderSizeCalculator();
            }
        }
    }

    @SuppressWarnings("serial")
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