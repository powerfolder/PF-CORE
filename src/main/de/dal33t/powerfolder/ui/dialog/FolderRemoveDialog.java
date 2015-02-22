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
package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderRemovePermission;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.dialog.BaseDialog;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;

/**
 * Panel displayed when wanting to remove a folder
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.00 $
 */
public class FolderRemoveDialog extends BaseDialog {

    private final FolderInfo foInfo;
    private Folder folder;
    private boolean localFolder;
    private boolean onlineFolder;
    private boolean admin;
    private JButton removeButton;
    private JButton cancelButton;

    private JLabel messageLabel;

    private JCheckBox removeFromLocalBox;
    private JCheckBox removeFromServerBox;
    private JCheckBox deleteSystemSubFolderBox;

    /**
     * Contructor when used on choosen folder
     *
     * @param controller
     * @param foInfo
     */
    public FolderRemoveDialog(Controller controller, FolderInfo foInfo) {
        super(Senior.MAIN_FRAME, controller, true);
        Reject.ifNull(foInfo, "FolderInfo");
        this.foInfo = foInfo;
        folder = foInfo.getFolder(getController());
        onlineFolder = getController().getOSClient().getAccount()
            .hasReadPermissions(foInfo);
        admin = getController().getOSClient().getAccount()
            .hasAdminPermission(foInfo);
        localFolder = folder != null;
    }

    // UI Building ************************************************************

