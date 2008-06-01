package de.dal33t.powerfolder.util.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

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
        folder.addDefaultExcludes();
        return null;
    }
}