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

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.security.SecurityException;
import de.dal33t.powerfolder.security.Token;
import de.dal33t.powerfolder.ui.dialog.ConfigurationLoaderDialog;
import de.dal33t.powerfolder.ui.util.IdPSelectionBox;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.*;
import jwf.WizardPanel;

import javax.swing.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class LoginPanel extends PFWizardPanel {
    private static final Logger LOG = Logger.getLogger(LoginPanel.class.getName());
    private static final String TOKEN_PLACEHOLDER = "_TOKEN_PLACEHOLDER_";

    private ServerClient client;
    private boolean showUseOS;

    private JComboBox<String> serverURLBox;
    private JLabel serverURLLabel;
    private JLabel idPLabel;
    private IdPSelectionBox idPSelectBox;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private LinkLabel recoverPasswordLabel;
    private LinkLabel createAccountLabel;
    private JLabel connectingLabel;
    private JLabel serverLabel;
    private ActionLabel serverInfoLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JProgressBar workingBar;
    private JCheckBox rememberPasswordBox;
    private JCheckBox useOSBox;
    private WizardPanel nextPanel;

    /**
     * Constructs a login panel for login to the default OS.
     *
     * @param controller
     * @param nextPanel  the next panel to display
     * @param showUseOS  if the checkbox to use Online Storage should be displayed
     */
    public LoginPanel(Controller controller, WizardPanel nextPanel, boolean showUseOS) {
        this(controller, controller.getOSClient(), nextPanel, showUseOS);
    }

    /**
     * @param controller
     * @param client     the online storage client to use.
     * @param nextPanel  the next panel to display
     * @param showUseOS  if the checkbox to use Online Storage should be displayed
     */
    public LoginPanel(Controller controller, ServerClient client, WizardPanel nextPanel, boolean showUseOS) {
        super(controller);
        Reject.ifNull(nextPanel, "Nextpanel is null");
        this.nextPanel = nextPanel;
        this.client = client;
        this.showUseOS = showUseOS;
        LOG.log(Level.FINE, "Opening login wizard");
    }

    public boolean hasNext() {
        return client.isConnected()
                && !StringUtils.isEmpty(usernameField.getText())
                && (passwordField.getPassword() != null && passwordField
                .getPassword().length > 0)
                && (StringUtils.isNotBlank(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL
                .getValue(getController())) ? idPSelectBox.isListLoaded() : true)
                && (idPSelectBox == null || idPSelectBox.getSelectedIndex() != 0);
    }

    public WizardPanel next() {
        // PFC-2638: Desktop sync option
        nextPanel = DesktopSyncSetupPanel.insertStepIfAvailable(getController(), nextPanel, client);

        return new SwingWorkerPanel(getController(), new LoginTask(),
                Translation.get("wizard.login_online_storage.logging_in"),
                Translation.get("wizard.login_online_storage.logging_in.text"),
                nextPanel);
    }

    protected JPanel buildContent() {
        String layoutRows;

        if (StringUtils.isBlank(ConfigurationEntry.SERVER_CONNECTION_URLS.getValue(getController()))
                || StringUtils.isBlank(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL.getValue(getController()))) {
            layoutRows = "15dlu, 7dlu, 15dlu, 7dlu, 15dlu, 3dlu, 15dlu, 3dlu, 15dlu, 34dlu, pref, 20dlu, pref, 3dlu, pref, 3dlu, pref";
        } else {
            layoutRows = "15dlu, 7dlu, 15dlu, 7dlu, 15dlu, 7dlu, 15dlu, 3dlu, 15dlu, 3dlu, 15dlu, 34dlu, pref, 3dlu, pref, 20dlu, pref, 3dlu, pref";
        }

        FormLayout layout = new FormLayout("50dlu, 3dlu, 110dlu, 40dlu, pref", layoutRows);
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        int row = 1;

        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_CONNECTION_URLS.getValue(getController()))) {
            builder.add(serverURLLabel, cc.xy(1, row));
            builder.add(serverURLBox, cc.xy(3, row));
            row += 2;
        }

        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL.getValue(getController()))) {
            builder.add(idPLabel, cc.xy(1, row));
            builder.add(idPSelectBox, cc.xy(3, row));
            row += 2;
        }

        // usernameField and connectingLabel have the same slot.
        builder.add(usernameLabel, cc.xy(1, row));
        builder.add(usernameField, cc.xy(3, row));
        builder.add(connectingLabel, cc.xyw(1, row, 4));

        row += 2;

        // passwordField and workingBar have the same slot.
        builder.add(passwordLabel, cc.xy(1, row));
        builder.add(passwordField, cc.xy(3, row));
        builder.add(workingBar, cc.xyw(1, row, 3));

        row += 2;
        builder.add(rememberPasswordBox, cc.xyw(3, row, 2));
        row += 2;
        builder.add(recoverPasswordLabel.getUIComponent(), cc.xyw(3, row, 2));
        row += 2;
        builder.add(createAccountLabel.getUIComponent(), cc.xyw(3, row, 2));
        row += 2;
        builder.add(serverLabel, cc.xy(1, row));
        builder.add(serverInfoLabel.getUIComponent(), cc.xyw(3, row, 2));
        row += 2;

        if (showUseOS) {
            builder.add(useOSBox, cc.xyw(1, row, 4));
            row += 2;
            LinkLabel link = new LinkLabel(getController(),
                    Translation.get("wizard.webservice.learn_more"),
                    ConfigurationEntry.PROVIDER_ABOUT_URL.getValue(getController()));
            builder.add(link.getUIComponent(), cc.xyw(1, row, 5));
            row += 2;
        }

        return builder.getPanel();
    }

    // UI building ************************************************************

    /**
     * Initializes all necessary components
     */
    protected void initComponents() {
        boolean changeLoginAllowed = ConfigurationEntry.SERVER_CONNECT_CHANGE_LOGIN_ALLOWED
                .getValueBoolean(getController());
        boolean rememberPasswordAllowed = ConfigurationEntry.SERVER_CONNECT_REMEMBER_PASSWORD_ALLOWED
                .getValueBoolean(getController());
        serverLabel = new JLabel(Translation.get("general.server"));
        serverInfoLabel = new ActionLabel(getController(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                new ConfigurationLoaderDialog(getController()).openAndWait();
            }
        });
        serverInfoLabel.setText(client.getServerString());
        serverInfoLabel.setEnabled(changeLoginAllowed);

        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_CONNECTION_URLS.getValue(getController()))) {
            serverURLLabel = new JLabel(Translation.get("general.server"));

            String webURL = client.getWebURL();
            int selection = 0;

            String allServers = ConfigurationEntry.SERVER_CONNECTION_URLS.getValue(getController());
            String[] allServersArray = allServers.split(";");
            String[] serverLabels = new String[allServersArray.length];

            for (int i = 0; i < allServersArray.length; i++) {
                try {
                    String server = allServersArray[i];
                    serverLabels[i] = server.substring(0, server.indexOf("="));
                    String serverURL = server.substring(server.indexOf("=") + 1);
                    if (serverURL.equals(webURL)) {
                        selection = i;
                    }
                } catch (Exception e) {
                    Logger.getLogger(LoginPanel.class.getName()).warning(
                            "Unable to read servers config: " + allServers);
                }
            }

            serverURLBox = new JComboBox<String>(serverLabels);
            serverURLBox.setSelectedIndex(selection);
            serverURLBox.setEditable(false);
            serverURLBox.addActionListener(new ServerSelectAction());
        }

        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL.getValue(getController()))) {
            idPLabel = new JLabel(Translation.get("general.idp"));
            idPSelectBox = new IdPSelectionBox(getController());
            idPSelectBox.addItemListener(e -> updateButtons());
            idPSelectBox.addItemListener(e -> updateOnlineStatus());
        }

        usernameLabel = new JLabel(LoginUtil.getUsernameLabel(getController()));
        usernameField = new JTextField();
        usernameField.addKeyListener(new MyKeyListener());
        usernameField.setEditable(changeLoginAllowed);
        passwordLabel = new JLabel(Translation.get("general.password") + ':');
        passwordField = new JPasswordField();
        passwordField.setEditable(changeLoginAllowed);
        passwordField.addKeyListener(new MyKeyListener());

        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL.getValue(getController()))) {
            usernameField.setText(ConfigurationEntry.SERVER_CONNECT_USERNAME.getValue(getController()));
            if (ConfigurationEntry.SERVER_CONNECT_PASSWORD.hasNonBlankValue(getController())) {
                passwordField.setText(new String(LoginUtil.deobfuscate(ConfigurationEntry.SERVER_CONNECT_PASSWORD
                        .getValue(getController()))));
            } else if (ConfigurationEntry.SERVER_CONNECT_TOKEN.hasNonBlankValue(getController())) {
                if (!Token.isExpired(client.getDeviceToken())) {
                    passwordField.setText(TOKEN_PLACEHOLDER);
                } else {
                    passwordField.setText("");
                }
            }
        } else if (client.isConnected()) {
            usernameField.setText(client.getUsername());
            if (!client.isPasswordEmpty()) {
                passwordField.setText(client.getPasswordClearText());
            } else if (client.isTokenLogin()) {
                if (!Token.isExpired(client.getDeviceToken())) {
                    passwordField.setText(TOKEN_PLACEHOLDER);
                } else {
                    passwordField.setText("");
                }
            }
        }

        rememberPasswordBox = BasicComponentFactory.createCheckBox(
                        PreferencesEntry.SERVER_REMEMBER_PASSWORD.getModel(getController()),
                        Translation.get("wizard.login_online_storage.remember_password"));
        rememberPasswordBox.setOpaque(false);
        rememberPasswordBox.setVisible(changeLoginAllowed && rememberPasswordAllowed);

        recoverPasswordLabel = new LinkLabel(getController(),
                Translation.get("exp.wizard.webservice.recover_password"),
                client.getRecoverPasswordURL());
        recoverPasswordLabel.setVisible(client.supportsRecoverPassword());

        createAccountLabel = new LinkLabel(getController(), Translation.get("wizard.activation.signup_account"),
                client.getRegisterURL());
        createAccountLabel.setVisible(client.supportsWebRegistration());

        useOSBox = new JCheckBox(Translation.get("wizard.login_online_storage.no_os")); // @todo
        // "Use online storage"?
        useOSBox.setSelected(!PreferencesEntry.USE_ONLINE_STORAGE.getValueBoolean(getController()));
        useOSBox.addActionListener(e -> PreferencesEntry.USE_ONLINE_STORAGE.setValue(getController(), !useOSBox.isSelected()));
        useOSBox.setOpaque(false);
        connectingLabel = SimpleComponentFactory.createLabel(Translation.get("wizard.login_online_storage.connecting"));
        workingBar = new JProgressBar();
        workingBar.setIndeterminate(true);
        updateOnlineStatus();
        client.addListener(new MyServerClientListner());

        // Never run forever
        getController().scheduleAndRepeat(() -> {
            if (!client.isConnected()) {
                getWizard().next();
            }
        }, 60000L, 10000L);

        updateButtons();
    }

    protected String getTitle() {
        return Translation.get("exp.wizard.webservice.login");
    }

    private void updateOnlineStatus() {
        boolean connected = client.isConnected();
        boolean changeLoginAllowed = ConfigurationEntry.SERVER_CONNECT_CHANGE_LOGIN_ALLOWED.getValueBoolean(getController());
        boolean rememberPasswordAllowed = ConfigurationEntry.SERVER_CONNECT_REMEMBER_PASSWORD_ALLOWED.getValueBoolean(getController());
        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_CONNECTION_URLS.getValue(getController()))) {
            serverURLLabel.setVisible(true);
            serverURLBox.setVisible(true);
        }
        boolean idpSelected = idPSelectBox == null || idPSelectBox.getSelectedIndex() > 0;
        boolean unPwFieldVisible = connected && idpSelected;
        usernameLabel.setVisible(unPwFieldVisible);
        usernameField.setVisible(unPwFieldVisible);
        passwordLabel.setVisible(unPwFieldVisible);
        passwordField.setVisible(unPwFieldVisible);
        // loginButton.setVisible(enabled);
        rememberPasswordBox.setVisible(connected && changeLoginAllowed && rememberPasswordAllowed);
        recoverPasswordLabel.setVisible(connected && client.supportsRecoverPassword());
        createAccountLabel.setVisible(client.supportsWebRegistration());

        connectingLabel.setVisible(!connected);
        workingBar.setVisible(!connected);

        if (getController().getOSClient().showServerInfo()) {
            serverLabel.setVisible(true);
            serverInfoLabel.getUIComponent().setVisible(true);
            serverInfoLabel.setText(client.getServerString());
        } else {
            serverLabel.setVisible(false);
            serverInfoLabel.getUIComponent().setVisible(false);
        }

        if (connected) {
            usernameLabel.requestFocus();
        }
        updateButtons();
    }

    /**
     * Open the basedir in file browser iff the main frame is minimized.
     */
    private void openBasedirIfMinimized() {
        if (getController().getUIController().getMainFrame().isIconifiedOrHidden()) {
            if (!PreferencesEntry.WEBDAV_ONLY.getValueBoolean(getController())) {
                getController().getUIController().getApplicationModel().openExplorer();
            } else {
                getController().getUIController().getApplicationModel().openExplorerWebDAVPath();
            }
        }
    }

    private class LoginTask implements Runnable {
        public void run() {
            try {
                if (!client.isConnected()) {
                    LOG.log(Level.WARNING, "Unable to connect");
                    throw new SecurityException(Translation.get("wizard.webservice.connect_failed"));
                }

                boolean loginOk = false;
                String newUsername = usernameField.getText();
                char[] pw = passwordField.getPassword();
                boolean reuseToken = Arrays.equals(TOKEN_PLACEHOLDER.toCharArray(), pw) && newUsername != null && newUsername.equals(client.getUsername());
                if (reuseToken) {
                    loginOk = client.login(newUsername, client.getDeviceToken()).isValid();
                } else {
                    loginOk = client.login(newUsername, pw).isValid();
                }

                LoginUtil.clear(pw);
                if (!loginOk) {
                    if (isShibbolethIDPMissing()) {
                        JDialog diag = getWizardDialog();
                        diag.setVisible(false);
                        diag.dispose();
                        PFWizard.openLoginWizard(getController(), client);
                        return;
                    }

                    throw new SecurityException(Translation.get("online_storage.account_data"));
                }

                // PFC-2517: if the main frame is minimized after activation, show
                // PowerFolders to the user to make clear that the software is now active
                if (getController().isFirstStart()) {
                    openBasedirIfMinimized();
                }

            } catch (SecurityException e) {
                LOG.log(Level.SEVERE, "Problem logging in: " + e.getMessage());
                throw e;
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Problem logging in: " + e, e);
                throw new SecurityException(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }
    }

    private boolean isShibbolethIDPMissing() {
        if (!ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL.hasNonBlankValue(getController())) {
            return false;
        }
        return !ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP.hasNonBlankValue(getController());
    }

    private JDialog getWizardDialog() {
        return (JDialog) getWizardContext().getAttribute(WizardContextAttributes.DIALOG_ATTRIBUTE);
    }

    private class MyServerClientListner implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
        }

        public void login(ServerClientEvent event) {
            if (idPSelectBox.isBrowserLoginOpened()) {
                JDialog diag = getWizardDialog();
                diag.setVisible(false);
                diag.dispose();
            }
        }

        public void serverConnected(ServerClientEvent event) {
            usernameField.setText(client.getUsername());
            if (!client.isPasswordEmpty()) {
                passwordField.setText(client.getPasswordClearText());
            } else if (client.isTokenLogin()) {
                if (!Token.isExpired(client.getDeviceToken())) {
                    passwordField.setText(TOKEN_PLACEHOLDER);
                }
            }
            updateOnlineStatus();
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
        }

        public void childClientSpawned(ServerClientEvent event) {
            updateOnlineStatus();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateOnlineStatus();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyKeyListener extends KeyAdapter {
        public void keyReleased(KeyEvent e) {
            // Fires hasNext(), to see if user has entered username.
            updateButtons();
        }
    }

    @SuppressWarnings("unchecked")
    private class ServerSelectAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JComboBox<String> source = (JComboBox<String>) e.getSource();

            String item = (String) source.getSelectedItem();
            String serversList = ConfigurationEntry.SERVER_CONNECTION_URLS.getValue(getController());

            // find the item, skip it an the equals-sign
            int begin = serversList.indexOf(item) + item.length() + 1;
            int end = serversList.indexOf(";", begin);

            if (end == -1) {
                end = serversList.length();
            }

            final String server = serversList.substring(begin, end);
            getController().getIOProvider().startIO(() -> client.loadConfigURL(server));
        }
    }
}
