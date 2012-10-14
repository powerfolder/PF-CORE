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
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.information.folder.files.versions.FileInfoVersionTypeHolder;
import de.dal33t.powerfolder.light.FileInfo;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

/**
 * Call this class via PFWizard.
 */
public class SingleFileRestorePanel extends PFWizardPanel {

    private final Folder folder;
    private final FileInfo fileInfoToRestore;
    private final FileInfoVersionTypeHolder selectedInfo; // Note - this may be null.

    private final JLabel infoLabel;

    public SingleFileRestorePanel(Controller controller, Folder folder, FileInfo fileInfoToRestore) {
        this(controller, folder, fileInfoToRestore, null);
    }

    public SingleFileRestorePanel(Controller controller, Folder folder, FileInfo fileInfoToRestore,
                                  FileInfoVersionTypeHolder selectedInfo) {
        super(controller);
        this.folder = folder;
        this.fileInfoToRestore = fileInfoToRestore;
        this.selectedInfo = selectedInfo; // Note - this may be null.

        infoLabel = new JLabel();
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("140dlu, pref:grow", "pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(infoLabel, cc.xyw(1, 1, 2));

        return builder.getPanel();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.single_file_restore.title");
    }

    protected void initComponents() {
        infoLabel.setText("Work in progress - PFC2202");
    }

    public boolean hasNext() {
        return false;
    }

    public WizardPanel next() {
        return null;
    }
}
