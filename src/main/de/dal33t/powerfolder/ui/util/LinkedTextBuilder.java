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
package de.dal33t.powerfolder.ui.util;

import javax.swing.JLabel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * helper class to create a text with links.
 *
 * @version $Revision: 1.1 $
 */
public class LinkedTextBuilder {

    /**
     * All lines starting with http:// are transformed into linkLabels.
     *
     * @return PanelBuilder containing the generated JPanel
     */
    public static PanelBuilder build(Controller controller, String text) {
        CellConstraints cc = new CellConstraints();

        FormLayout layout = new FormLayout("pref");
        PanelBuilder builder = new PanelBuilder(layout);
        // split into tokens
        String[] txtTokens = text.split("\n");

        int row = 1;
        for (String lineText : txtTokens) {
            // Make it simple stoopid. A line can be a link or a text.
            // Simplifies things much
            if (lineText.toLowerCase().startsWith("http://")) {
                builder.appendRow("pref");
                builder.add(new LinkLabel(controller, lineText, lineText).getUIComponent(), cc.xy(1,
                        row));
            } else if (StringUtils.isEmpty(lineText.trim())) {
                // Add gap
                builder.appendRow("3dlu");
            } else {
                builder.appendRow("pref");
                builder.add(new JLabel(lineText), cc.xy(1, row));
            }
            row += 1;
        }
        return builder;
    }
}
