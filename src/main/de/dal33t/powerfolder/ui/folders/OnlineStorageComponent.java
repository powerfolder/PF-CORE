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
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Format;
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
    private ActionLabel syncActionLabel;
    private JButton webButton;
    private MySyncAction syncAction;
    private Folder folder;

    public OnlineStorageComponent(Controller controller, Folder folder) {
        super(controller);
        this.folder = folder;
    }

    public Component getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUI();
        }
        return uiComponent;
    }

    private void initialize() {
        syncAction = new MySyncAction(getController());
        syncActionLabel = new ActionLabel(getController(), syncAction);
        MyWebButtonAction webButtonAction = new MyWebButtonAction(getController());
        webButton = new JButtonMini(webButtonAction, true);
    }

    private void buildUI() {
        FormLayout layout = new FormLayout("pref, pref:grow, pref",
            "3dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addSeparator(null, cc.xyw(1, 2, 3));
        builder.add(syncActionLabel.getUIComponent(), cc.xy(1, 4));
        builder.add(webButton, cc.xy(3, 4));
        uiComponent = builder.getPanel();
        uiComponent.setOpaque(false);
    }

    public void setSyncPercentage(double serverSync, boolean warned,
                                  boolean joined) {
        if (joined) {
            syncActionLabel.setText(Translation.getTranslation(
                    "online_storage_component.online_storage_text", Format
                            .formatNumber(serverSync)));
            syncActionLabel.setForeground(warned ? Color.red : ColorUtil
                    .getTextForegroundColor());
            syncActionLabel.setToolTipText(warned ? Translation.getTranslation(
                    "online_storage_component.online_storage_warning") :
                    Translation.getTranslation(
                            "online_storage_component.online_storage_tip"));
            webButton.setToolTipText(Translation.getTranslation(
                            "online_storage_component.online_storage_tip"));
        } else {
            syncActionLabel.setText(Translation.getTranslation(
                    "online_storage_component.online_storage_unjoined_text"));
            syncActionLabel.setToolTipText(Translation.getTranslation(
                    "online_storage_component.online_storage_unjoined_tip"));
            webButton.setToolTipText(Translation.getTranslation(
                            "online_storage_component.online_storage_unjoined_tip"));
        }
        syncAction.setJoined(joined);
    }

    public void setFolder(Folder folderArg) {
        folder = folderArg;
    }

    private class MySyncAction extends BaseAction {

        private boolean joined;

        private MySyncAction(Controller controller) {
            super("action_sync_folder", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (joined) {
                if (getController().getOSClient().hasWebURL()) {
                    try {
                        BrowserLauncher.openURL(getController().getOSClient()
                            .getLoginURLWithCredentials());
                    } catch (IOException e1) {
                        logSevere(e1);
                    }
                }
            } else {
                PFWizard.openMirrorFolderWizard(getController(), folder);
            }
        }

        public void setJoined(boolean joined) {
            this.joined = joined;
        }
    }

    private class MyWebButtonAction extends BaseAction {

        private MyWebButtonAction(Controller controller) {
            super("action_online_storage", controller);
        }

        public void actionPerformed(ActionEvent e) {
            // FolderOnlineStoragePanel knows if folder already joined :-)
            PFWizard.openMirrorFolderWizard(getController(), folder);
        }
    }
}
