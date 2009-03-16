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
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.ui.ColorUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;

/**
 * Class showing the Online storage sync details for an ExpandableFolderView
 */
public class OnlineStorageComponent extends PFUIComponent {

    private JPanel uiComponent;
    private JLabel syncLabel;
    private JButton webButton;

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
        syncLabel = new JLabel(Translation.getTranslation(
                "online_storage_component.online_storage_text", 0));
        webButton = new JButtonMini(new MyOnlineStorageAction(getController()), true);
    }

    private void buildUI() {
        FormLayout layout = new FormLayout("pref, pref:grow, pref",
            "3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addSeparator(null, cc.xyw(1, 2, 3));
        builder.add(syncLabel, cc.xy(1, 4));
        builder.add(webButton, cc.xy(3, 4));
        uiComponent = builder.getPanel();
    }

    public void setSyncPercentage(double serverSync, boolean warned) {
        syncLabel.setText(Translation.getTranslation(
                "online_storage_component.online_storage_text", serverSync));
        syncLabel.setForeground(warned ? Color.red :
                ColorUtil.getTextForegroundColor());
        syncLabel.setToolTipText(warned ? Translation.getTranslation(
                "online_storage_component.online_storage_warning") : null);
    }

    private class MyOnlineStorageAction extends BaseAction {

        private MyOnlineStorageAction(Controller controller) {
            super("action_online_storage", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (getController().getOSClient().hasWebURL()) {
                try {
                    BrowserLauncher.openURL(getController().getOSClient()
                        .getWebURL());
                } catch (IOException e1) {
                    logSevere(e1);
                }
            }
        }
    }
}
