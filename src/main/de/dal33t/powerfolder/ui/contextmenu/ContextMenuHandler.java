/*
 * Copyright 2004 - 2014 Christian Sprajc. All rights reserved.
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.liferay.nativity.modules.contextmenu.ContextMenuControlCallback;
import com.liferay.nativity.modules.contextmenu.model.ContextMenuAction;
import com.liferay.nativity.modules.contextmenu.model.ContextMenuItem;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.util.Translation;

/**
 * Builds the Context Menu Items and applies the the correct
 * {@link ContextMenuAction}.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class ContextMenuHandler extends PFComponent implements
    ContextMenuControlCallback
{

    private ContextMenuItem shareLinkItem;
    private ContextMenuItem shareFolderItem;
    private ContextMenuItem openColabItem;

    private ContextMenuItem pfMainItem;
    private ContextMenuItem openWebItem;
    private ContextMenuItem stopSyncItem;
    private ContextMenuItem lockItem;
    private ContextMenuItem unlockItem;
    private ContextMenuItem versionHistoryItem;

    public ContextMenuHandler(Controller controller) {
        super(controller);

        shareLinkItem = new ContextMenuItem(
            Translation.getTranslation("context_menu.share_link"));
        shareLinkItem
            .setContextMenuAction(new ShareLinkAction(getController()));
        shareFolderItem = new ContextMenuItem(
            Translation.getTranslation("context_menu.share_folder"));
        shareFolderItem.setContextMenuAction(new ShareFolderAction(
            getController()));

        pfMainItem = new ContextMenuItem(
            Translation.getTranslation("context_menu.main_item"));

        openWebItem = new ContextMenuItem(
            Translation.getTranslation("context_menu.open_web"));
        openWebItem.setContextMenuAction(new OpenWebAction(getController()));
        stopSyncItem = new ContextMenuItem(
            Translation.getTranslation("context_menu.stop_sync"));
        stopSyncItem.setContextMenuAction(new StopSyncAction(getController()));
        lockItem = new ContextMenuItem(
            Translation.getTranslation("context_menu.lock"));
        lockItem.setContextMenuAction(new LockAction(getController()));
        unlockItem = new ContextMenuItem(
            Translation.getTranslation("context_menu.unlock"));
        unlockItem.setContextMenuAction(new UnlockAction(getController()));
    }

    @Override
    public List<ContextMenuItem> getContextMenuItems(String[] pathNames) {
        // Clear the context menu
        for (ContextMenuItem cmi : pfMainItem.getAllContextMenuItems()) {
            pfMainItem.removeContextMenuItem(cmi);
        }

        // Gather some information to decide which context menu items to show
        boolean containsFolderPath = false;
        boolean containsFileInfoPath = false;

        // Check for folder base paths
        FolderRepository fr = getController().getFolderRepository();
        for (String pathName : pathNames) {
            Path path = Paths.get(pathName);

            if (fr.findExistingFolder(path) != null) {
                containsFolderPath = true;
                break;
            }
        }

        // Check for FileInfos or DirectoryInfos, that are managed by a Folder
        for (Folder folder : fr.getFolders()) {
            for (String pathName : pathNames) {
                Path path = Paths.get(pathName);

                if (fr.findExistingFolder(path) != null
                    || !path.startsWith(folder.getLocalBase()))
                {
                    break;
                }

                FileInfo lookup = FileInfoFactory.lookupInstance(folder, path);

                if (folder.getDAO().find(lookup, null) != null) {
                    containsFileInfoPath = true;
                }
                if (containsFileInfoPath) {
                    break;
                }
            }
            if (containsFileInfoPath) {
                break;
            }
        }

        // Build the context menu - the order is from BOTTOM to TOP

        if (containsFolderPath || containsFileInfoPath) {
            pfMainItem.addContextMenuItem(unlockItem);
            pfMainItem.addContextMenuItem(lockItem);
        }

        if (containsFolderPath && !containsFileInfoPath) {
            pfMainItem.addContextMenuItem(stopSyncItem);
        }

        if (containsFileInfoPath) {
            pfMainItem.addContextMenuItem(shareLinkItem);
        }

        if ((containsFolderPath && !containsFileInfoPath)
            || getController().getOSClient().isAllowedToCreateFolders())
        {
            pfMainItem.addContextMenuItem(shareFolderItem);
        }

        if ((containsFileInfoPath
            || containsFolderPath)
            && ConfigurationEntry.WEB_LOGIN_ALLOWED
                .getValueBoolean(getController()))
        {
            pfMainItem.addContextMenuItem(openWebItem);
        }

        List<ContextMenuItem> items = new ArrayList<>(1);
        if (pfMainItem.getContextMenuItems().size() > 0) {
            items.add(pfMainItem);
        }

        return items;
    }
}
