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

import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.util.StringUtils;

public class TextLinesPanelBuilder {
    public static JPanel createTextPanel(String text, int fontsize) {
        // split into tokens
        String[] contentsArray = text.split("\n");
        FormLayout contentsForm = new FormLayout("pref");
        PanelBuilder builder = new PanelBuilder(contentsForm);

        int row = 1;
        CellConstraints cc = new CellConstraints();

        for (String lineText : contentsArray) {
            if (StringUtils.isEmpty(lineText.trim())) {
                // Add gap
                builder.appendRow("3dlu");
            } else {
                builder.appendRow("pref");
                JLabel label = new JLabel("<HTML><BODY>" + lineText
                        + "</BODY></HTML>");
                Font font = new Font(label.getFont().getFontName(), Font.BOLD,
                        fontsize);
                label.setFont(font);
                builder.add(label, cc.xy(1, row));
            }
            row += 1;
        }
        JPanel textBoxPanel = builder.getPanel();
        textBoxPanel.setBackground(Color.WHITE);
        return textBoxPanel;
    }
}
