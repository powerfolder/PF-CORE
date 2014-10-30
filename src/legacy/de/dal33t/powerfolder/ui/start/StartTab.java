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
 * $Id: WelcomeTab.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.start;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.security.OnlineStorageSubscription;
import de.dal33t.powerfolder.ui.FileDropTransferHandler;
import de.dal33t.powerfolder.ui.util.UIUtil;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.GradientPanel;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.ui.wizard.PFWizard;
import de.dal33t.powerfolder.ui.wizard.PFWizardPanel;
import de.dal33t.powerfolder.ui.wizard.WhatToDoPanel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import jwf.WizardContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Date;

/**
 * Class for the Status tab in the main tab area of the UI.
 */
public class StartTab extends PFUIComponent {

    private JPanel uiComponent;
    private ActionLabel synchronizedLink;
    private ActionLabel backupLink;
    private ActionLabel hostLink;
    private LinkLabel documentationLink;
    private ActionLabel tellFriendLabel;
    private ServerClient client;
    private ActionLabel onlineStorageAccountLabel;

    /**
     * Constructor
     * 
     * @param controller
     */
    public StartTab(Controller controller) {
        super(controller);
    }

    /**
     * @return the UI component after optionally building it.
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    /**
     * One-off build of UI component.
     */
    private void buildUI() {
        initComponents();

        FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:0:grow");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Toolbar
        JPanel toolbar = createToolBar();
        toolbar.setOpaque(false);
        builder.add(toolbar, cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 2));

        // Main panel in scroll pane
        JPanel mainPanel = buildMainPanel();
        mainPanel.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        UIUtil.removeBorder(scrollPane);
        builder.add(scrollPane, cc.xyw(1, 6, 2));

