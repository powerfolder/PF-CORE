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
package de.dal33t.powerfolder.util;

import javax.swing.JOptionPane;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

/**
 * A Thread that can be manually invoked to check for updates to PowerFolder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a> *
 * @version $Revision: 1.3 $
 */
public class ManuallyInvokedUpdateChecker extends UpdateChecker {

    public ManuallyInvokedUpdateChecker(Controller controller,
        UpdateSetting settings)
    {
        super(controller, settings);
    }

    /**
     * Notifies user that no update is available
     */
    @Override
    protected void notifyNoUpdateAvailable() {
        if (controller.isUIEnabled()) {
            DialogFactory.genericDialog(
                    getParentFrame(),
                    Translation.getTranslation("dialog.updatecheck.noUpdateAvailable"),
                    Translation.getTranslation("dialog.updatecheck.noUpdateAvailable"),
                    GenericDialogType.INFO);
        }
    }

    protected boolean shouldCheckForNewerVersion() {
        return true;
    }
}