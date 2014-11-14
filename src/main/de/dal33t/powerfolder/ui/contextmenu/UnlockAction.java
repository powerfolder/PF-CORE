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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Lock;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Translation;

/**
 * Set file to be "NOT in use for edit" to display a message to the user.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class UnlockAction extends PFContextMenuAction {

    private static final Logger log = Logger.getLogger(UnlockAction.class
        .getName());

    UnlockAction(Controller controller) {
        super(controller);
    }

    @Override
    public void onSelection(String[] paths) {
        List<FileInfo> fileInfos = getFileInfos(paths);

        for (FileInfo fileInfo : fileInfos) {
            unlockFileInfo(fileInfo);
        }
    }

    private void unlockFileInfo(FileInfo fileInfo) {
        if (fileInfo.isDiretory()) {
            DirectoryInfo dInfo = (DirectoryInfo) fileInfo;

            FileInfoCriteria criteria = new FileInfoCriteria();
            criteria.addMySelf(fileInfo.getFolder(getController()
                .getFolderRepository()));
            criteria.setPath(dInfo);
            criteria.setRecursive(true);

            Collection<FileInfo> infos = dInfo
                .getFolder(getController().getFolderRepository()).getDAO()
                .findFiles(criteria);

            for (FileInfo info : infos) {
                unlockFileInfo(info);
            }
        }

        unlock(fileInfo);
    }

    private void unlock(FileInfo fileInfo) {
        if (fileInfo.isLocked(getController())) {
            Lock lock = fileInfo.getLock(getController());
            boolean bySameDevice = lock.getMemberInfo().equals(getController()
                .getMySelf().getInfo());
            boolean bySameAccount = lock.getAccountInfo().equals(getController()
                .getOSClient().getAccountInfo());

            if (bySameDevice && bySameAccount) {
                unlock0(fileInfo);
            } else {
                UIUtil.invokeLaterInEDT(new UnlockForeignTask(getController(),
                    fileInfo, lock));
            }
        }
    }

    private void unlock0(FileInfo fileInfo) {
        if (!fileInfo.unlock(getController())) {
            log.warning("File " + fileInfo + " could not be unlocked!");
        }
    }

    private class UnlockForeignTask implements Runnable {

        private final Controller controller;
        private final FileInfo fileInfo;
        private final Lock lock;

        UnlockForeignTask(Controller controller, FileInfo fileInfo, Lock lock) {
            this.controller = controller;
            this.fileInfo = fileInfo;
            this.lock = lock;
        }

        @Override
        public void run() {
            String name = fileInfo.getFilenameOnly();
            String displayName = lock.getAccountInfo().getDisplayName();
            String date = new SimpleDateFormat("dd MMM yyyy HH:mm").format(lock
                .getCreated());
            String memberName = Translation
                .getTranslation("context_menu.unlock.message.web");
            MemberInfo member = lock.getMemberInfo();
            if (member != null) {
                memberName = member.getNick();
            }

            int res = DialogFactory
                .genericDialog(
                    controller,
                    Translation.getTranslation("context_menu.unlock.title"),
                    Translation.getTranslation("context_menu.unlock.message",
                        name, displayName, date, memberName),
                    new String[]{
                        Translation
                            .getTranslation("context_menu.unlock.unlock"),
                        Translation
                            .getTranslation("context_menu.unlock.keep_lock")},
                    1, GenericDialogType.QUESTION);

            if (res == 0) {
                unlock0(fileInfo);
            }
        }
    }
}
