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
 * $Id: FilesStatsPanel.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.files;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.util.Translation;

public class FilesStatsPanel extends PFUIComponent {

    private JPanel uiComponent;

    private JLabel pathLabel;
    private JLabel localLabel;
    private JLabel deletedLabel;

    public FilesStatsPanel(Controller controller) {
        super(controller);
        pathLabel = new JLabel("");
        localLabel = new JLabel(Translation.getTranslation(
            "files_stats_panel.local_label", ""));
        deletedLabel = new JLabel(Translation.getTranslation(
            "files_stats_panel.deleted_label", ""));
    }

    public JPanel getUiComponent() {
        if (uiComponent == null) {
            buildUiComponent();
        }
        return uiComponent;
    }

    public void setStats(long local, long deleted) {
        localLabel.setText(Translation.getTranslation(
            "files_stats_panel.local_label", String.valueOf(local)));
        deletedLabel.setText(Translation.getTranslation(
            "files_stats_panel.deleted_label", String.valueOf(deleted)));
    }

    private void buildUiComponent() {
        FormLayout layout = new FormLayout(
            "fill:pref:grow, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(2, 12));

        JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
        sep2.setPreferredSize(new Dimension(2, 12));

        builder.add(pathLabel, cc.xy(1, 1));
        builder.add(sep1, cc.xy(3, 1));
        builder.add(localLabel, cc.xy(5, 1));
        builder.add(sep2, cc.xy(7, 1));
        builder.add(deletedLabel, cc.xy(9, 1));

        uiComponent = builder.getPanel();
    }

    public void setDirectory(DirectoryInfo dir) {
        String text = dir.getDiskFile(getController().getFolderRepository())
                .toAbsolutePath().toString();
        // Limit insanely long paths.
        if (text.length() > 100) {
            text = text.substring(0, 100);
            int i = text.lastIndexOf(dir
                .getDiskFile(getController().getFolderRepository())
                .getFileSystem().getSeparator());
            // Look for last separator inside the remaining path.
            if (i > -1) {
                text = text.substring(0, i + 1);
            }
            pathLabel.setText(text + "...");
        } else {
            pathLabel.setText(text);
        }
    }
}
