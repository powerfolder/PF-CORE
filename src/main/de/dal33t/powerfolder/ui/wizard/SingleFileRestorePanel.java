/*
 * Copyright 2004 - 2012 Christian Sprajc. All rights reserved.
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
 * $Id: SingleFileRestorePanel.java 19700 2012-09-01 04:48:56Z glasgow $
 */
package de.dal33t.powerfolder.ui.wizard;

import jwf.WizardPanel;

import javax.swing.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.information.folder.files.versions.FileInfoVersionTypeHolder;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * Call this class via PFWizard.
 */
public class SingleFileRestorePanel extends PFWizardPanel {

    public SingleFileRestorePanel(Controller controller, Folder folder, FileInfo fileInfoToRestore,
                                  FileInfoVersionTypeHolder selectedInfo) {
        super(controller);
    }

    public SingleFileRestorePanel(Controller controller, Folder folder, FileInfo fileInfoToRestore) {
        this(controller, folder, fileInfoToRestore, null);
    }

    protected JComponent buildContent() {
        return null;
    }

    protected String getTitle() {
        return null;
    }

    protected void initComponents() {
    }

    public boolean hasNext() {
        return false;
    }

    public WizardPanel next() {
        return null;
    }
}
