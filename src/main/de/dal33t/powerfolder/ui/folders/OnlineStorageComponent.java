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
 * $Id: OnlineStorageComponent.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.folders;

import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

/**
 * Class showing the Online storage sync details for an ExpandableFolderView
 */
public class OnlineStorageComponent extends PFUIComponent {

    private JPanel uiComponent;

    public OnlineStorageComponent(Controller controller) {
        super(controller);
    }

    public Component getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUI();
        }
        return uiComponent;
    }

    private void initialize() {
    }

    private void buildUI() {
        FormLayout layout = new FormLayout("pref, pref:grow",
            "3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addSeparator(null, cc.xyw(1, 2, 2));
        builder.add(new JLabel(Translation.getTranslation("online_storage_component.online_storage_text", 0)), cc.xy(1, 4));
        uiComponent = builder.getPanel();
        uiComponent.setBackground(SystemColor.text);

    }
}
