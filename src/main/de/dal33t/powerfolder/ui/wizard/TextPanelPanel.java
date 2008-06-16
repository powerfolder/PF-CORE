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
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import jwf.WizardPanel;

import javax.swing.*;
import java.util.StringTokenizer;

/**
 * A general text panel, displays the given text and offers to finish wizard
 * process. No next panel
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class TextPanelPanel extends PFWizardPanel {

    private String title;
    private String text;

    public TextPanelPanel(Controller controller, String title, String text) {
        super(controller);
        this.title = title;
        this.text = text;
    }

    public boolean hasNext() {
        return false;
    }

    public WizardPanel next() {
        return null;
    }

    public boolean canFinish() {
        return true;
    }

    protected JPanel buildContent() {

        FormLayout layout = new FormLayout("pref",
            "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Add text as labels
        StringTokenizer nizer = new StringTokenizer(text, "\n");
        int y = 1;
        while (nizer.hasMoreTokens()) {
            String line = nizer.nextToken();
            builder.appendRow("pref");
            builder.addLabel(line, cc.xy(1, y));
            y++;
        }
        return builder.getPanel();
    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
    }

    protected Icon getPicto() {
        return getContextPicto();
    }

    protected String getTitle() {
        return title;
    }

}