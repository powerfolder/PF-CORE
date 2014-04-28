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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.security.Permission;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.BrowserLauncher.URLProducer;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

/**
 * Class showing the Online storage sync details for an ExpandableFolderView
 */
public class OnlineStorageComponent extends PFUIComponent {

    private JPanel uiComponent;
    private ActionLabel syncActionLabel;
    private JButton webButton;
    private MySyncAction syncAction;
    private Folder folder;
    private MyWebButtonAction webButtonAction;

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
        webButtonAction = new MyWebButtonAction(getController());
        webButton = new JButtonMini(webButtonAction);
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
        boolean joined)
    {
        if (joined) {
            syncActionLabel.setText(Translation.getTranslation(
                "online_storage_component.online_storage_text",
                Format.formatPercent(serverSync)).replace("%%", "%"));
            syncActionLabel.setForeground(warned ? Color.red : ColorUtil
                .getTextForegroundColor());
            syncActionLabel
                .setToolTipText(warned
                    ? Translation
                        .getTranslation("online_storage_component.online_storage_warning")
                    : Translation
                        .getTranslation("online_storage_component.online_storage_tip"));
            webButton.setToolTipText(Translation
                .getTranslation("online_storage_component.online_storage_remove"));
        } else {
            syncActionLabel
                .setText(Translation
                    .getTranslation("online_storage_component.online_storage_unjoined_text"));
            syncActionLabel
                .setToolTipText(Translation
                    .getTranslation("online_storage_component.online_storage_unjoined_tip"));
            webButton
                .setToolTipText(Translation
                    .getTranslation("online_storage_component.online_storage_unjoined_tip"));
        }
        syncAction.setJoined(joined);
    }

    public void setFolder(Folder folderArg) {
        folder = folderArg;
        if (folderArg != null) {
            Permission fa = FolderPermission.admin(folderArg.getInfo());
            webButtonAction.allowWith(fa);
            syncAction.allowWith(fa);
        }
    }

    private class MySyncAction extends BaseAction {

        private boolean joined;

        private MySyncAction(Controller controller) {
            super("action_sync_folder", controller);
        }

        public void actionPerformed(ActionEvent e) {
            if (joined) {
                final ServerClient client = getController().getOSClient();
                if (client.supportsWebLogin()) {
                    BrowserLauncher.open(getController(), new URLProducer() {
                        public String url() {
                            return client.getFolderURLWithCredentials(folder
                                .getInfo());
                        }
                    });
                }
            } else {
                PFWizard.openMirrorFolderWizard(getController(), folder);
            }
        }

        public void setJoined(boolean joined) {
            this.joined = joined;
            if (joined) {
                syncAction.allowWith(null);
                syncAction.setEnabled(true);
            }
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
