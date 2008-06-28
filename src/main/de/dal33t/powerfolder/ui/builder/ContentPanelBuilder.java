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
package de.dal33t.powerfolder.ui.builder;

import javax.swing.JComponent;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.util.Reject;

/**
 * A builder to easily create a content panel with:
 * <p>
 * A quickinfo panel
 * <p>
 * A toolbar
 * <p>
 * A content area
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ContentPanelBuilder {

    private JComponent panel;

    private JComponent quickInfo;
    private JComponent filterbar;
    private JComponent toolbar;
    private JComponent content;

    /**
     * Actually builds the panel upon the given input.
     * 
     * @return the panel.
     */
    public JComponent getPanel() {
        if (panel == null) {
            Reject.ifNull(content, "Content is null");

            FormLayout layout = new FormLayout("fill:pref:grow");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            int row = 1;
            if (quickInfo != null) {
                builder.appendRow("pref");
                builder.add(quickInfo, cc.xy(1, row));
                row++;
                builder.appendRow("pref");
                builder.addSeparator(null, cc.xy(1, row));
                row++;
            }

            if (filterbar != null) {
                builder.appendRow("pref");
                builder.add(filterbar, cc.xy(1, row));
                row++;
                builder.appendRow("pref");
                builder.addSeparator(null, cc.xy(1, row));
                row++;
            }

            if (toolbar != null) {
                builder.appendRow("pref");
                builder.add(toolbar, cc.xy(1, row));
                row++;
                builder.appendRow("pref");
                builder.addSeparator(null, cc.xy(1, row));
                row++;
            }

            builder.appendRow("fill:pref:grow");
            builder.add(content, cc.xy(1, row));
            row++;

            panel = builder.getPanel();
        }
        return panel;
    }

    // Accessors **************************************************************

    public JComponent getContent() {
        return content;
    }

    public void setContent(JComponent content) {
        Reject.ifTrue(panel != null, "Panel already built");
        this.content = content;
    }

    public JComponent getQuickInfo() {
        return quickInfo;
    }

    public void setQuickInfo(JComponent quickInfo) {
        Reject.ifTrue(panel != null, "Panel already built");
        this.quickInfo = quickInfo;
    }

    public JComponent getToolbar() {
        return toolbar;
    }

    public void setToolbar(JComponent toolbar) {
        Reject.ifTrue(panel != null, "Panel already built");
        this.toolbar = toolbar;
    }

    public JComponent getFilterbar() {
        return filterbar;
    }

    public void setFilterbar(JComponent filterbar) {
        this.filterbar = filterbar;
    }
}
