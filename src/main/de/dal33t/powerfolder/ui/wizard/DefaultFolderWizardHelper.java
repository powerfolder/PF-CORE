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
 * $Id: LoginOnlineStoragePanel.java 5214 2008-09-11 21:12:13Z tot $
 */
package de.dal33t.powerfolder.ui.wizard;

import static de.dal33t.powerfolder.disk.SyncProfile.AUTOMATIC_SYNCHRONIZATION;
import static de.dal33t.powerfolder.ui.wizard.PFWizard.SUCCESS_PANEL;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.BACKUP_ONLINE_STOARGE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.CREATE_DESKTOP_SHORTCUT;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDERINFO_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.FOLDER_LOCAL_BASE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SAVE_INVITE_LOCALLY;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SEND_INVIATION_AFTER_ATTRIBUTE;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.SYNC_PROFILE_ATTRIBUTE;

import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import jwf.WizardContext;
import jwf.WizardPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.LogDispatch;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

/**
 * Helper class to setup the default folder during wizard steps
 * 
 * @author Christian Sprajc
 * @version $Revision$
 */
public class DefaultFolderWizardHelper extends PFUIPanel {

    private ServerClient client;
    private ServerClientListener listener;
    private JComponent panel;

    private ValueModel setupDefaultModel;
    private JCheckBox setupDefaultCB;
    private File defaultSynchronizedFolder;

    DefaultFolderWizardHelper(Controller controller, ServerClient client) {
        super(controller);
        this.client = client;
    }

    protected void initComponents() {
        listener = new MyServerClientListener();

        setupDefaultModel = new ValueHolder(
            PreferencesEntry.SETUP_DEFAULT_FOLDER
                .getValueBoolean(getController()), true);
        setupDefaultCB = BasicComponentFactory.createCheckBox(
            setupDefaultModel, Translation
                .getTranslation("wizard.login_online_storage.setup_default"));
        setupDefaultCB.setOpaque(true);
        setupDefaultCB.setBackground(Color.white);

        defaultSynchronizedFolder = new File(getController()
            .getFolderRepository().getFoldersBasedir(), Translation
            .getTranslation("wizard.basicsetup.default_folder_name"));

        if (defaultSynchronizedFolder.exists()) {
            // Hmmm. User has already created this???
            setupDefaultCB.setSelected(false);
        }

        client.addListener(listener);
    }

    public Component getUIComponent() {
        if (panel == null) {
            initComponents();

            FormLayout layout = new FormLayout("pref, 3dlu, pref", "pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
            builder.add(setupDefaultCB, cc.xy(1, 1));
            builder
                .add(Help.createWikiLinkLabel("Default_Folder"), cc.xy(3, 1));
            builder.setOpaque(true);
            builder.setBackground(Color.white);

            panel = builder.getPanel();
            updateVisibility();
        }
        return panel;
    }

    WizardPanel next(WizardPanel nextPanel, WizardContext context) {
        Reject.ifNull(nextPanel, "Next panel is null");
        boolean setupDefault = (Boolean) setupDefaultModel.getValue();
        // Remind for next logins
        PreferencesEntry.SETUP_DEFAULT_FOLDER.setValue(getController(),
            setupDefault);
        client.removeListener(listener);

        // Create default
        if (setupDefault && client.isLastLoginOK()) {
            Account account = client.getAccount();

            // If there is already a default folder for this account, use that
            FolderInfo accountFolder = account.getDefaultSynchronizedFolder();
            LogDispatch.logInfo(DefaultFolderWizardHelper.class.getName(),
                "Default synced folder on " + account.getUsername() + " is "
                    + accountFolder);

            FolderInfo foInfo;
            if (accountFolder == null) {
                // Default sync folder has user name...
                String name = account.getUsername() + '-'
                    + defaultSynchronizedFolder.getName();
                foInfo = new FolderInfo(name, '[' + IdGenerator.makeId() + ']');
            } else {
                // Take from account.
                foInfo = accountFolder;
            }

            // Redirect via folder create of the default sync folder.
            context.setAttribute(
                WizardContextAttributes.SET_DEFAULT_SYNCHRONIZED_FOLDER, true);
            context.setAttribute(FOLDERINFO_ATTRIBUTE, foInfo);
            context.setAttribute(CREATE_DESKTOP_SHORTCUT, false);
            context.setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE, false);
            context.setAttribute(SUCCESS_PANEL, nextPanel);
            context.setAttribute(SYNC_PROFILE_ATTRIBUTE,
                AUTOMATIC_SYNCHRONIZATION);
            context.setAttribute(FOLDER_LOCAL_BASE, defaultSynchronizedFolder);
            // Create only if not already existing.
            context.setAttribute(BACKUP_ONLINE_STOARGE, accountFolder == null);
            context.setAttribute(SAVE_INVITE_LOCALLY, Boolean.FALSE);
            return new FolderCreatePanel(getController());
        }
        return nextPanel;
    }

    private void updateVisibility() {
        if (panel == null) {
            return;
        }
        // Only show if not already setup
        panel.setVisible(!defaultSynchronizedFolder.exists()
            && client.isLastLoginOK());
    }

    private class MyServerClientListener implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
        }

        public void login(ServerClientEvent event) {
            updateVisibility();
        }

        public void serverConnected(ServerClientEvent event) {
        }

        public void serverDisconnected(ServerClientEvent event) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
