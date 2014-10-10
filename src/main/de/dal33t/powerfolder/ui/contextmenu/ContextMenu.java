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

import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.modules.contextmenu.ContextMenuControlUtil;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Initialize the Context Menu and load the libraries needed.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class ContextMenu {

    public ContextMenu(Controller controller, NativityControl nc) {
        ContextMenuControlUtil.getContextMenuControl(nc,
            new ContextMenuHandler(controller));

        /*
         *  PFC-2395 first introduced, then removed:
         *  The Registry Entry which should be set by #setFilterFolders(String[])
         *  is not set. So the Installer will handle this.
         */
        StringBuffer sb = new StringBuffer();
        for (Path root : Paths.get(".").getFileSystem().getRootDirectories()) {
            sb.append(root.toString());
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);

        nc.setFilterFolders(sb.toString().split(","));
        // END: PFC-2395

        loadLibs();
    }

    private void loadLibs() {
//        String libNameUtil = "LiferayNativityUtil";
        String libNameContextMenu = "LiferayNativityContextMenus";

        if (OSUtil.isWindowsSystem()) {
            if (OSUtil.is64BitPlatform()) {
//                libNameUtil += "_x64";
                libNameContextMenu += "_x64";
            } else {
//                libNameUtil += "_x86";
                libNameContextMenu += "_x86";
            }
        }

//        OSUtil.loadLibrary(ContextMenu.class, libNameUtil);
        OSUtil.loadLibrary(ContextMenu.class, libNameContextMenu);
    }
}
