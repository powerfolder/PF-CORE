package de.dal33t.powerfolder.util.ui;

import java.io.File;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.disk.FolderSettings;
import static de.dal33t.powerfolder.disk.Folder.*;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.FolderCreatePanel;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
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
 * results of the folder creation process. Exceptions can be retrieved with
 * <code>getFolderException()</code>
 * <p>
 * It is highly recommended that you disable any buttons, that trigger this
 * worker before you actually start it. E.g. the "Ok" button in the folder join
 * dialog.
 */
public abstract class FolderCreateWorker extends ActivityVisualizationWorker {
    private final Logger LOG = Logger.getLogger(FolderCreatePanel.class);

    private Controller controller;
    private FolderInfo foInfo;
    private File localBase;
    private SyncProfile syncProfile;
    private boolean storeInvitation;
    private FolderException exception;
    private Folder folder;
    private boolean createShortcut;
    private boolean useRecycleBin;
    private boolean previewOnly;

    public FolderCreateWorker(Controller theController, FolderInfo aFoInfo,
        File aLocalBase, SyncProfile aProfile, boolean storeInv,
        boolean createShortcut, boolean useRecycleBin, boolean previewOnly)
    {
        super(theController.getUIController().getMainFrame().getUIComponent());
        Reject.ifNull(aFoInfo, "FolderInfo is null");
        Reject.ifNull(aLocalBase, "Folder local basedir is null");
        Reject.ifNull(aProfile, "Syncprofile is null");

        controller = theController;
        foInfo = aFoInfo;
        localBase = aLocalBase;
        syncProfile = aProfile;
        storeInvitation = storeInv;
        this.createShortcut = createShortcut;
        this.useRecycleBin = useRecycleBin;
        this.previewOnly = previewOnly;
    }

    /**
     * @return the folder exception if problem while folde creation occoured
     */
    protected FolderException getFolderException() {
        return exception;
    }

    /**
     * @return the freshly created folder.
     */
    protected Folder getFolder() {
        return folder;
    }

    // Overiding stuff ********************************************************

    @Override
    protected String getTitle()
    {
        return Translation.getTranslation("foldercreate.progress.text",
            foInfo.name);
    }

    @Override
    protected String getWorkingText()
    {
        return Translation.getTranslation("foldercreate.progress.text",
            foInfo.name);
    }

    @Override
    public Object construct()
    {
        try {
            FolderSettings folderSettings =
                    new FolderSettings(localBase, syncProfile, storeInvitation, true, previewOnly);
            folder = controller.getFolderRepository().createFolder(foInfo,
                    folderSettings);
            if (createShortcut) {
                folder.setDesktopShortcut(true);
            }
            if (OSUtil.isWindowsSystem()) {
                // Add thumbs to ignore pattern on windows systems
                // Don't duplicate thumbs (like when moving a preview folder)
                if (!folder.getBlacklist().getPatterns().contains(THUMBS_DB))
                {
                    folder.getBlacklist().addPattern(THUMBS_DB);
                }

                // Add desktop.ini to ignore pattern on windows systems
                if (ConfigurationEntry.USE_PF_ICON
                        .getValueBoolean(controller))
                {
                    folder.getBlacklist().addPattern(DESKTOP_INI_FILENAME);
                }

            }
            folder.setUseRecycleBin(useRecycleBin);
        } catch (FolderException ex) {
            exception = ex;
            LOG.error("Unable to create new folder " + foInfo, ex);
        }
        return null;
    }
}