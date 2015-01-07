/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.ui;

import java.nio.file.Path;

import com.liferay.nativity.modules.fileicon.FileIconControl;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.FolderScannerListener;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.iconoverlay.IconOverlayHandler;

/**
 * Adds an overlay icon to the file described in the passed FileInfo.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
class IconOverlayApplier extends PFComponent implements FolderScannerListener {

    private FileIconControl iconControl;
    private IconOverlayHandler iconHandler;

    IconOverlayApplier(Controller controller, FileIconControl iconControl) {
        super(controller);
        this.iconControl = iconControl;
        iconHandler = new IconOverlayHandler(getController());
    }

    @Override
    public void notify(FileInfo fInfo) {
        Path file = fInfo.getDiskFile(getController().getFolderRepository());

        if (file == null) {
            return;
        }

        String fileName = file.toAbsolutePath().toString();
        int index = iconHandler.getIconForFile(fileName);

        if (index == 0) {
            // yes, on Windows "0" means "No Icon". On Mac it is "-1". m(
            index = -1;
        }

        iconControl.setFileIcon(fileName, index);
//        iconControl.refreshIcons();
    }
}
