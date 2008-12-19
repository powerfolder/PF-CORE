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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import jwf.WizardPanel;

import org.apache.commons.lang.StringUtils;

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
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

public class LoginOnlineStoragePanel extends PFWizardPanel {
    private static final Logger LOG = Logger
        .getLogger(LoginOnlineStoragePanel.class.getName());

    private ServerClient client;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel connectingLabel;
    private JCheckBox remindPasswordBox;
    private WizardPanel nextPanel;
    private DefaultFolderWizardHelper defaultFolderHelper;

    private boolean entryRequired;

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
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Problem logging in", e);
            list.add(Translation.getTranslation("online_storage.general_error",
                e.getMessage()));
        }
        return loginOk;
    }

    public WizardPanel next() {
        return defaultFolderHelper.next(nextPanel, getWizardContext());
    }

    protected JPanel buildContent() {
        FormLayout layout = new FormLayout(
            "$wlabel, $lcg, $wfield, 0:g",
            "pref, 10dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 15dlu, pref, 5dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.enter_account"), cc.xyw(1, 1, 4));

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.username"), cc.xy(1, 3));
        builder.add(usernameField, cc.xy(3, 3));
        builder.add(connectingLabel, cc.xy(3, 3));

        builder.addLabel(Translation
            .getTranslation("wizard.webservice.password"), cc.xy(1, 5));
        builder.add(passwordField, cc.xy(3, 5));

        builder.add(remindPasswordBox, cc.xy(3, 7));

        if (client.getRegisterURL() != null) {
            builder.add(new LinkLabel(Translation
                .getTranslation("pro.wizard.activation.register_now"), client
                .getRegisterURL()), cc.xy(3, 9));

            LinkLabel link = new LinkLabel(Translation
                .getTranslation("wizard.webservice.learn_more"),
                "http://www.powerfolder.com/online_storage_features.html");
            builder.add(link, cc.xyw(1, 11, 4));
        }

        // Default setup
        builder.add(defaultFolderHelper.getUIComponent(), cc.xyw(1, 13, 4));

        return builder.getPanel();
    }

    // UI building ************************************************************

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
        // FIXME Use separate account stores for diffrent servers?
        ValueModel usernameModel = new ValueHolder(client.getUsername(), true);
        usernameField = BasicComponentFactory.createTextField(usernameModel);
        passwordField = new JPasswordField(client.getPassword());
        updateButtons();
        usernameModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });
        remindPasswordBox = BasicComponentFactory.createCheckBox(
            PreferencesEntry.SERVER_REMIND_PASSWORD.getModel(getController()),
            Translation
                .getTranslation("wizard.login_online_storage.remind_password"));
        remindPasswordBox.setOpaque(false);
        connectingLabel = SimpleComponentFactory.createLabel(Translation
            .getTranslation("wizard.login_online_storage.connecting"));
        updateOnlineStatus();
        client.addListener(new MyServerClientListner());

        defaultFolderHelper = new DefaultFolderWizardHelper(getController(),
            client);
    }

    protected JComponent getPictoComponent() {
        return new JLabel(Icons.WEB_SERVICE_PICTO);
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

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}
