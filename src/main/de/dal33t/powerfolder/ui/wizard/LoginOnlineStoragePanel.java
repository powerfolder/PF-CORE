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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import jwf.WizardPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.debug.FormDebugPanel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

public class LoginOnlineStoragePanel extends PFWizardPanel {
    private static final Logger LOG = Logger
        .getLogger(LoginOnlineStoragePanel.class.getName());

    private ServerClient client;
    private boolean showUseOS;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel connectingLabel;
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
    public LoginOnlineStoragePanel(Controller controller,
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
    public LoginOnlineStoragePanel(Controller controller, ServerClient client,
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
        boolean loginOk = false;
        try {
            loginOk = client.login(usernameField.getText(),
                new String(passwordField.getPassword())).isValid();
            if (!loginOk) {
                DialogFactory.genericDialog(getController(), Translation
                    .getTranslation("wizard.error_title"), Translation
                    .getTranslation("online_storage.account_data"),
                    GenericDialogType.INFO);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "Problem logging in", e);
            DialogFactory.genericDialog(getController(), Translation
                .getTranslation("wizard.error_title"), Translation
                .getTranslation("online_storage.general_error",
                    e.getMessage() == null ? e.toString() : e.getMessage()),
                GenericDialogType.INFO);
        }
        return loginOk;
    }

    public WizardPanel next() {
        return nextPanel;
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
            "right:60dlu, 3dlu, 80dlu, pref:grow",
            "pref, 6dlu, 15dlu, 3dlu, 15dlu, 3dlu, 15dlu, 3dlu, pref, 3dlu, pref, 20dlu, pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;
        builder.addLabel(Translation
            .getTranslation("wizard.webservice.enter_account"), cc.xyw(1, row,
            4));
        row += 2;

        // usernameField and connectingLabel have the same slot.
        builder.add(usernameLabel, cc.xy(1, row));
        builder.add(usernameField, cc.xy(3, row));
        builder.add(connectingLabel, cc.xyw(1, row, 3));
        row += 2;

        // passwordField and workingBar have the same slot.
        builder.add(passwordLabel, cc.xy(1, row));
        builder.add(passwordField, cc.xy(3, row));
        builder.add(workingBar, cc.xyw(1, row, 3));
        row += 2;

        builder.add(rememberPasswordBox, cc.xyw(3, row, 2));
        row += 2;

        if (client.supportsWebRegistration()) {
            builder.add(new LinkLabel(getController(), Translation
                .getTranslation("pro.wizard.activation.register_now"), client
                .getRegisterURL()).getUiComponent(), cc.xyw(1, row, 4));
            row += 2;

            builder.add(new LinkLabel(getController(), Translation
                .getTranslation("wizard.webservice.recover_password"),
                getController().getOSClient().getWebURL()).getUiComponent(), cc
                .xyw(1, row, 4));
            row += 2;
        }

        if (showUseOS) {
            LinkLabel link = new LinkLabel(getController(), Translation
                .getTranslation("wizard.webservice.learn_more"),
                ConfigurationEntry.PROVIDER_ABOUT_URL.getValue(getController()));
            builder.add(link.getUiComponent(), cc.xyw(1, row, 4));
            row += 2;

            builder.add(useOSBox, cc.xyw(1, row, 4));
            row += 2;
        }

        return builder.getPanel();
    }

    // UI building ************************************************************

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
        // FIXME Use separate account stores for diffrent servers?
        usernameLabel = new JLabel(Translation
            .getTranslation("wizard.webservice.username"));
        usernameField = new JTextField();
        usernameField.addKeyListener(new MyKeyListener());
        passwordLabel = new JLabel(Translation
            .getTranslation("wizard.webservice.password"));
        passwordField = new JPasswordField();

        if (client.isConnected()) {
            usernameField.setText(client.getUsername());
            passwordField.setText(client.getPassword());
        }

        rememberPasswordBox = BasicComponentFactory
            .createCheckBox(
                PreferencesEntry.SERVER_REMEMBER_PASSWORD
                    .getModel(getController()),
                Translation
                    .getTranslation("wizard.login_online_storage.remember_password"));
        rememberPasswordBox.setOpaque(false);
        useOSBox = BasicComponentFactory.createCheckBox(getController()
            .getUIController().getApplicationModel().getUseOSModel(),
            Translation.getTranslation("wizard.login_online_storage.no_os"));
        connectingLabel = SimpleComponentFactory.createLabel(Translation
            .getTranslation("wizard.login_online_storage.connecting"));
        workingBar = new JProgressBar();
        workingBar.setIndeterminate(true);
        updateOnlineStatus();
        client.addListener(new MyServerClientListner());
    }

    protected JComponent getPictoComponent() {
        return new JLabel(Icons.getIconById(Icons.WEB_SERVICE_PICTO));
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
        rememberPasswordBox.setVisible(enabled);
        connectingLabel.setVisible(!enabled);
        workingBar.setVisible(!enabled);

        if (enabled) {
            usernameLabel.requestFocus();
        }
        updateButtons();
    }

    private class MyServerClientListner implements ServerClientListener {

        public void accountUpdated(ServerClientEvent event) {
        }

        public void login(ServerClientEvent event) {
        }

        public void serverConnected(ServerClientEvent event) {
            usernameField.setText(client.getUsername());
            passwordField.setText(client.getPassword());
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

}