    /**
     * Initalizes all ui components
     */
    private void initComponents() {
        boolean allowRemove = !ConfigurationEntry.SECURITY_PERMISSIONS_STRICT
            .getValueBoolean(getController())
            || getController().getOSClient().getAccount()
                .hasPermission(FolderRemovePermission.INSTANCE);

        // Create folder leave dialog message
        boolean syncFlag = folder != null && folder.isTransferring();
        String folderLeaveText;
        String removeKey;
        String folderLeaveMessage, folderLeaveMessageAdmin;
        if(ConfigurationEntry.SECURITY_PERMISSIONS_STRICT.getValueBoolean(getController())){
            folderLeaveMessage = "folder_remove.dialog.strict.online_text";
            folderLeaveMessageAdmin = "folder_remove.dialog.strict.online_text.admin";
        }else{
            folderLeaveMessage = "folder_remove.dialog.online_text";
            folderLeaveMessageAdmin = "folder_remove.dialog.online_text.admin";
        }
        if(admin) {
            removeKey = onlineFolder && !localFolder
                ? folderLeaveMessageAdmin
                : "folder_remove.dialog.text";
        }else {
            removeKey = onlineFolder && !localFolder
                ? folderLeaveMessage
                : "folder_remove.dialog.text";
        }
        if (syncFlag) {
            folderLeaveText = Translation.get(removeKey,
                foInfo.getLocalizedName())
                + '\n'
                + Translation
                    .get("folder_remove.dialog.sync_warning");
        } else {
            folderLeaveText = Translation
                .get(removeKey, foInfo.getLocalizedName());
        }
        messageLabel = new JLabel(folderLeaveText);

        removeFromLocalBox = SimpleComponentFactory.createCheckBox(Translation
            .get("folder_remove.dialog.remove_from_local"));
        removeFromLocalBox.setSelected(true);
        removeFromLocalBox.setEnabled(allowRemove);
        removeFromLocalBox.addActionListener(new ConvertActionListener());

        deleteSystemSubFolderBox = SimpleComponentFactory
            .createCheckBox(Translation
                .get("folder_remove.dialog.delete"));
        deleteSystemSubFolderBox.setEnabled(allowRemove);
        deleteSystemSubFolderBox.setVisible(allowRemove);

        removeFromServerBox = SimpleComponentFactory.createCheckBox(Translation
            .get("folder_remove.dialog.remove_from_os"));
        removeFromServerBox.addActionListener(new ConvertActionListener());
        removeFromServerBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getApplicationModel().getServerClientModel()
                    .checkAndSetupAccount();
            }
        });
        removeFromServerBox.setSelected(!localFolder && onlineFolder);
        removeFromServerBox.setEnabled(allowRemove);
        removeFromServerBox.setVisible(allowRemove);

        // Buttons
        createRemoveButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeButton.setEnabled(false);
                confirmedFolderLeave(removeFromLocalBox.isSelected(),
                    deleteSystemSubFolderBox.isSelected(),
                    removeFromServerBox.isSelected());
            }
        });

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
    }

    private void createRemoveButton(ActionListener listener) {
        String removeButtonText = !admin
            ? "folder_remove_online.dialog.button.name"
            : "folder_remove_online.dialog.button.name.admin";
        if (!localFolder) {
            removeButton = new JButton(
                Translation.get(removeButtonText));
            removeButton
            .setMnemonic(Translation
                .get("folder_remove_online.dialog.button.key").trim()
                .charAt(0));
        }else {
            removeButton = new JButton(
                Translation.get("folder_remove.dialog.button.name"));
            removeButton
            .setMnemonic(Translation
                .get("folder_remove.dialog.button.key").trim()
                .charAt(0));
        }
        removeButton.addActionListener(listener);
    }

    // Methods for BaseDialog *************************************************

    public String getTitle() {
        String removeOnline = !admin
            ? "folder_remove_online.dialog.title"
            : "folder_remove_online.dialog.title.admin";
        if (!localFolder) {
            return Translation.get(removeOnline);
        }else {
            return Translation.get("folder_remove.dialog.title");
        }
    }

    protected Icon getIcon() {
        return null;
    }

    protected JComponent getContent() {

        initComponents();

        FormLayout layout;

        // ----- | ------ | ---------------------- | ---------------- |
        // local | online | remove/delete local cb | remove server cb |
        // ----- | ------ | ---------------------- | ---------------- |
        //   Y   |   Y    |           Y            |       Y          |
        //   Y   |   N    |           Y            |       N          |
        //   N   |   Y    |           N            |       N          |
        //   N   |   N    |           ?            |       ?          |
        // ----- | ------ | ---------------------- | ---------------- |

        // Remove unnecessary gaps if cbs not visible.
        if (localFolder) {
            if (onlineFolder) {
                // Local and online cbs
                layout = new FormLayout("pref:grow, 3dlu, pref:grow",
                    "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
            } else {
                // Local two cbs only
                layout = new FormLayout("pref:grow, 3dlu, pref:grow",
                    "pref, 3dlu, pref, 3dlu, pref");
            }
        } else {
         // Just online. Don't need the online cb; obvious.
            if(ConfigurationEntry.SECURITY_PERMISSIONS_STRICT.getValueBoolean(getController())){
                layout = new FormLayout("pref:grow, 3dlu, pref:grow",
                    "pref, 3dlu, pref");
            }else {
                layout = new FormLayout("pref:grow, 3dlu, pref:grow",
                    "pref");
            }

        }

        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        int row = 1;

        builder.add(messageLabel, cc.xyw(1, row, 3));
        row += 2;

        if(ConfigurationEntry.SECURITY_PERMISSIONS_STRICT.getValueBoolean(getController()) && !localFolder){
            String noteLabel;
            if(admin){
                noteLabel = Translation.get("folder_remove_online.strict.note.admin");
            }else {
                noteLabel = Translation.get("folder_remove_online.strict.note");
            }
            builder.add(new JLabel(noteLabel), cc.xyw(1, row, 3));
            row += 2;
        }


        if (localFolder) {
            builder.add(removeFromLocalBox, cc.xyw(1, row, 3));
            row += 2;

            builder.add(deleteSystemSubFolderBox, cc.xyw(1, row, 3));
            row += 2;
        }

        // Don't need to show this if just online.
        // If not used, refactor actions
//        if (onlineFolder && localFolder) {
//            builder.add(removeFromServerBox, cc.xyw(1, row, 3));
//            row += 2;
//        }

        configureComponents();

        return builder.getPanel();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(removeButton, cancelButton);
    }

    protected JButton getDefaultButton() {
        return removeButton;
    }

    private void configureComponents() {

        if (!localFolder && !onlineFolder) {
            // Should never be.
            removeButton.setEnabled(false);
        }

        removeButton.setEnabled(removeFromLocalBox.isSelected()
            || removeFromServerBox.isSelected());

        if (!removeFromLocalBox.isSelected()) {
            deleteSystemSubFolderBox.setSelected(false);
        }
    }

    private void confirmedFolderLeave(final boolean removeLocal,
        boolean deleteSystemSubFolder, boolean removeFromOS)
    {

        // Dispose before closing parent frame (when folder is deleted),
        // otherwise parent closes and this is orphanned, and reappears next
        // time Info window displays.
        close();

        FolderRepository folderRepository = getController()
            .getFolderRepository();

        if (removeLocal) {
            Folder f = foInfo.getFolder(getController());
            if (f != null) {
                folderRepository.removeFolder(f, deleteSystemSubFolder);
            }
        }

        if (removeFromOS) {
            // If remove local means = total removal of folder, also remove
            // permissions.
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    ServerClient client = getController().getOSClient();
                    client.getFolderService().removeFolder(foInfo, true,
                        removeLocal);
                    return null;
                }
            };
            worker.execute();

            if (!removeLocal) {
                // TODO For what is that?
                FolderSettings folderSettings = FolderSettings.load(
                    getController(), folder.getConfigEntryId());
                folderRepository.saveFolderConfig(foInfo, folderSettings, true);
            }
        }
    }

    // ////////////////
    // Inner Classes //
    // ////////////////

    private class ConvertActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            configureComponents();
        }
    }
}