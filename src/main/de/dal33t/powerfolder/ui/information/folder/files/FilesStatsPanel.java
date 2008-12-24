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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;

public class FilesStatsPanel extends PFUIComponent {

    private JPanel uiComponent;

    private JLabel localLabel;
    private JLabel incomingLabel;
    private JLabel deletedLabel;
    private JLabel recycledLabel;

    public FilesStatsPanel(Controller controller) {
        super(controller);
        localLabel = new JLabel(Translation.getTranslation("files_stats_panel.local_label", ""));
        incomingLabel = new JLabel(Translation.getTranslation("files_stats_panel.incoming_label", ""));
        deletedLabel = new JLabel(Translation.getTranslation("files_stats_panel.deleted_label", ""));
        recycledLabel = new JLabel(Translation.getTranslation("files_stats_panel.recycled_label", ""));
    }

    public JPanel getUiComponent() {
        if (uiComponent == null) {
            buildUiComponent();
        }
        return uiComponent;
    }

    public void setStats(long local, long incoming, long deleted, long recycled) {
        localLabel.setText(Translation.getTranslation(
                "files_stats_panel.local_label", String.valueOf(local)));
        incomingLabel.setText(Translation.getTranslation(
                "files_stats_panel.incoming_label", String.valueOf(incoming)));
        deletedLabel.setText(Translation.getTranslation(
                "files_stats_panel.deleted_label", String.valueOf(deleted)));
        recycledLabel.setText(Translation.getTranslation(
                "files_stats_panel.recycled_label", String.valueOf(recycled)));
    }

    private void buildUiComponent() {
        FormLayout layout = new FormLayout(
            "fill:pref:grow, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(2, 12));

        JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
        sep2.setPreferredSize(new Dimension(2, 12));

        JSeparator sep3 = new JSeparator(SwingConstants.VERTICAL);
        sep3.setPreferredSize(new Dimension(2, 12));

        builder.add(localLabel, cc.xy(2, 1));
        builder.add(sep1, cc.xy(4, 1));
        builder.add(incomingLabel, cc.xy(6, 1));
        builder.add(sep2, cc.xy(8, 1));
        builder.add(deletedLabel, cc.xy(10, 1));
        builder.add(sep3, cc.xy(12, 1));
        builder.add(recycledLabel, cc.xy(14, 1));

        uiComponent = builder.getPanel();        
    }
}
