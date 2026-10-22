/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.transfer;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.Lock;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Date;

/**
 * This class delegates responsibility for processing metaFolder fileInfo
 * 'events' to relevant classes.
 */
public class MetaFolderDataHandler extends PFComponent {

    /** PFS-5842: how long after a lock was removed its former holder is still offered to take it again. */
    private static final long RELOCK_OFFER_MAX_AGE = 24L * 60 * 60 * 1000;

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
            .getContentFolder(fileInfo.getFolderInfo());
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
            Member remoteNode = remoteMember.getNode(getController(), true);
            AccountInfo accountInfo = fileInfo.getModifiedByAccount();
            String overrideBy = accountInfo != null ? accountInfo.getDisplayName() : null;
            if (StringUtils.isBlank(overrideBy) && remoteNode != null && remoteNode.isServer()) {
                overrideBy = Translation.get("general.server");
            }
            if (StringUtils.isBlank(overrideBy)) {
                overrideBy = remoteMember.nick;
            }
            if (StringUtils.isBlank(overrideBy)) {
                overrideBy = Translation.get("estimation.unknown");
            }
            final String overrideByFinal = overrideBy;
            // PFS-5842: the deleted lock file names who removed the lock and when (Locking#unlock records the
            // remover); the lock itself names the content file and so its folder.
            final FileInfo contentFInfo = lock.getFileInfo();
            final String folderName = contentFInfo != null
                ? contentFInfo.getFolderInfo().getLocalizedName() : "";
            final Date removedAt = fileInfo.getModifiedDate();
            final String removedAtText = removedAt != null
                ? Format.formatDateShort(removedAt) : Translation.get("estimation.unknown");
            // A removal that is old news by now - typically seen after a long time offline - is told, not undone:
            // somebody may well have been working on the file since.
            final boolean offerRelock = contentFInfo != null && removedAt != null
                && System.currentTimeMillis() - removedAt.getTime() < RELOCK_OFFER_MAX_AGE;
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    logInfo("Lock for file " + fileInfo.toDetailString() + " of " +
                            lockMember.getNick() + " removed by " + overrideByFinal);

                    String[] options = offerRelock
                            ? new String[]{
                                Translation.get("dialog.lock.removed_by_other_member.lock_again"),
                                Translation.get("general.ok")}
                            : new String[]{Translation.get("general.ok")};
                    int choice = DialogFactory.genericDialog(
                            getController(),
                            Translation.get("dialog.lock.removed_by_other_member.title"),
                            Translation.get("dialog.lock.removed_by_other_member.message",
                                    fileInfo.getFilenameOnly(), overrideByFinal, folderName, removedAtText),
                            options,
                            options.length - 1, GenericDialogType.WARN);
                    if (offerRelock && choice == 0) {
                        relock(contentFInfo, fileInfo.getFilenameOnly());
                    }
                    return null;
                }
            };
            worker.execute();
        }
    }

    /**
     * PFS-5842: takes the lock again - unless somebody has taken it since the removal. Decided at the click,
     * not when the dialog opened: it may have stood open for hours, and Locking#lock overwrites any lock.
     */
    private void relock(FileInfo contentFInfo, String fileName) {
        Lock current = contentFInfo.getLock(getController());
        boolean takenByOther = current != null ? !isMine(current) : contentFInfo.isLocked(getController());
        if (takenByOther || !contentFInfo.lock(getController())) {
            String holder = current != null && StringUtils.isNotBlank(current.getAccountDisplayName())
                ? current.getAccountDisplayName() : Translation.get("estimation.unknown");
            DialogFactory.genericDialog(getController(),
                Translation.get("dialog.lock.relock_failed.title"),
                Translation.get("dialog.lock.relock_failed.message", fileName, holder),
                new String[]{Translation.get("general.ok")}, 0, GenericDialogType.WARN);
        }
    }

    /** Whether {@code lock} was taken by this very device - the old lock, if its file is not gone yet. */
    private boolean isMine(Lock lock) {
        return lock.getMemberInfo() != null && lock.getMemberInfo().equals(getController().getMySelf().getInfo());
    }
}