        uiComponent = GradientPanel.create(builder.getPanel());
    }

    /**
     * Initialise class components.
     */
    private void initComponents() {
        client = getApplicationModel().getServerClientModel().getClient();
        client.addListener(new MyServerClientListener());
        synchronizedLink = new ActionLabel(getController(),
            new DoSynchronizedAction(Translation
                .getTranslation("wizard.what_to_do.synchronized_folder")));
        synchronizedLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.synchronized_folder.tip"));
        synchronizedLink.convertToBigLabel();

        backupLink = new ActionLabel(getController(), new DoBackupAction(
            Translation.getTranslation("wizard.what_to_do.backup_folder")));
        backupLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.backup_folder.tip"));
        backupLink.convertToBigLabel();

        hostLink = new ActionLabel(getController(), new DoHostAction(
            Translation.getTranslation("wizard.what_to_do.host_work")));
        hostLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.host_work.tip"));
        hostLink.convertToBigLabel();

        documentationLink = Help.createQuickstartGuideLabel(getController(),
            Translation
                .getTranslation("wizard.what_to_do.open_online_documentation"));
        documentationLink.setToolTipText(Translation
            .getTranslation("wizard.what_to_do.open_online_documentation.tip"));
        documentationLink.convertToBigLabel();
        onlineStorageAccountLabel = new ActionLabel(getController(),
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    PFWizard.openLoginWizard(getController(), getController()
                        .getOSClient());
                }
            });
        tellFriendLabel = SimpleComponentFactory
            .createTellAFriendLabel(getController());

        updateOnlineStorageDetails();

    }

    /**
     * Build the main panel with all the detail lines.
     * 
     * @return
     */
    private JPanel buildMainPanel() {
        FormLayout layout = new FormLayout(
            "pref:grow",
            "pref, 10dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref:grow, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        // Bottom border
        builder.setBorder(Borders.createEmptyBorder("1dlu, 3dlu, 2dlu, 3dlu"));
        CellConstraints cc = new CellConstraints();

        JLabel label = new JLabel(Translation
            .getTranslation("start_tab.welcome_text"));
        UIUtil.setFontSize(label, UIUtil.MED_FONT_SIZE);
        UIUtil.setFontStyle(label, Font.BOLD);

        int row = 1;
        builder.add(label, cc.xy(1, row));

        row += 2;

        builder.add(synchronizedLink.getUIComponent(), cc.xy(1, row));

        row += 2;

        builder.add(backupLink.getUIComponent(), cc.xy(1, row));

        row += 2;

        builder.add(hostLink.getUIComponent(), cc.xy(1, row));

        row += 2;

        builder.add(documentationLink.getUIComponent(), cc.xy(1, row));

        row += 2;

        builder.add(onlineStorageAccountLabel.getUIComponent(), cc.xy(1, row));

        row += 2;

        builder.addLabel(Translation.getTranslation("start_tab.drag_hint"), cc
            .xy(1, row, CellConstraints.CENTER, CellConstraints.CENTER));

        if (PreferencesEntry.SHOW_TELL_A_FRIEND
            .getValueBoolean(getController()))
        {
            row += 2;
            builder.add(tellFriendLabel.getUIComponent(), cc.xy(1, row));
        }

        JPanel panel = builder.getPanel();

        panel.setTransferHandler(new FileDropTransferHandler(getController()));

        return panel;
    }

    /**
     * Cretes the toolbar.
     * 
     * @return the toolbar
     */
    private JPanel createToolBar() {

        FormLayout layout = new FormLayout("pref, 3dlu, pref, 3dlu:grow",
            "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        ActionLabel newFolderLink = new ActionLabel(getController(),
                getApplicationModel().getActionModel().getFolderWizardAction());
        builder.add(newFolderLink.getUIComponent(), cc.xy(1, 1));
        if (!getController().isBackupOnly()) {
            ActionLabel searchComputerLink = new ActionLabel(getController(), 
                    getApplicationModel().getActionModel().getFindComputersAction());
            builder.add(searchComputerLink.getUIComponent(), cc.xy(3, 1));
        }

        return builder.getPanel();
    }

    private void updateOnlineStorageDetails() {
        boolean show = false;
        String username = client.getUsername();
        if (username != null && username.trim().length() != 0) {
            if (client.isConnected() && !client.isPasswordEmpty()) {
                if (client.isLoggedIn()) {
                    OnlineStorageSubscription storageSubscription = client
                        .getAccount().getOSSubscription();
                    if (storageSubscription.isDisabled()) {
                        Date expirationDate = storageSubscription
                            .getDisabledExpirationDate();
                        if (storageSubscription.isDisabledExpiration()
                            && expirationDate != null)
                        {
                            onlineStorageAccountLabel
                                .setText(Translation
                                    .getTranslation(
                                        "status_tab.online_storage.account_disabled_expiration",
                                        username,
                                        Format
                                            .formatDateCanonical(expirationDate)));
                        } else if (storageSubscription.isDisabledUsage()) {
                            onlineStorageAccountLabel
                                .setText(Translation
                                    .getTranslation(
                                        "status_tab.online_storage.account_disabled_usage",
                                        username));
                        } else {
                            onlineStorageAccountLabel
                                .setText(Translation
                                    .getTranslation(
                                        "status_tab.online_storage.account_disabled",
                                        username));
                        }
                        onlineStorageAccountLabel
                            .setToolTipText(Translation
                                .getTranslation("status_tab.online_storage.account_disabled.tips"));
                        show = true;
                    }
                }
            }
        }
        onlineStorageAccountLabel.getUIComponent().setVisible(show);
    }

    private class DoSynchronizedAction extends AbstractAction {

        private DoSynchronizedAction(String name) {
            putValue(NAME, name);
        }

        public void actionPerformed(ActionEvent e) {
            PFWizard wizard = new PFWizard(getController(), Translation
                .getTranslation("wizard.pfwizard.folder_title"));
            WizardContext context = wizard.getWizardContext();
            PFWizardPanel panel = WhatToDoPanel.doSyncOption(getController(),
                context);
            wizard.open(panel);
        }
    }

    private class DoBackupAction extends AbstractAction {

        private DoBackupAction(String name) {
            putValue(NAME, name);
        }

        public void actionPerformed(ActionEvent e) {
            PFWizard wizard = new PFWizard(getController(), Translation
                .getTranslation("wizard.pfwizard.folder_title"));
            WizardContext context = wizard.getWizardContext();
            PFWizardPanel panel = WhatToDoPanel.doBackupOption(getController(),
                context);
            wizard.open(panel);
        }
    }

    private class DoHostAction extends AbstractAction {

        private DoHostAction(String name) {
            putValue(NAME, name);
        }

        public void actionPerformed(ActionEvent e) {
            PFWizard wizard = new PFWizard(getController(), Translation
                .getTranslation("wizard.pfwizard.folder_title"));
            WizardContext context = wizard.getWizardContext();
            PFWizardPanel panel = WhatToDoPanel.doHostOption(getController(),
                context);
            wizard.open(panel);
        }
    }

    private class MyServerClientListener implements ServerClientListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void accountUpdated(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void login(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void serverConnected(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
            updateOnlineStorageDetails();
        }
    }

}