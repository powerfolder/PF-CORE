package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.Reject;

import java.io.File;

/**
 * Utility class with methods for creating and converting preview folders.
 */
public class FolderPreviewHelper {

    /**
     * The System property name for the tmp dir
     */
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

    /**
     * The powerfolder directory that preview folders are put in.
     */
    private static final String DOT_POWER_FOLDER = ".PowerFolder";

    /**
     * This helper creates FolderSettings for creation of a preview folder.
     * The local base dir is in the system temp dir and there is no
     * synchronization for a preview folder.
     *
     * @param folderName
     * @return
     */
    public static FolderSettings createPreviewFolderSettings(String folderName) {

        File localBase = makePreviewBaseDir(folderName);
        return new FolderSettings(localBase, SyncProfile.NO_SYNC, false, false,
                true, false);
    }

    // Creates a preview folder directory for a folderName.
    // The folder is put in [java.io.tempdir]/.PowerFolder/folderName/
    private static File makePreviewBaseDir(String folderName) {

        String javaTempDir = System.getProperty(JAVA_IO_TMPDIR);
        File tempPF = new File(javaTempDir, DOT_POWER_FOLDER);
        return new File(tempPF, folderName);
    }

    /**
     * Converts a normal folder to a preview folder.
     *
     * @param controller
     * @param folder
     * @param deleteSystemSubDir
     */
    public static void convertFolderToPreview(Controller controller,
                                              Folder folder,
                                              boolean deleteSystemSubDir) {

        Reject.ifTrue(folder.isPreviewOnly(),
                "Can not convert a preview folder to preview");

        FolderRepository folderRepository = controller.getFolderRepository();

        FolderSettings initialFolderSettings = folderRepository
                .loadFolderSettings(folder.getName());

        FolderSettings previewFolderSettings =
                createPreviewFolderSettings(folder.getName());
        FolderInfo folderInfo = new FolderInfo(folder);

        // Saved FolderSettings are like initial, but preview is true.
        FolderSettings savedFolderSettings = new FolderSettings(
                initialFolderSettings.getLocalBaseDir(),
                initialFolderSettings.getSyncProfile(),
                initialFolderSettings.isCreateInvitationFile(),
                initialFolderSettings.isUseRecycleBin(),
                true,
                initialFolderSettings.isWhitelist());

        try {
            folderRepository.removeFolder(folder, deleteSystemSubDir);
            folderRepository.createPreviewFolder(folderInfo,
                    previewFolderSettings);
            folderRepository.saveFolderConfig(folderInfo, 
                    savedFolderSettings, true);

        } catch (FolderException e) {
            e.show(controller);
        }
    }

    /**
     * Converts a preview folder to a normal folder.
     *
     * @param controller
     * @param folder
     * @param deleteSystemSubDir
     */
    public static void convertFolderFromPreview(Controller controller,
                                         Folder folder,
                                         FolderSettings newFolderSettings,
                                         boolean deleteSystemSubDir) {

        Reject.ifTrue(!folder.isPreviewOnly(),
                "Can not convert a non-preview folder to non-previrew");
        Reject.ifTrue(newFolderSettings.isPreviewOnly(),
                "Can not convert to a preview folder");


        FolderRepository folderRepository = controller.getFolderRepository();

        FolderInfo folderInfo = new FolderInfo(folder);

        folderRepository.removeFolder(folder, deleteSystemSubDir);

        try {
            folderRepository.createFolder(folderInfo, newFolderSettings);
        } catch (FolderException e) {
            e.show(controller);
        }
    }
}
