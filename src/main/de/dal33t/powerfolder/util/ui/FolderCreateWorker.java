package de.dal33t.powerfolder.util.ui;

import static de.dal33t.powerfolder.disk.Folder.THUMBS_DB;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Worker that helps to create a folder in the UI environment.
 * <p>
 * It prevents whitescreens/UI freezes when creating a folder, since the actual
 * creation is executed in a background thread.
 * <p>
 * Will display activity visualisation when the creation process is taking
 * longer.
 * <p>
 * Basically you only need to override <code>finish()</code> to react on
 * results of the folder creation process. 
 * <p>
 * It is highly recommended that you disable any buttons, that trigger this
 * worker before you actually start it. E.g. the "Ok" button in the folder join
 * dialog.
 */
public abstract class FolderCreateWorker extends ActivityVisualizationWorker {
    private static final Logger LOG = Logger
        .getLogger(FolderCreateWorker.class);

    private Controller controller;
    private FolderInfo foInfo;
    private FolderSettings folderSettings;
    private Folder folder;
    private boolean createShortcut;

    public FolderCreateWorker(Controller theController, FolderInfo folderInfo,
        FolderSettings folderSettings, boolean createShortcut)
    {
        super(theController.getUIController().getMainFrame().getUIComponent());
        Reject.ifNull(folderInfo, "FolderInfo is null");
        Reject.ifNull(folderSettings, "FolderSettings is null");
        Reject.ifNull(folderSettings.getLocalBaseDir(),
            "Folder local basedir is null");
        Reject.ifNull(folderSettings.getSyncProfile(), "Syncprofile is null");
        this.controller = theController;
        this.foInfo = folderInfo;
        this.folderSettings = folderSettings;
        this.createShortcut = createShortcut;
    }

    /**
     * @return the freshly created folder.
     */
    protected Folder getFolder() {
        return folder;
    }

    // Overiding stuff ********************************************************

    @Override
    protected String getTitle() {
        return Translation.getTranslation("folder_create.progress.text",
            foInfo.name);
    }

    @Override
    protected String getWorkingText() {
        return Translation.getTranslation("folder_create.progress.text",
            foInfo.name);
    }

    @Override
    public Object construct() {
        folder = controller.getFolderRepository().createFolder(foInfo,
            folderSettings);
        if (createShortcut) {
            folder.setDesktopShortcut(true);
        }
        if (OSUtil.isWindowsSystem()) {
            // Add thumbs to ignore pattern on windows systems
            // Don't duplicate thumbs (like when moving a preview folder)
            if (!folder.getDiskItemFilter().getPatterns().contains(THUMBS_DB)) {
                folder.getDiskItemFilter().addPattern(THUMBS_DB);
            }

            // Add desktop.ini to ignore pattern on windows systems
            if (!OSUtil.isWindowsVistaSystem()
                && ConfigurationEntry.USE_PF_ICON
                    .getValueBoolean(controller))
            {
                folder.getDiskItemFilter().addPattern(FileUtils.DESKTOP_INI_FILENAME);
            }
        }
        return null;
    }
}