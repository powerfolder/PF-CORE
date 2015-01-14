/*
 * Copyright 2015 Christian Sprajc. All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.contextmenu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * Move an existing folder to another base dir.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class MoveExistingFolderAction extends PFContextMenuAction {

    private static final Logger log = Logger.getLogger(MoveExistingFolderAction.class.getName());

    private Folder folder;

    MoveExistingFolderAction(Controller controller) {
        super(controller);
    }

    MoveExistingFolderAction(Controller controller, Folder folder) {
        super(controller);
        this.folder = folder;
    }

    @Override
    public void onSelection(String[] paths) {
        if (paths.length != 1) {
            
        }
        try {
            
        } catch (RuntimeException re) {
            
        }
    }

    /**
     * Controls the movement of a folder directory.
     */
    public void moveLocalFolder() {

        // Lock out the 'new folder' scanner.
        // Else it's _just_ possible the scanner might see the renamed folder
        // and autocreate it during the file move.
        getController().getFolderRepository().setSuspendNewFolderSearch(true);

        try {

            Path originalDirectory = folder.getCommitOrLocalDir();

            // Select the new folder.
            List<Path> files = DialogFactory.chooseDirectory(getController()
                    .getUIController(), originalDirectory, false);
            if (!files.isEmpty()) {
                Path newDirectory = files.get(0);
                if (!folder.checkIfDeviceDisconnected() &&
                        PathUtils.isSubdirectory(originalDirectory, newDirectory)) {
                    // Can't move a folder to one of its subdirectories.
                    DialogFactory.genericDialog(getController(),
                            Translation.getTranslation("general.directory"),
                            Translation.getTranslation("general.subdirectory_error.text"),
                            GenericDialogType.ERROR);
                    return;
                }

                Path foldersBaseDir = getController().getFolderRepository().getFoldersBasedir();
                if (newDirectory.equals(foldersBaseDir)) {
                    // Can't move a folder to the base directory.
                    DialogFactory.genericDialog(getController(),
                            Translation.getTranslation("general.directory"),
                            Translation.getTranslation("general.basedir_error.text"),
                            GenericDialogType.ERROR);
                    return;
                }

                if (ConfigurationEntry.FOLDER_CREATE_IN_BASEDIR_ONLY
                    .getValueBoolean(getController()))
                {
                    if (!newDirectory.getParent().equals(
                        getController().getFolderRepository()
                            .getFoldersBasedir()))
                    {
                        // Can't move a folder outside the base directory.
                        DialogFactory.genericDialog(getController(),
                            Translation.getTranslation("general.directory"),
                            Translation.getTranslation(
                                "general.outside_basedir_error.text",
                                getController().getFolderRepository()
                                    .getFoldersBasedirString()),
                            GenericDialogType.ERROR);
                        return;
                    }
                }

                // Find out if the user wants to move the content of the
                // current folder
                // to the new one.
                int moveContent = shouldMoveContent();

                if (moveContent == 2) {
                    // Cancel
                    return;
                }

                moveDirectory(originalDirectory, newDirectory,
                        moveContent == 0);
            }
        } finally {
            try {
                // Unlock the 'new folder' scanner.
                getController().getFolderRepository()
                        .setSuspendNewFolderSearch(false);
            } catch (Exception e) {
                log.log(Level.SEVERE, "", e);
            }
        }
    }

    /**
     * Should the content of the existing folder be moved to the new location?
     *
     * @return true if should move.
     */
    private int shouldMoveContent() {
        return DialogFactory.genericDialog(
            getController(),
            Translation.getTranslation("settings_tab.move_content.title"),
            Translation.getTranslation("settings_tab.move_content"),
            new String[]{
                Translation.getTranslation("settings_tab.move_content.move"),
                Translation.getTranslation("settings_tab.move_content.dont"),
                Translation.getTranslation("general.cancel"),}, 0,
            GenericDialogType.INFO);
    }

    /**
     * Move the directory.
     */
    public void moveDirectory(Path originalDirectory, Path newDirectory,
        boolean moveContent)
    {
        if (!newDirectory.equals(originalDirectory)) {

            // Check for any problems with the new folder.
            if (checkNewLocalFolder(newDirectory)) {

                // Confirm move.
                if (shouldMoveLocal(newDirectory)) {
                    try {
                        // Move contentes selected
                        ActivityVisualizationWorker worker = new FolderMoveWorker(
                            moveContent, originalDirectory, newDirectory);
                        worker.start();
                    } catch (Exception e) {
                        // Probably failed to create temp directory.
                        DialogFactory
                            .genericDialog(
                                getController(),
                                Translation
                                    .getTranslation("settings_tab.move_error.title"),
                                Translation
                                    .getTranslation("settings_tab.move_error.temp"),
                                getController().isVerbose(), e);
                    }
                }
            }
        }
    }

    /**
     * Confirm that the user really does want to go ahead with the move.
     *
     * @param newDirectory
     * @return true if the user wishes to move.
     */
    private boolean shouldMoveLocal(Path newDirectory) {
        String title = Translation
            .getTranslation("settings_tab.confirm_local_folder_move.title");
        String message = Translation.getTranslation(
            "settings_tab.confirm_local_folder_move.text", folder
                .getCommitOrLocalDir().toAbsolutePath().toString(), newDirectory
                .toAbsolutePath().toString());

        return DialogFactory.genericDialog(getController(), title, message,
            new String[]{Translation.getTranslation("general.continue"),
                Translation.getTranslation("general.cancel")}, 0,
            GenericDialogType.INFO) == 0;
    }

    /**
     * Do some basic validation. Warn if moving to a folder that has files /
     * directories in it.
     *
     * @param newDirectory
     * @return
     */
    private boolean checkNewLocalFolder(Path newDirectory) {

        // Warn if target directory is not empty.
        if (newDirectory != null && Files.exists(newDirectory)
            && PathUtils.getNumberOfSiblings(newDirectory) > 0)
        {
            int result = DialogFactory.genericDialog(getController(),
                Translation
                    .getTranslation("exp.settings_tab.folder_not_empty.title"),
                Translation.getTranslation("exp.settings_tab.folder_not_empty",
                    newDirectory.toAbsolutePath().toString()),
                new String[]{Translation.getTranslation("general.continue"),
                    Translation.getTranslation("general.cancel")}, 1,
                GenericDialogType.WARN); // Default is cancel.
            if (result != 0) {
                // User does not want to move to new folder.
                return false;
            }
        }

        // All good.
        return true;
    }

    /**
     * Displays an error if the folder move failed.
     *
     * @param e
     *            the error
     */
    private void displayError(Exception e) {
        DialogFactory.genericDialog(
            getController(),
            Translation.getTranslation("settings_tab.move_error.title"),
            Translation.getTranslation("settings_tab.move_error.other",
                e.getMessage()), GenericDialogType.WARN);
    }

    /**
     * Moves the contents of a folder to another via a temporary directory.
     *
     * @param moveContent
     * @param originalDirectory
     * @param newDirectory
     * @return
     */
    private Object transferFolder(boolean moveContent, Path originalDirectory,
        Path newDirectory)
    {
        try {
            newDirectory = PathUtils.removeInvalidFilenameChars(newDirectory);

            // Copy the files to the new local base
            if (Files.notExists(newDirectory)) {
                try {
                    Files.createDirectories(newDirectory);
                } catch (IOException ioe) {
                    throw new IOException("Failed to create directory: "
                        + newDirectory + ". " + ioe);
                }
            }

            // Remove the old folder from the repository.
            FolderRepository repository = getController().getFolderRepository();
            repository.removeFolder(folder, false);

            // Move it.
            if (moveContent) {
                PathUtils.recursiveMove(originalDirectory, newDirectory);
            }

            Path commitDir = null;
            boolean hasCommitDir = folder.getCommitDir() != null;
            if (hasCommitDir) {
                commitDir = newDirectory;
                newDirectory = newDirectory.resolve(
                    Constants.ATOMIC_COMMIT_TEMP_TARGET_DIR);
                PathUtils.setAttributesOnWindows(newDirectory, true, true);
            }

            // Remember patterns if content not moving.
            List<String> patterns = null;
            if (!moveContent) {
                patterns = folder.getDiskItemFilter().getPatterns();
            }

            // Create the new Folder in the repository.
            FolderInfo fi = new FolderInfo(folder);
            FolderSettings fs = new FolderSettings(newDirectory,
                folder.getSyncProfile(), folder.getDownloadScript(), folder
                    .getFileArchiver().getVersionsPerFile(),
                folder.isSyncPatterns(), commitDir, folder.getSyncWarnSeconds());
            folder = repository.createFolder(fi, fs);

            // Restore patterns if content not moved.
            if (!moveContent && patterns != null) {
                for (String pattern : patterns) {
                    folder.addPattern(pattern);
                }
            }
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    /**
     * Visualisation worker for folder move.
     */
    private class FolderMoveWorker extends ActivityVisualizationWorker {

        private final boolean moveContent;
        private final Path originalDirectory;
        private final Path newDirectory;

        FolderMoveWorker(boolean moveContent,
            Path originalDirectory, Path newDirectory)
        {
            super(getController().getUIController());
            this.moveContent = moveContent;
            this.originalDirectory = originalDirectory;
            this.newDirectory = newDirectory;
        }

        @Override
        public Object construct() {
            return transferFolder(moveContent, originalDirectory, newDirectory);
        }

        @Override
        protected String getTitle() {
            return Translation.getTranslation("settings_tab.working.title");
        }

        @Override
        protected String getWorkingText() {
            return Translation
                .getTranslation("settings_tab.working.description");
        }

        @Override
        public void finished() {
            if (get() != null) {
                displayError((Exception) get());
            }
        }
    }
}
