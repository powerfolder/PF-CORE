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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

import javax.swing.*;

import jwf.WizardPanel;
import jwf.Wizard;

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
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.action.BaseAction;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

public class LoginOnlineStoragePanel extends PFWizardPanel {
    private static final Logger LOG = Logger
        .getLogger(LoginOnlineStoragePanel.class.getName());

    private ServerClient client;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel connectingLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JProgressBar workingBar;
    private JCheckBox rememberPasswordBox;
    private WizardPanel nextPanel;

    private boolean entryRequired;
    private boolean noThanks;

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
        Reject.ifNull(nextPanel, "Nextpanel is null");
        this.nextPanel = nextPanel;
        this.entryRequired = entryRequired;
        this.client = client;
    }

    public boolean hasNext() {
        if (entryRequired) {
            return client.isConnected() &&
                    !StringUtils.isEmpty(usernameField.getText());  
        } else {
            return true;
        }
    }

    protected void afterDisplay() {
        noThanks = false;
    }

    public boolean validateNext() {
        if (noThanks ||
                !entryRequired && StringUtils.isEmpty(usernameField.getText()))
        {
            return true;
        }
        boolean loginOk = false;
        try {
            loginOk = client.login(usernameField.getText(),
                new String(passwordField.getPassword())).isValid();
            if (!loginOk) {
                DialogFactory.genericDialog(getController(),
                        Translation.getTranslation("wizard.error_title"),
                        Translation.getTranslation("online_storage.account_data"),
                        GenericDialogType.INFO);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Problem logging in", e);
            DialogFactory.genericDialog(getController(),
                    Translation.getTranslation("wizard.error_title"),
                    Translation.getTranslation("online_storage.general_error"),
                    GenericDialogType.INFO);
        }
        return loginOk;
    }

    public WizardPanel next() {
        return nextPanel;
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, 140dlu, pref:grow",
            "pref, 6dlu, pref, 3dlu, pref, 3dlu, pref, 10dlu, pref, 10dlu, pref, 10dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        if (entryRequired) {
            builder.addLabel(Translation
                .getTranslation("wizard.webservice.enter_account"),
                    cc.xyw(1, 1, 4));
        } else {
            builder.addLabel(Translation
                .getTranslation("wizard.webservice.enter_account_optional"),
                    cc.xyw(1, 1, 4));
        }

        builder.add(usernameLabel, cc.xy(1, 3));

        // usernameField and connectingLabel have the same slot.
        builder.add(usernameField, cc.xy(3, 3));
        builder.add(connectingLabel, cc.xyw(3, 3, 2));

        builder.add(passwordLabel, cc.xy(1, 5));

        // passwordField and workingBar have the same slot.
        builder.add(passwordField, cc.xy(3, 5));
        builder.add(workingBar, cc.xy(3, 5));

        builder.add(rememberPasswordBox, cc.xyw(3, 7, 2));

        int row = 9;

        if (!entryRequired) {
            builder.add(new ActionLabel(getController(),
                    new MySkipLoginAction(getController())).getUIComponent(),
                    cc.xyw(1, row, 4));
            row += 2;
        }

        if (getController().getBranding().supportWeb()
                && client.getRegisterURL() != null) {
            builder.add(new LinkLabel(getController(), Translation
                    .getTranslation("pro.wizard.activation.register_now"), client
                    .getRegisterURL()).getUiComponent(), cc.xyw(1, row, 4));
            row += 2;

            LinkLabel link = new LinkLabel(getController(), Translation
                    .getTranslation("wizard.webservice.learn_more"),
                    ConfigurationEntry.PROVIDER_ABOUT_URL.getValue(getController()));
            builder.add(link.getUiComponent(), cc.xyw(1, row, 4));
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
        usernameLabel = new JLabel(Translation.getTranslation(
                "wizard.webservice.username"));
        usernameField = new JTextField();
        usernameField.addKeyListener(new MyKeyListener());
        passwordLabel = new JLabel(Translation.getTranslation(
                "wizard.webservice.password"));
        passwordField = new JPasswordField();

        if (client.isConnected()) {
            usernameField.setText(client.getUsername());
            passwordField.setText(client.getPassword());
        }

        rememberPasswordBox = BasicComponentFactory.createCheckBox(
            PreferencesEntry.SERVER_REMEMBER_PASSWORD.getModel(getController()),
            Translation.getTranslation(
                    "wizard.login_online_storage.remember_password"));
        rememberPasswordBox.setOpaque(false);
        connectingLabel = SimpleComponentFactory.createLabel(Translation
            .getTranslation("wizard.login_online_storage.connecting"));
        workingBar = new JProgressBar();
        workingBar.setIndeterminate(true);
        updateOnlineStatus();
        client.addListener(new MyServerClientListner());
    }

    protected JComponent getPictoComponent() {
        return new JLabel(Icons.WEB_SERVICE_PICTO);
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

    private class MySkipLoginAction extends BaseAction {

        private MySkipLoginAction(Controller controller) {
            super("action_skip_login", controller);
        }

        public void actionPerformed(ActionEvent e) {
            noThanks = true;

            // Use is not interested in OS. Hide OS stuff in UI.
            PreferencesEntry.USE_ONLINE_STORAGE.setValue(getController(), false);
            
            Wizard wizard = (Wizard) getWizardContext().getAttribute(
                Wizard.WIZARD_ATTRIBUTE);
            wizard.next();
        }
    }
}
