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
package de.dal33t.powerfolder.ui.iconoverlay;

import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.modules.fileicon.FileIconControl;
import com.liferay.nativity.modules.fileicon.FileIconControlUtil;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Initialize the Icon Overlay and load the libraries needed.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class IconOverlay {

    public IconOverlay(Controller controller, NativityControl nc) {
        FileIconControl iconControl = FileIconControlUtil.getFileIconControl(nc,
            new IconOverlayHandler(controller));
        iconControl.enableFileIcons();

        loadLibs();
    }

    private void loadLibs() {
        String libOKOverlay = "OKOverlay";
        String libSyncingOverlay = "SyncingOverlay";
        String libWarningOverlay = "WarningOverlay";
        String libIgnoredOverlay = "IgnoredOverlay";
        String libLockedOverlay = "LockedOverlay";

        if (OSUtil.isWindowsSystem()) {
            if (OSUtil.is64BitPlatform()) {
                libOKOverlay += "_x64";
                libSyncingOverlay += "_x64";
                libWarningOverlay += "_x64";
                libIgnoredOverlay += "_x64";
                libLockedOverlay += "_x64";
            } else {
                libOKOverlay += "_x86";
                libSyncingOverlay += "_x86";
                libWarningOverlay += "_x86";
                libIgnoredOverlay += "_x86";
                libLockedOverlay += "_x86";
            }
        }

        OSUtil.loadLibrary(IconOverlay.class, libOKOverlay);
        OSUtil.loadLibrary(IconOverlay.class, libSyncingOverlay);
        OSUtil.loadLibrary(IconOverlay.class, libWarningOverlay);
        OSUtil.loadLibrary(IconOverlay.class, libIgnoredOverlay);
        OSUtil.loadLibrary(IconOverlay.class, libLockedOverlay);
    }
}
