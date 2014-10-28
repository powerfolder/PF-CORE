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

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.SyncStatus;
import de.dal33t.powerfolder.disk.Lock;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Translation;

/**
 * Open the selected file with the standard application of the OS and lock it.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class OpenColaborateAction extends PFContextMenuAction {

    private static final Logger log = Logger
        .getLogger(OpenColaborateAction.class.getName());

    OpenColaborateAction(Controller controller) {
        super(controller);
    }

    @Override
    public void onSelection(String[] paths) {
        if (!Desktop.isDesktopSupported()) {
            log.info("Won't be able to open file. Unsupported operation required.");
            return;
        }

        List<FileInfo> files = getFileInfos(paths);

        for (final FileInfo file : files) {
            if (SyncStatus.of(getController(), file) == SyncStatus.IGNORED) {
                continue;
            }

            Lock lock = file.getLock(getController());
            boolean bySameDevice = lock.getMemberInfo().equals(getController()
                .getMySelf().getInfo());
            boolean bySameAccount = lock.getAccountInfo().equals(getController()
                .getOSClient().getAccountInfo());

            if (!(bySameDevice && bySameAccount)) {
                UIUtil.invokeLaterInEDT(new UnlockForeignTask(getController(),
                    file, lock));
            } else {
                open(file);
            }
        }
    }

    private void open(final FileInfo file) {
        UIUtil.invokeLaterInEDT(new Runnable() {
            @Override
            public void run() {
                try {
                    Path path = file.getDiskFile(getController()
                        .getFolderRepository());
                    Desktop.getDesktop().open(path.toFile());
                    file.lock(getController());
                } catch (IOException ioe) {
                    log.warning("Could not open file " + file
                        + " for editing. " + ioe);
                }
            }
        });
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

            DialogFactory.genericDialog(controller, Translation
                .getTranslation("context_menu.open_and_colaborate.title"),
                Translation.getTranslation("context_menu.open_and_colaborate.message", name,
                    displayName, date, memberName), new String[]{Translation
                    .getTranslation("general.ok")}, 0, GenericDialogType.INFO);
        }
    }
}
