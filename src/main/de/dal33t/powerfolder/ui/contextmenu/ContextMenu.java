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
import java.util.logging.Logger;

import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.control.NativityControlUtil;
import com.liferay.nativity.modules.contextmenu.ContextMenuControlUtil;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Initialize the Context Menu and load the libraries needed.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class ContextMenu {

    private static final Logger log = Logger.getLogger(ContextMenu.class
        .getName());

    public ContextMenu(Controller controller) {
        NativityControl nc = NativityControlUtil.getNativityControl();
        ContextMenuControlUtil.getContextMenuControl(nc,
            new ContextMenuHandler(controller));

        StringBuffer sb = new StringBuffer();
        for (Path root : Paths.get(".").getFileSystem().getRootDirectories()) {
            sb.append(root.toString());
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);

        nc.setFilterFolders(sb.toString().split(","));
        if (!nc.connect()) {
            log.fine("Could not initialize for context menu!");
            nc.disconnect();
            return;
        }

        loadLibs();
    }

    private void loadLibs() {
        String libNameUtil = "LiferayNativityUtil";
        String libNameContextMenu = "LiferayNativityContextMenus";

        if (OSUtil.isWindowsSystem()) {
            if (OSUtil.is64BitPlatform()) {
                libNameUtil += "_x64";
                libNameContextMenu += "_x64";
            } else {
                libNameUtil += "_x86";
                libNameContextMenu += "_x86";
            }
        }

        OSUtil.loadLibrary(ContextMenu.class, libNameUtil);
        OSUtil.loadLibrary(ContextMenu.class, libNameContextMenu);
    }
}
