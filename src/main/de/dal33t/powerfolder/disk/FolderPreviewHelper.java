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
     */
    public static boolean convertFolderToPreview(Controller controller,
                                              Folder folder) {

        Reject.ifTrue(folder.isPreviewOnly(),
                "Can not convert a preview folder to preview");

        FolderRepository folderRepository = controller.getFolderRepository();

        FolderSettings initialFolderSettings = folderRepository
                .loadV3FolderSettings(folder.getName());

        if (initialFolderSettings == null) {
            return false;
        }

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

        folderRepository.removeFolder(folder, false);
        folderRepository.createPreviewFolder(folderInfo,
                previewFolderSettings);
        folderRepository.saveFolderConfig(folderInfo,
                savedFolderSettings, true);

        return true;
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

        folderRepository.createFolder(folderInfo, newFolderSettings);
    }
}
