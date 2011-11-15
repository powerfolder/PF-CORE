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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import jwf.WizardPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.AbstractConverter;
import com.jgoodies.binding.value.ValueModel;
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
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.LoginUtil;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.ConfigurationLoaderDialog;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

@SuppressWarnings("serial")
public class LoginPanel extends PFWizardPanel {
    private static final Logger LOG = Logger
        .getLogger(LoginPanel.class.getName());

    private ServerClient client;
    private boolean showUseOS;

    private JTextField usernameField;
    private JPasswordField passwordField;
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
    public LoginPanel(Controller controller,
        WizardPanel nextPanel, boolean showUseOS)
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
            && !StringUtils.isEmpty(usernameField.getText());
    }

    protected void afterDisplay() {
    }

    public boolean validateNext() {
        return true;
    }

    public WizardPanel next() {
        WizardPanel loginWorkerPanel = new SwingWorkerPanel(getController(),
            new LoginTask(),
            Translation
                .getTranslation("wizard.login_online_storage.logging_in"),
            Translation
                .getTranslation("wizard.login_online_storage.logging_in.text"),
            nextPanel);
        return loginWorkerPanel;
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout("40dlu, 3dlu, 80dlu, 40dlu, pref",
            "15dlu, 7dlu, 15dlu, 3dlu, 15dlu, 3dlu, pref, 20dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        int row = 1;

        // usernameField and connectingLabel have the same slot.
        builder.add(usernameLabel, cc.xy(1, row));
        builder.add(usernameField, cc.xy(3, row));
        builder.add(connectingLabel, cc.xyw(1, row, 4));
        if (client.supportsWebRegistration()) {
            LinkLabel signupLabel = new LinkLabel(getController(),
                Translation
                    .getTranslation("pro.wizard.activation.register_now"),
                client.getRegisterURL());
            signupLabel.convertToBigLabel();
            builder.add(signupLabel.getUIComponent(), cc.xy(5, row));
        }
        row += 2;

        // passwordField and workingBar have the same slot.
        builder.add(passwordLabel, cc.xy(1, row));
        builder.add(passwordField, cc.xy(3, row));
        builder.add(workingBar, cc.xyw(1, row, 3));

        if (client.supportsRecoverPassword()) {
            LinkLabel recoverPasswordLabel = new LinkLabel(getController(),
                Translation
                    .getTranslation("wizard.webservice.recover_password"),
                client.getRecoverPasswordURL());
            recoverPasswordLabel.convertToBigLabel();
            builder.add(recoverPasswordLabel.getUIComponent(), cc.xy(5, row));
        }

        row += 2;
        builder.add(rememberPasswordBox, cc.xyw(3, row, 2));
        row += 2;
        builder.add(serverLabel, cc.xy(1, row));
        builder.add(serverInfoLabel.getUIComponent(), cc.xyw(3, row, 2));
        row += 2;

        if (showUseOS) {
            builder.add(useOSBox, cc.xyw(1, row, 4));
            row += 2;
            LinkLabel link = new LinkLabel(getController(),
                Translation.getTranslation("wizard.webservice.learn_more"),
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
        serverLabel = new JLabel(Translation.getTranslation("general.server"));
        serverInfoLabel = new ActionLabel(getController(), new AbstractAction()
        {
            public void actionPerformed(ActionEvent e) {
                new ConfigurationLoaderDialog(getController()).openAndWait();
            }
        });
        serverInfoLabel.setText(client.getServerString());
        serverInfoLabel.setEnabled(changeLoginAllowed);

        usernameLabel = new JLabel(LoginUtil.getUsernameLabel(getController()));
        usernameField = new JTextField();
        usernameField.addKeyListener(new MyKeyListener());
        usernameField.setEditable(changeLoginAllowed);
        passwordLabel = new JLabel(
            Translation.getTranslation("general.password") + ":");
        passwordField = new JPasswordField();
        passwordField.setEditable(changeLoginAllowed);

        if (client.isConnected()) {
            usernameField.setText(client.getUsername());
            passwordField.setText(client.getPasswordClearText());
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
                    .getTranslation("wizard.login_online_storage.remember_password"));
        rememberPasswordBox.setOpaque(false);
        rememberPasswordBox.setEnabled(changeLoginAllowed);

        useOSBox = BasicComponentFactory.createCheckBox(
            new BooleanNotConverter(getController().getUIController()
                .getApplicationModel().getUseOSModel()),
            Translation.getTranslation("wizard.login_online_storage.no_os"));
        useOSBox.setOpaque(false);
        connectingLabel = SimpleComponentFactory.createLabel(Translation
            .getTranslation("wizard.login_online_storage.connecting"));
        workingBar = new JProgressBar();
        workingBar.setIndeterminate(true);
        updateOnlineStatus();
        client.addListener(new MyServerClientListner());
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.webservice.login");
    }

    private void updateOnlineStatus() {
        boolean enabled = client.isConnected();
        usernameLabel.setVisible(enabled);
        usernameField.setVisible(enabled);
        passwordLabel.setVisible(enabled);
        passwordField.setVisible(enabled);
        // loginButton.setVisible(enabled);
        rememberPasswordBox.setVisible(enabled);
        connectingLabel.setVisible(!enabled);
        workingBar.setVisible(!enabled);

        if (getController().getOSClient().showServerInfo()) {
            serverLabel.setVisible(true);
            serverInfoLabel.getUIComponent().setVisible(true);
            serverInfoLabel.setText(client.getServerString());
        } else {
            serverLabel.setVisible(false);
            serverInfoLabel.getUIComponent().setVisible(false);
        }

        if (enabled) {
            usernameLabel.requestFocus();
        }
        updateButtons();
    }

    private final class LoginTask implements Runnable {
        public void run() {
            boolean loginOk = false;
            try {
                char[] pw = passwordField.getPassword();
                loginOk = client.login(usernameField.getText(), pw).isValid();
                LoginUtil.clear(pw);
                if (!loginOk) {
                    throw new SecurityException(
                        Translation
                            .getTranslation("online_storage.account_data"));
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Problem logging in", e);
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

    private final class BooleanNotConverter extends AbstractConverter {
        private BooleanNotConverter(ValueModel subject) {
            super(subject);
        }

        @Override
        public Object convertFromSubject(Object b) {
            return !(Boolean) b;
        }

        public void setValue(Object b) {
            subject.setValue(!(Boolean) b);
        }
    }
}
