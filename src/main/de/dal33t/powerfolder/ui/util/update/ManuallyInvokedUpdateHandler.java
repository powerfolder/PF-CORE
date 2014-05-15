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
 * $Id: ManuallyInvokedUpdateHandler.java -1   $
 */
package de.dal33t.powerfolder.ui.util.update;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.util.update.UpdaterEvent;

/**
 * A Thread that can be manually invoked to check for updates to PowerFolder
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a> *
 * @version $Revision: 1.3 $
 */
public class ManuallyInvokedUpdateHandler extends UIUpdateHandler {

    public ManuallyInvokedUpdateHandler(Controller controller) {
        super(controller);
    }

    @Override
    public void noNewReleaseAvailable(UpdaterEvent event) {
        UIUtil.invokeLaterInEDT(new Runnable() {
            public void run() {
                DialogFactory.genericDialog(getController(), Translation
                    .getTranslation("general.application.name"), Translation
                    .getTranslation("dialog.update_check.noUpdateAvailable"),
                    GenericDialogType.INFO);
            }
        });
    }

    @Override
    public boolean shouldCheckForNewVersion() {
        return true;
    }

}