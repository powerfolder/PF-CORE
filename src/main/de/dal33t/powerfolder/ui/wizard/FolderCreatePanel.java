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

import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SET_DEFAULT_SYNCHRONIZED_FOLDER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jwf.Wizard;
import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.SyncFolderPanel;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.ui.SwingWorker;

/**
 * A panel that actually starts the creation process of a folder on display.
 * Automatically switches to the next panel when succeeded otherwise prints
 * error.
 * <p>
 * Extracts the settings for the folder from the
 * <code>WizardContextAttributes</code>.
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class FolderCreatePanel extends PFWizardPanel {

    private FolderInfo foInfo;
    private boolean backupByOS;
    private boolean sendInvitations;
    private boolean createShortcut;
    private FolderSettings folderSettings;

    private Folder folder;

    private JLabel statusLabel;
    private JTextArea errorArea;
    private JComponent errorPane;
    private JProgressBar bar;

    public FolderCreatePanel(Controller controller) {
        super(controller);
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("pref, $lcg, $wfield",
            "pref, 5dlu, pref, 5dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;

        statusLabel = builder.addLabel(Translation
            .getTranslation("wizard.create_folder.working"), cc.xy(1, row));

        row += 2;
        bar = new JProgressBar();
        bar.setIndeterminate(true);
        builder.add(bar, cc.xy(1, row));

        errorArea = new JTextArea();
        errorArea.setRows(5);
        errorArea.setWrapStyleWord(true);
        errorPane = new JScrollPane(errorArea);
        builder.add(errorPane, cc.xy(1, row));
        return builder.getPanel();
    }

    @Override
    protected void afterDisplay() {

        // Mandatory
        File localBase = (File) getWizardContext().getAttribute(
            WizardContextAttributes.FOLDER_LOCAL_BASE);
        SyncProfile syncProfile = (SyncProfile) getWizardContext()
            .getAttribute(WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE);
        Boolean saveLocalInvite = (Boolean) getWizardContext().getAttribute(
            WizardContextAttributes.SAVE_INVITE_LOCALLY);
        Reject.ifNull(localBase, "Local base for folder is null/not set");
        Reject.ifNull(syncProfile, "Sync profile for folder is null/not set");
        Reject.ifNull(saveLocalInvite,
            "Save invite locally attribute is null/not set");

        // Optional
        foInfo = (FolderInfo) getWizardContext().getAttribute(
            WizardContextAttributes.FOLDERINFO_ATTRIBUTE);
        if (foInfo == null) {
            // Create new folder info
            String name = getController().getMySelf().getNick() + '-'
                + localBase.getName();
            String folderId = '[' + IdGenerator.makeId() + ']';
            foInfo = new FolderInfo(name, folderId);
            getWizardContext().setAttribute(
                WizardContextAttributes.FOLDERINFO_ATTRIBUTE, foInfo);
        }
        Boolean prevAtt = (Boolean) getWizardContext().getAttribute(
            WizardContextAttributes.PREVIEW_FOLDER_ATTIRBUTE);
        boolean previewFolder = prevAtt != null && prevAtt;
        boolean useRecycleBin = ConfigurationEntry.USE_RECYCLE_BIN
            .getValueBoolean(getController());
        createShortcut = (Boolean) getWizardContext().getAttribute(
            WizardContextAttributes.CREATE_DESKTOP_SHORTCUT);
        Boolean osAtt = (Boolean) getWizardContext().getAttribute(
            WizardContextAttributes.BACKUP_ONLINE_STOARGE);
        backupByOS = osAtt != null && osAtt;
        if (backupByOS) {
            getController().getUIController().getServerClientModel()
                .checkAndSetupAccount();
        }
        // Send invitation after by default.
        Boolean sendInvsAtt = (Boolean) getWizardContext().getAttribute(
            WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE);
        sendInvitations = sendInvsAtt == null || sendInvsAtt;

        folderSettings = new FolderSettings(localBase, syncProfile,
            saveLocalInvite, useRecycleBin, previewFolder, false);

        // Reset
        folder = null;

        SwingWorker worker = new MyFolderCreateWorker();
        bar.setVisible(true);
        errorPane.setVisible(false);
        worker.start();

        updateButtons();
    }

    protected void initComponents() {
    }

    protected Icon getPicto() {
        return getContextPicto();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.create_folder.title");
    }

    @Override
    public boolean hasNext() {
        return folder != null;
    }

    @Override
    public WizardPanel next() {
        WizardPanel next;
        if (sendInvitations) {
            next = new SendInvitationsPanel(getController(), true);
        } else {
            next = (WizardPanel) getWizardContext().getAttribute(
                PFWizard.SUCCESS_PANEL);
        }
        return next;
    }

    private class MyFolderCreateWorker extends SwingWorker {

        private boolean problems;

        @Override
        public Object construct() {
            folder = getController().getFolderRepository().createFolder(foInfo,
                folderSettings);
            if (createShortcut) {
                folder.setDesktopShortcut(true);
            }

            folder.addDefaultExcludes();
            ServerClient client = getController().getOSClient();
            if (backupByOS && client.isLastLoginOK()) {
                try {
                    // Try to back this up by online storage.
                    if (client.hasJoined(folder)) {
                        // Already have this os folder.
                        Loggable.logWarningStatic(FolderCreatePanel.class,
                            "Already have os folder " + foInfo.name);
                        return null;
                    }

                    client.getFolderService().createFolder(foInfo,
                        SyncProfile.BACKUP_TARGET_NO_CHANGE_DETECT);
                    client.refreshAccountDetails();

                    // Set as default synced folder?
                    Object attribute = getWizardContext().getAttribute(
                        SET_DEFAULT_SYNCHRONIZED_FOLDER);
                    if (attribute != null && (Boolean) attribute) {
                        // TODO: Ugly. Use abstraction: Runnable? Callback with
                        // folder? Which is placed on WizardContext.
                        client.getFolderService().setDefaultSynchronizedFolder(
                            foInfo);
                        client.refreshAccountDetails();
                        createDefaultFolderHelpFile();
                        folder.recommendScanOnNextMaintenance();
                        try {
                            FileUtils.openFile(folder.getLocalBase());
                        } catch (IOException e) {
                            Loggable.logFinerStatic(FolderCreatePanel.class, e);
                        }
                    }
                } catch (FolderException e) {
                    problems = true;
                    errorArea.setText(Translation
                        .getTranslation("foldercreate.dialog.backuperror.text")
                        + "\n" + e.getMessage());
                    errorPane.setVisible(true);
                    Loggable.logSevereStatic(FolderCreatePanel.class,
                        "Unable to backup folder to online storage", e);
                }
            }
            return null;
        }

        private void createDefaultFolderHelpFile() {
            File helpFile = new File(folder.getLocalBase(),
                "Place files to sync here.txt");
            if (helpFile.exists()) {
                return;
            }
            try {
                Writer w = new OutputStreamWriter(
                    new FileOutputStream(helpFile));
                w
                    .write("This is the default synchronized folder of PowerFolder.\r\n");
                w
                    .write("Simply place files into this directory to sync them\r\n");
                w.write("across all your computers running PowerFolder.\r\n");
                w.write("\r\n");
                w
                    .write("More information: http://wiki.powerfolder.com/wiki/Default_Folder");
                w.close();
            } catch (IOException e) {
                // Doesn't matter.
            }
        }

        @Override
        public void finished() {
            bar.setVisible(false);

            if (problems) {
                updateButtons();

                statusLabel.setText(Translation
                    .getTranslation("wizard.create_folder.failed"));
                errorPane.setVisible(true);
            } else {
                if (SyncProfile.MANUAL_SYNCHRONIZATION.equals(folder
                    .getSyncProfile()))
                {
                    // Show sync folder panel after created a project folder
                    new SyncFolderPanel(getController(), folder).open();
                }

                Wizard wiz = (Wizard) getWizardContext().getAttribute(
                    Wizard.WIZARD_ATTRIBUTE);
                wiz.next();
            }
        }
    }

}
