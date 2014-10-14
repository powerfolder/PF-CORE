package de.dal33t.powerfolder.ui.contextmenu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jwf.WizardPanel;

import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.task.CreateFolderOnServerTask;
import de.dal33t.powerfolder.ui.wizard.ChooseDiskLocationPanel;
import de.dal33t.powerfolder.ui.wizard.FolderCreatePanel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.TextPanelPanel;
import de.dal33t.powerfolder.ui.wizard.WizardContextAttributes;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Translation;

class ShareFolderAction extends ContextMenuAction {

    private Controller controller;
    private FolderRepository repository;

    ShareFolderAction(Controller controller) {
        this.controller = controller;
        this.repository = controller.getFolderRepository();
    }

    @Override
    public void onSelection(String[] paths) {
        for (String pathName : paths) {
            Path path = Paths.get(pathName);

            if (!Files.isDirectory(path)) {
                continue;
            }

            Folder folder = repository.findExistingFolder(path);
            FolderInfo foInfo;
            String name = null;

            if (folder != null) {
                foInfo = folder.getInfo();
                name = foInfo.getName();
            } else {
                String id = IdGenerator.makeFolderId();
                name = PathUtils.getSuggestedFolderName(path);

                foInfo = new FolderInfo(name, id);
            }

            SyncProfile syncProfile = SyncProfile.getDefault(controller);
            boolean backupByServer = controller.getOSClient()
                .isBackupByDefault();

            if (controller.isUIEnabled()
                && !PreferencesEntry.BEGINNER_MODE.getValueBoolean(controller))
            {
                PFWizard wizard = new PFWizard(controller,
                    Translation.getTranslation("wizard.pfwizard.folder_title"));
                wizard.getWizardContext().setAttribute(
                    WizardContextAttributes.INITIAL_FOLDER_NAME, name);
                wizard.getWizardContext()
                    .setAttribute(
                        WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE,
                        syncProfile);
                wizard.getWizardContext().setAttribute(
                    WizardContextAttributes.BACKUP_ONLINE_STOARGE,
                    backupByServer);
                wizard.getWizardContext().setAttribute(
                    WizardContextAttributes.FOLDERINFO_ATTRIBUTE, foInfo);

                WizardPanel nextPanel = new FolderCreatePanel(controller);
                // Setup success panel of this wizard path
                TextPanelPanel successPanel = new TextPanelPanel(controller,
                    Translation.getTranslation("wizard.setup_success"),
                    Translation.getTranslation("wizard.success_join"));
                wizard.getWizardContext().setAttribute(PFWizard.SUCCESS_PANEL,
                    successPanel);
                ChooseDiskLocationPanel panel = new ChooseDiskLocationPanel(
                    controller, path.toAbsolutePath().toString(), nextPanel);
                wizard.open(panel);
            } else if (controller.isUIEnabled() && PreferencesEntry.BEGINNER_MODE.getValueBoolean(controller)) {
                
            } else {
                FolderSettings settings = new FolderSettings(path, syncProfile,
                    null,
                    ConfigurationEntry.DEFAULT_ARCHIVE_VERSIONS
                        .getValueInt(controller), true);
                repository.createFolder(foInfo, settings);
                if (backupByServer) {
                    new CreateFolderOnServerTask(foInfo, null)
                        .scheduleTask(controller);
                }
            }
        }
    }

}
