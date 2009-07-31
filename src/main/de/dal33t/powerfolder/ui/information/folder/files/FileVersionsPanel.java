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
* $Id: FileDetailsPanel.java 5457 2009-07-31 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.light.FileInfo;

import javax.swing.*;
import java.awt.*;
import java.util.TimerTask;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.DefaultFormBuilder;

/**
 * A Panel to display version history about a file
 *
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class FileVersionsPanel extends PFUIComponent {

    private JPanel panel;
    private JLabel emptyLabel;
    private JLabel otherComponent;
    private volatile FileInfo fileInfo;

    public FileVersionsPanel(Controller controller) {
        super(controller);
    }

    public Component getPanel() {
        if (panel == null) {

            // Initalize components
            initComponents();

            FormLayout layout = new FormLayout(
                        "pref:grow",
                        "fill:0:grow");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            CellConstraints cc = new CellConstraints();

            // emptyLabel and scrollPane occupy the same slot.
            builder.add(emptyLabel, cc.xy(1, 1));
            builder.add(otherComponent, cc.xy(1, 1));

            panel = builder.getPanel();
        }
        return panel;
    }

    private void initComponents() {
        emptyLabel = new JLabel(Translation.getTranslation(
                "file_version_tab.no_versions_available"), SwingConstants.CENTER);
        emptyLabel.setEnabled(false);

        otherComponent = new JLabel("Temporary text component...");
    }

    public void setFileInfo(FileInfo fileInfo) {
        if (panel == null) {
            // Panel not initalized yet
            return;
        }

        this.fileInfo = fileInfo;

        if (fileInfo == null) {
            setEmptyState(true, false);
            return;
        }

        getController().schedule(new TimerTask() {
            public void run() {
                loadVersionHistory();
            }
        }, 100);
    }

    private void loadVersionHistory() {

        setEmptyState(true, true);

        // Loading...

        // @todo harry work in progress...
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }

        // Loaded...

        if (fileInfo == null) {
            // Huh. Loaded the history, but now no FileInfo is selected.
            setEmptyState(true, false);
        } else {
            // Got it. Show it.
            setEmptyState(false, false);
        }
    }

    private void setEmptyState(boolean empty, boolean loading) {
        if (panel == null) {
            return;
        }

        emptyLabel.setVisible(empty);
        if (loading) {
            emptyLabel.setText(Translation.getTranslation(
                    "file_version_tab.loading"));
        } else {
            emptyLabel.setText(Translation.getTranslation(
                    "file_version_tab.no_versions_available"));
        }
        otherComponent.setVisible(!empty);
    }
}
