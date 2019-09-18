/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: MetaDataHandler.java 12163 2010-04-24 02:06:55Z harry $
 */
package de.dal33t.powerfolder.transfer;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.Lock;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.nio.file.Path;

/**
 * This class delegates responsibility for processing metaFolder fileInfo
 * 'events' to relevant classes.
 */
public class MetaFolderDataHandler extends PFComponent {

    public MetaFolderDataHandler(Controller controller) {
        super(controller);
    }

    /**
     * Handle file change to metaFolder files.
     * 
     * @param fileInfo
     *            metaFolder file info
     */
    public void handleMetaFolderFileInfo(FileInfo fileInfo) {
        if (!fileInfo.getFolderInfo().isMetaFolder()) {
            logSevere("Unable to handle meta data file. Not in meta folder: "
                + fileInfo.toDetailString());
            return;
        }
        Folder parentFolder = getController().getFolderRepository()
            .getParentFolder(fileInfo.getFolderInfo());
        if (parentFolder == null) {
            logSevere("Parent folder for meta folder not found: "
                + fileInfo.getFolderInfo() + '/' + fileInfo.getFolderInfo().id);
            return;
        }
        if (fileInfo.getRelativeName().endsWith(
            DiskItemFilter.PATTERNS_FILENAME))
        {
            parentFolder.handleMetaFolderSyncPatterns(fileInfo);
        }
        if (fileInfo.getRelativeName().equals(Folder.METAFOLDER_MEMBERS)) {
            parentFolder.updateMetaFolderMembers();
        }
        if (fileInfo.getRelativeName().startsWith(Folder.METAFOLDER_LOCKS_DIR))
        {
            getController().getFolderRepository().getLocking()
                .lockStateChanged(fileInfo);
        }
    }

    /**
     * Check {@code fileInfo} to be a remote lock deletion and inform the user
     * about it.
     *
     * @param fileInfo
     * @param file
     */
    public void handleRemoteLockOverwrite(final FileInfo fileInfo, Path file) {
        if (getController().getMySelf().isServer()) {
            return;
        }

        if (fileInfo == null) {
            return;
        }

        Lock lock = getController().getFolderRepository().getLocking()
                .getLock(file);

        if (lock == null) {
            return;
        }

        final MemberInfo lockMember = lock.getMemberInfo();
        final MemberInfo remoteMember = fileInfo.getModifiedBy();

        if (lockMember == null || remoteMember == null || !lockMember.getNode(getController(), true).isMySelf()) {
            return;
        }

        if (!lockMember.equals(remoteMember)) {
            AccountInfo accountInfo = remoteMember.getNode(getController(), true).getAccountInfo();
            String overrideBy = accountInfo != null ? accountInfo.getDisplayName() : remoteMember.nick;
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    logWarning("Overwriting lock for " + fileInfo + " set by " +
                            lockMember.getNick() + " removed by " +
                            remoteMember.getNick());

                    DialogFactory
                            .genericDialog(
                                    getController(),
                                    Translation.get("dialog.lock.removed_by_other_member.title"),
                                    Translation.get("dialog.lock.removed_by_other_member.message",
                                            fileInfo.getFilenameOnly(), overrideBy),
                                    new String[]{"OK"},
                                    0, GenericDialogType.WARN);

                    return null;
                }
            };
            worker.execute();
        }
    }
}
