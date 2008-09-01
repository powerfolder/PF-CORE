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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jwf.WizardPanel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

public class LoginOnlineStoragePanel extends PFWizardPanel {

    private ServerClient client;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel connectingLabel;
    private WizardPanel nextPanel;

    private boolean entryRequired;

    private ValueModel setupDefaultModel;
    private JCheckBox setupDefaultCB;

    private File defaultSynchronizedFolder;

    /**
     * Constructs a login panel for login to the default OS.
     * 
     * @param controller
     * @param nextPanel
     *            the next panel to display
     * @param entryRequired
     *            if username and password has to be validated.
     */
    public LoginOnlineStoragePanel(Controller controller,
        WizardPanel nextPanel, boolean entryRequired)
    {
        this(controller, controller.getOSClient(), nextPanel, entryRequired);
    }

    /**
     * @param controller
     * @param client
     *            the online storage client to use.
     * @param nextPanel
     *            the next panel to display
     * @param entryRequired
     *            if username and password has to be validated.
     */
    public LoginOnlineStoragePanel(Controller controller, ServerClient client,
        WizardPanel nextPanel, boolean entryRequired)
    {
        super(controller);
        this.nextPanel = nextPanel;
        this.entryRequired = entryRequired;
        this.client = client;
    }

    public boolean hasNext() {
        return !entryRequired || !StringUtils.isEmpty(usernameField.getText());
    }

    public boolean validateNext(List list) {
        if (!entryRequired && StringUtils.isEmpty(usernameField.getText())) {
            return true;
        }
        // TODO Move this into worker. Make nicer. Difficult because function
        // returns loginOk.
        boolean loginOk = false;
        try {
            loginOk = client.login(usernameField.getText(),
                new String(passwordField.getPassword())).isValid();
            if (!loginOk) {
                list.add(Translation
                    .getTranslation("online_storage.account_data"));
            }
            // FIXME Use separate account stores for different servers?
            ConfigurationEntry.WEBSERVICE_USERNAME.setValue(getController(),
                usernameField.getText());
            ConfigurationEntry.WEBSERVICE_PASSWORD.setValue(getController(),
                new String(passwordField.getPassword()));
            getController().saveConfig();

        } catch (Exception e) {
            Loggable.logSevereStatic(LoginOnlineStoragePanel.class,
                    "Problem logging in", e);
            list.add(Translation.getTranslation("online_storage.general_error",
                e.getMessage()));
        }
        return loginOk;
    }

    public WizardPanel next() {

        boolean setupDefault = (Boolean) setupDefaultModel.getValue();
        // Create default
        if (setupDefault) {
            Account account = client.getAccount();

            // If there is already a default folder for this account, use that
            FolderInfo accountFolder = account.getDefaultSynchronizedFolder();
            Loggable.logInfoStatic(LoginOnlineStoragePanel.class,
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
            getWizardContext().setAttribute(
                WizardContextAttributes.SET_DEFAULT_SYNCHRONIZED_FOLDER, true);
            getWizardContext().setAttribute(FOLDERINFO_ATTRIBUTE, foInfo);
            getWizardContext().setAttribute(CREATE_DESKTOP_SHORTCUT, false);
            getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
                false);
            getWizardContext().setAttribute(SUCCESS_PANEL, nextPanel);
            getWizardContext().setAttribute(SYNC_PROFILE_ATTRIBUTE,
                AUTOMATIC_SYNCHRONIZATION);
            getWizardContext().setAttribute(FOLDER_LOCAL_BASE,
                defaultSynchronizedFolder);
            // Create only if not already existing.
            getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE,
                accountFolder == null);

            getWizardContext().setAttribute(SAVE_INVITE_LOCALLY,
                Boolean.FALSE);

            return new FolderCreatePanel(getController());
        }
        // Remind for next logins
        PreferencesEntry.SETUP_DEFAULT_FOLDER.setValue(getController(),
            setupDefault);

        return nextPanel;
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("$wlabel, $lcg, $wfield, 0:g",
            "pref, 10dlu, pref, 5dlu, pref, 5dlu, pref, 15dlu, pref, 5dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.enteraccount"), cc.xyw(1, 1, 4));

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.username"), cc.xy(1, 3));
        builder.add(usernameField, cc.xy(3, 3));
        builder.add(connectingLabel, cc.xy(3, 3));

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.password"), cc.xy(1, 5));
        builder.add(passwordField, cc.xy(3, 5));

        builder.add(new LinkLabel(Translation
            .getTranslation("pro.wizard.activation.register_now"),
            Constants.ONLINE_STORAGE_REGISTER_URL), cc.xy(3, 7));

        LinkLabel link = new LinkLabel(Translation
            .getTranslation("wizard.webservice.learnmore"),
            "http://www.powerfolder.com/node/webservice");
        builder.add(link, cc.xyw(1, 9, 4));

        if (defaultSynchronizedFolder.exists()) {
            // Hmmm. User has already created this???
            setupDefaultCB.setSelected(false);
        } else {
            builder.add(createSetupDefaultPanel(), cc.xyw(1, 11, 4));
        }

        return builder.getPanel();
    }

    private Component createSetupDefaultPanel() {
        FormLayout layout = new FormLayout("pref, 3dlu, pref", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(setupDefaultCB, cc.xy(1, 1));
        builder.add(Help.createWikiLinkLabel("Default_Folder"), cc.xy(3, 1));
        builder.setOpaque(true);
        builder.setBackground(Color.white);

        return builder.getPanel();
    }

    // UI building ************************************************************

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
        // FIXME Use separate account stores for diffrent servers?
        ValueModel usernameModel = new ValueHolder(
            ConfigurationEntry.WEBSERVICE_USERNAME.getValue(getController()),
            true);
        usernameField = BasicComponentFactory.createTextField(usernameModel);
        passwordField = new JPasswordField(
            ConfigurationEntry.WEBSERVICE_PASSWORD.getValue(getController()));
        updateButtons();
        usernameModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });
        connectingLabel = SimpleComponentFactory.createLabel(Translation
            .getTranslation("wizard.login_online_storage.connecting"));
        updateOnlineStatus();
        client.addListener(new MyServerClientListner());

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
    }

    protected Icon getPicto() {
        return Icons.WEBSERVICE_PICTO;
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.webservice.login");
    }

    private void updateOnlineStatus() {
        boolean enabled = client.isConnected();
        usernameField.setVisible(enabled);
        passwordField.setVisible(enabled);
        connectingLabel.setVisible(!enabled);
    }

    private class MyServerClientListner implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
        }

        public void login(ServerClientEvent event) {
        }

        public void serverConnected(ServerClientEvent event) {
            updateOnlineStatus();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateOnlineStatus();
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
