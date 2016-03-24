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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import jwf.WizardPanel;

import org.json.JSONArray;
import org.json.JSONObject;

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
import de.dal33t.powerfolder.ui.StyledComboBox;
import de.dal33t.powerfolder.ui.dialog.ConfigurationLoaderDialog;
import de.dal33t.powerfolder.ui.util.IdPSelectionAction;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

@SuppressWarnings("serial")
public class LoginPanel extends PFWizardPanel {
    private static final Logger LOG = Logger.getLogger(LoginPanel.class
        .getName());

    private ServerClient client;
    private boolean showUseOS;

    private JComboBox<String> serverURLBox;
    private JLabel serverURLLabel;
    private JLabel idPLabel;
    private StyledComboBox<String> idPSelectBox;
    private boolean listLoaded;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private LinkLabel recoverPasswordLabel;
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
     * @param nextPanel
     *            the next panel to display
     * @param showUseOS
     *            if the checkbox to use Online Storage should be displayed
     */
    public LoginPanel(Controller controller, WizardPanel nextPanel,
        boolean showUseOS)
    {
        this(controller, controller.getOSClient(), nextPanel, showUseOS);
    }

    /**
     * @param controller
     * @param client
     *            the online storage client to use.
     * @param nextPanel
     *            the next panel to display
     * @param showUseOS
     *            if the checkbox to use Online Storage should be displayed
     */
    public LoginPanel(Controller controller, ServerClient client,
        WizardPanel nextPanel, boolean showUseOS)
    {
        super(controller);
        Reject.ifNull(nextPanel, "Nextpanel is null");
        this.nextPanel = nextPanel;
        this.client = client;
        this.showUseOS = showUseOS;
    }

    public boolean hasNext() {
        return client.isConnected()
            && !StringUtils.isEmpty(usernameField.getText())
            && (passwordField.getPassword() != null && passwordField
                .getPassword().length > 0)
            && (StringUtils
                .isNotBlank(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL
                    .getValue(getController())) ? listLoaded : true);
    }

    public WizardPanel next() {
        // PFC-2638: Desktop sync option
        nextPanel = DesktopSyncSetupPanel.insertStepIfAvailable(
            getController(), nextPanel, client);

        return new SwingWorkerPanel(getController(), new LoginTask(),
            Translation
                .get("wizard.login_online_storage.logging_in"),
            Translation
                .get("wizard.login_online_storage.logging_in.text"),
            nextPanel);
    }

