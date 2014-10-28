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
import java.util.List;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Lock;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.Translation;

/**
 * View information about who locked a file, when was it locked and on which
 * device.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class LockInfoAction extends PFContextMenuAction {

    private static final Logger log = Logger.getLogger(LockInfoAction.class
        .getName());

    LockInfoAction(Controller controller) {
        super(controller);
    }

    @Override
    public void onSelection(String[] paths) {
        final List<FileInfo> files = getFileInfos(paths);

        if (files.size() != 1) {
            log.warning("More than one file was selected for locking information. Not showing any info.");
            return;
        }

        UIUtil.invokeLaterInEDT(new Runnable() {
            @Override
            public void run() {
                FileInfo file = files.get(0);
                Lock fileLock = file.getLock(getController());

                SimpleDateFormat format = new SimpleDateFormat(
                    "dd MMM yyyy HH:mm");

                String name = file.getFilenameOnly();
                String date = format.format(fileLock.getCreated());;
                String displayName = fileLock.getAccountInfo().getDisplayName();
                String memberName = Translation
                    .getTranslation("context_menu.lock_information.message.web");

                MemberInfo member = fileLock.getMemberInfo();
                if (member != null) {
                    memberName = member.getNick();
                }

                DialogFactory.genericDialog(getController(), Translation
                    .getTranslation("context_menu.lock_information.title"),
                    Translation.getTranslation(
                        "context_menu.lock_information.message", name, date,
                        displayName, memberName), new String[]{Translation
                        .getTranslation("general.ok")}, 0,
                    GenericDialogType.INFO);
            }
        });
    }
}
