/* $Id$
 * 
 * Copyright (c) 2007 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.ui.builder;

import javax.swing.JComponent;

import com.jgoodies.forms.builder.PanelBuilder;
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
}