    protected JPanel buildContent() {
        String layoutRows;

        if (StringUtils.isBlank(ConfigurationEntry.SERVER_CONNECTION_URLS
            .getValue(getController()))
            || StringUtils.isBlank(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL
                .getValue(getController())))
        {
            layoutRows = "15dlu, 7dlu, 15dlu, 7dlu, 15dlu, 3dlu, 15dlu, 3dlu, 15dlu, 34dlu, pref, 20dlu, pref, 3dlu, pref";
        } else {
            layoutRows = "15dlu, 7dlu, 15dlu, 7dlu, 15dlu, 7dlu, 15dlu, 3dlu, 15dlu, 3dlu, 15dlu, 34dlu, pref, 20dlu, pref, 3dlu, pref";
        }

        FormLayout layout = new FormLayout("50dlu, 3dlu, 110dlu, 40dlu, pref",
            layoutRows);
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        int row = 1;

        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_CONNECTION_URLS
            .getValue(getController())))
        {
            builder.add(serverURLLabel, cc.xy(1, row));
            builder.add(serverURLBox, cc.xy(3, row));
            row += 2;
        }

        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL
            .getValue(getController())))
        {
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
        serverInfoLabel = new ActionLabel(getController(), new AbstractAction()
        {
            public void actionPerformed(ActionEvent e) {
                new ConfigurationLoaderDialog(getController()).openAndWait();
            }
        });
        serverInfoLabel.setText(client.getServerString());
        serverInfoLabel.setEnabled(changeLoginAllowed);

        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_CONNECTION_URLS
            .getValue(getController())))
        {
            serverURLLabel = new JLabel(
                Translation.get("general.server"));

            String webURL = client.getWebURL();
            int selection = 0;

            String allServers = ConfigurationEntry.SERVER_CONNECTION_URLS
                .getValue(getController());
            String[] allServersArray = allServers.split(";");
            String[] serverLabels = new String[allServersArray.length];

            for (int i = 0; i < allServersArray.length; i++) {
                try {
                    String server = allServersArray[i];
                    serverLabels[i] = server.substring(0, server.indexOf("="));
                    String serverURL = server
                        .substring(server.indexOf("=") + 1);
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

        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL
            .getValue(getController())))
        {
            idPLabel = new JLabel(Translation.get("general.idp"));
            idPSelectBox = new StyledComboBox<>(new String[]{Translation.get("general.loading")});
            idPSelectBox.setEnabled(false);

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
            {
                @Override
                protected Void doInBackground() throws Exception {
                    URL url = new URL(
                        ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL
                            .getValue(getController()));
                    HttpsURLConnection con = (HttpsURLConnection) url
                        .openConnection();

                    BufferedReader is = new BufferedReader(
                        new InputStreamReader(con.getInputStream(),
                            Convert.UTF8.toString()));
                    String line = is.readLine();
                    StringBuilder body = new StringBuilder();

                    while (line != null) {
                        body.append(line);
                        line = is.readLine();
                    }

                    JSONArray resp = new JSONArray(body.toString());
                    List<String> idPList = new ArrayList<>(resp.length());

                    String lastIdP = ConfigurationEntry.SERVER_IDP_LAST_CONNECTED
                        .getValue(getController());
                    boolean lastIdPSet = false;
                    short namesOffset = 0;

                    idPSelectBox.removeAllItems();
                    if (ConfigurationEntry.SERVER_IDP_EXTERNAL_NAMES
                        .hasNonBlankValue(getController()))
                    {
                        String[] extNames = ConfigurationEntry.SERVER_IDP_EXTERNAL_NAMES
                            .getValue(getController()).split(",");

                        for (String name : extNames) {
                            if (StringUtils.isNotBlank(name)) {
                                idPSelectBox.addItem(name.trim());
                                idPList.add(name.trim());
                                namesOffset++;
                                if (!lastIdPSet && name.equals(lastIdP)) {
                                    idPSelectBox.setSelectedIndex(namesOffset - 1);
                                    lastIdPSet = true;
                                }
                            }
                        }
                    } else {
                        idPSelectBox.addItem(
                            Translation.get("wizard.login.external_users"));
                        idPList.add("ext");
                        namesOffset = 1;
                    }

                    for (int i = 0; i < resp.length(); i++) {
                        JSONObject obj = resp.getJSONObject(i);

                        String entity = obj.getString("entityID");
                        String name = obj.getJSONArray("DisplayNames")
                            .getJSONObject(0).getString("value");

                        idPSelectBox.addItem(name);
                        idPList.add(entity);

                        if (!lastIdPSet && entity.equals(lastIdP)) {
                            idPSelectBox.setSelectedIndex(i + namesOffset);
                            lastIdPSet = true;
                        }
                    }

                    if (!lastIdPSet) {
                        idPSelectBox.setSelectedIndex(0);
                        ConfigurationEntry.SERVER_IDP_LAST_CONNECTED.setValue(
                            getController(), "ext");
                        ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP
                            .setValue(getController(), "ext");
                    }

                    idPSelectBox.addActionListener(new IdPSelectionAction(
                        getController(), idPList));
                    idPSelectBox.setEnabled(true);
                    listLoaded = true;

                    updateButtons();

                    return null;
                }
            };

            worker.execute();
        }

        usernameLabel = new JLabel(LoginUtil.getUsernameLabel(getController()));
        usernameField = new JTextField();
        usernameField.addKeyListener(new MyKeyListener());
        usernameField.setEditable(changeLoginAllowed);
        passwordLabel = new JLabel(
            Translation.get("general.password") + ':');
        passwordField = new JPasswordField();
        passwordField.setEditable(changeLoginAllowed);
        passwordField.addKeyListener(new MyKeyListener());

        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL
            .getValue(getController())))
        {
            usernameField.setText(ConfigurationEntry.SERVER_CONNECT_USERNAME
                .getValue(getController()));
            passwordField.setText(new String(LoginUtil
                .deobfuscate(ConfigurationEntry.SERVER_CONNECT_PASSWORD
                    .getValue(getController()) == null
                    ? ""
                    : ConfigurationEntry.SERVER_CONNECT_PASSWORD
                        .getValue(getController()))));
       } else if (client.isConnected()) {
            usernameField.setText(client.getUsername());
            if (!client.isPasswordEmpty()) {
                passwordField.setText(client.getPasswordClearText());                
            }
        }

        // loginButton = new JButton("Login");
        // loginButton.setOpaque(false);
        // loginButton.addActionListener(new ActionListener() {
        // public void actionPerformed(ActionEvent e) {
        // Wizard wiz = (Wizard) getWizardContext().getAttribute(
        // WizardContextAttributes.WIZARD_ATTRIBUTE);
        // wiz.next();
        // }
        // });

        rememberPasswordBox = BasicComponentFactory
            .createCheckBox(
                PreferencesEntry.SERVER_REMEMBER_PASSWORD
                    .getModel(getController()),
                Translation
                    .get("wizard.login_online_storage.remember_password"));
        rememberPasswordBox.setOpaque(false);
        rememberPasswordBox.setVisible(changeLoginAllowed
            && rememberPasswordAllowed);

        recoverPasswordLabel = new LinkLabel(getController(),
            Translation
                .get("exp.wizard.webservice.recover_password"),
            client.getRecoverPasswordURL());
        recoverPasswordLabel.setVisible(client.supportsRecoverPassword());

        useOSBox = new JCheckBox(
            Translation.get("wizard.login_online_storage.no_os")); // @todo
                                                                              // "Use online storage"?
        useOSBox.setSelected(!PreferencesEntry.USE_ONLINE_STORAGE
            .getValueBoolean(getController()));
        useOSBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreferencesEntry.USE_ONLINE_STORAGE.setValue(getController(),
                    !useOSBox.isSelected());
            }
        });
        useOSBox.setOpaque(false);
        connectingLabel = SimpleComponentFactory.createLabel(Translation
            .get("wizard.login_online_storage.connecting"));
        workingBar = new JProgressBar();
        workingBar.setIndeterminate(true);
        updateOnlineStatus();
        client.addListener(new MyServerClientListner());

        // Never run forever
        getController().scheduleAndRepeat(new Runnable() {
            public void run() {
                if (!client.isConnected()) {
                    getWizard().next();
                }
            }
        }, 60000L, 10000L);
    }

    protected String getTitle() {
        return Translation.get("exp.wizard.webservice.login");
    }

    private void updateOnlineStatus() {
        boolean connected = client.isConnected();
        boolean changeLoginAllowed = ConfigurationEntry.SERVER_CONNECT_CHANGE_LOGIN_ALLOWED
            .getValueBoolean(getController());
        boolean rememberPasswordAllowed = ConfigurationEntry.SERVER_CONNECT_REMEMBER_PASSWORD_ALLOWED
            .getValueBoolean(getController());
        if (StringUtils.isNotBlank(ConfigurationEntry.SERVER_CONNECTION_URLS
            .getValue(getController())))
        {
            serverURLLabel.setVisible(true);
            serverURLBox.setVisible(true);
        }
        usernameLabel.setVisible(connected);
        usernameField.setVisible(connected);
        passwordLabel.setVisible(connected);
        passwordField.setVisible(connected);
        // loginButton.setVisible(enabled);
        rememberPasswordBox.setVisible(connected && changeLoginAllowed
            && rememberPasswordAllowed);
        recoverPasswordLabel.setVisible(connected && client.supportsRecoverPassword());

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
        if (getController().getUIController().getMainFrame().isIconifiedOrHidden())
            PathUtils.openFile(getController().getFolderRepository().getFoldersBasedir());
    }

    private class LoginTask implements Runnable {
        public void run() {
            try {
                if (!client.isConnected()) {
                    LOG.log(Level.WARNING, "Unable to connect");
                    throw new SecurityException(
                        Translation
                            .get("wizard.webservice.connect_failed"));
                }

                boolean loginOk = false;
                char[] pw = passwordField.getPassword();
                loginOk = client.login(usernameField.getText(), pw).isValid();
                LoginUtil.clear(pw);
                if (!loginOk) {
                    throw new SecurityException(
                        Translation
                            .get("online_storage.account_data"));
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
                throw new SecurityException(e.getMessage() == null
                    ? e.toString()
                    : e.getMessage());
            }
        }
    }

    private class MyServerClientListner implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
        }

        public void login(ServerClientEvent event) {
        }

        public void serverConnected(ServerClientEvent event) {
            usernameField.setText(client.getUsername());
            passwordField.setText(client.getPasswordClearText());
            updateOnlineStatus();
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
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
            String serversList = ConfigurationEntry.SERVER_CONNECTION_URLS
                .getValue(getController());

            // find the item, skip it an the equals-sign
            int begin = serversList.indexOf(item) + item.length() + 1;
            int end = serversList.indexOf(";", begin);

            if (end == -1) {
                end = serversList.length();
            }

            final String server = serversList.substring(begin, end);
            getController().getIOProvider().startIO(new Runnable() {
                @Override
                public void run() {
                    client.loadConfigURL(server);
                }
            });
        }
    }
}
