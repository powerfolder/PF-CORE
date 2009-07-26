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

import javax.swing.*;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.Icons;

/**
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0 $
 */
public class TellFriendPanel extends PFWizardPanel {

    public TellFriendPanel(Controller controller) {
        super(controller);
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout(
            "140dlu, pref:grow",
            "pref, 3dlu, pref, 6dlu, pref, 3dlu, pref, 6dlu, pref, pref, 3dlu, pref, 3dlu, pref, pref, 6dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        return builder.getPanel();
    }

    protected JComponent getPictoComponent() {
        return new JLabel(Icons.getIconById(Icons.PROJECT_WORK_PICTO));
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.tell_friend.title");
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