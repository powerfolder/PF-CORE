/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.util;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.ui.StyledComboBox;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.SwingWorker;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The institution (IdP) chooser of the login wizard. The list comes from the Shibboleth discovery feed of the
 * server.
 * <p>
 * PFC-3451: The feed is fetched with the shared HTTP client (proxy settings, timeouts) instead of a bare
 * connection that could hang for minutes right after system start. A failed fetch no longer leaves the box
 * silently disabled: it says so, retries on its own while the wizard is open and on every click into the box.
 * All changes to the combo box model happen on the EDT.
 */
public class IdPSelectionBox extends StyledComboBox<String> {
    private static final Logger LOG = Logger.getLogger(IdPSelectionBox.class.getName());
    private static final int HTTP_TIMEOUT_MS = 10 * 1000;
    private static final int RETRIEVE_ATTEMPTS = 3;
    private static final int AUTO_RETRY_DELAY_MS = 5 * 1000;

    private final Controller controller;
    /** Entity ids, index-aligned with the items of the box. Only touched on the EDT. */
    private final List<String> idPList;
    private final List<String> samlIdPList;
    private boolean listLoaded;
    private boolean loading;
    private boolean browserLoginOpened;

    public IdPSelectionBox(Controller controller) {
        super(new String[]{Translation.get("general.loading")});
        Reject.ifNull(controller, "Controller");

        this.controller = controller;
        this.idPList = new ArrayList<>();
        this.samlIdPList = new ArrayList<>();
        this.listLoaded = false;
        setEnabled(false);
        addActionListener(new IdPSelectionAction());
        addPopupMenuListener(new RetryOnOpen());
        load();
    }

    public boolean isSAMLIDPSelected() {
        int index = getSelectedIndex();
        if (index < 0 || index >= idPList.size()) {
            return false;
        }
        String entityID = idPList.get(index);
        return entityID != null && samlIdPList.contains(entityID);
    }

    public boolean isListLoaded() {
        return listLoaded;
    }

    public boolean isBrowserLoginOpened() {
        return browserLoginOpened;
    }

    /**
     * Fetches the list in the background. Does nothing while a fetch is running or once the list is there.
     */
    private void load() {
        if (loading || listLoaded) {
            return;
        }
        loading = true;
        new Initializer().execute();
    }

    private void showLoadFailed() {
        removeAllItems();
        idPList.clear();
        samlIdPList.clear();
        addItem(Translation.get("login.saml.idp_list_failed"));
        setSelectedIndex(0);
        // Enabled, so that a click into the box can start the next attempt
        setEnabled(true);
        if (isShowing()) {
            Timer retry = new Timer(AUTO_RETRY_DELAY_MS, e -> {
                if (isShowing()) {
                    load();
                }
            });
            retry.setRepeats(false);
            retry.start();
        }
    }

    private void showList(List<String> names, List<String> entityIDs, List<String> samlEntityIDs) {
        removeAllItems();
        idPList.clear();
        samlIdPList.clear();
        idPList.addAll(entityIDs);
        samlIdPList.addAll(samlEntityIDs);
        for (String name : names) {
            addItem(name);
        }
        setSelectedIndex(0);
        ConfigurationEntry.SERVER_IDP_LAST_CONNECTED.setValue(controller, ServerClient.SAML_EXTERNAL_NON_SAML_USERS);
        ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP.setValue(controller, ServerClient.SAML_EXTERNAL_NON_SAML_USERS);
        listLoaded = true;
        setEnabled(true);
    }

    /**
     * A click into the box while the list is missing starts the next attempt.
     */
    private class RetryOnOpen implements PopupMenuListener {
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            if (!listLoaded) {
                load();
            }
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
        }
    }

    /**
     * The fetched list, built in the background and handed to the EDT in one piece.
     */
    private static class IdPEntries {
        final List<String> names = new ArrayList<>();
        final List<String> entityIDs = new ArrayList<>();
        final List<String> samlEntityIDs = new ArrayList<>();

        void add(String name, String entityID, boolean saml) {
            names.add(name);
            entityIDs.add(entityID);
            if (saml) {
                samlEntityIDs.add(entityID);
            }
        }
    }

    private class Initializer extends SwingWorker<IdPEntries, Void> {

        private JSONArray retrieve0() throws IOException, JSONException {
            String url = ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL.getValue(controller);
            RequestConfig timeouts = RequestConfig.custom().setConnectTimeout(HTTP_TIMEOUT_MS)
                .setConnectionRequestTimeout(HTTP_TIMEOUT_MS).setSocketTimeout(HTTP_TIMEOUT_MS).build();
            // PFC-2669: The shared builder knows the proxy settings
            HttpClientBuilder builder = Util.createHttpClientBuilder(controller).setDefaultRequestConfig(timeouts);
            HttpClient client = builder.build();
            HttpResponse response = client.execute(new HttpGet(url));
            int status = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity(), Convert.UTF8);
            if (status != 200) {
                throw new IOException("HTTP " + status + " from " + url);
            }
            return new JSONArray(body);
        }

        private JSONArray retrieve() throws IOException, JSONException {
            Exception lastException = null;
            for (int i = 0; i < RETRIEVE_ATTEMPTS; i++) {
                try {
                    return retrieve0();
                } catch (IOException | JSONException e) {
                    lastException = e;
                    LOG.fine("IdP list: attempt " + (i + 1) + " failed. " + e);
                }
                Waiter.waitRandom(1000);
            }
            if (lastException instanceof IOException) {
                throw (IOException) lastException;
            }
            throw (JSONException) lastException;
        }

        @Override
        protected IdPEntries doInBackground() throws Exception {
            JSONArray idps = retrieve();
            IdPEntries entries = new IdPEntries();

            // PFS-2006
            entries.add(Translation.get("wizard.login_online_storage.pre_selection_entry"), "", false);

            if (ConfigurationEntry.SERVER_IDP_EXTERNAL_NAMES.hasNonBlankValue(controller)) {
                String[] extNames = ConfigurationEntry.SERVER_IDP_EXTERNAL_NAMES.getValue(controller).split(",");
                for (String name : extNames) {
                    if (StringUtils.isBlank(name)) {
                        continue;
                    }
                    if (name.startsWith("!")) {
                        name = name.substring(1);
                    }
                    entries.add(name.trim(), name.trim(), false);
                }
            } else {
                entries.add(Translation.get("wizard.login.external_users"), ServerClient.SAML_EXTERNAL_NON_SAML_USERS,
                    false);
            }

            for (int i = 0; i < idps.length(); i++) {
                JSONObject obj = idps.getJSONObject(i);
                String entity = obj.getString("entityID");
                String name = obj.getJSONArray("DisplayNames").getJSONObject(0).getString("value");
                entries.add(name, entity, true);
            }
            return entries;
        }

        @Override
        protected void done() {
            loading = false;
            IdPEntries entries;
            try {
                entries = get();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "IdP list: unable to load "
                    + ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL.getValue(controller) + ". " + e.getCause());
                showLoadFailed();
                return;
            }
            showList(entries.names, entries.entityIDs, entries.samlEntityIDs);
        }
    }

    private class IdPSelectionAction implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (!listLoaded) {
                return;
            }
            int index = getSelectedIndex();
            if (index < 0 || index >= idPList.size()) {
                return;
            }
            String entityID = idPList.get(index);
            new IdPSelectionWorker(entityID).execute();
        }

        private class IdPSelectionWorker extends SwingWorker<Void, Void> {
            private final String entityID;

            private IdPSelectionWorker(String entityID) {
                this.entityID = entityID;
            }

            @Override
            protected Void doInBackground() {
                ConfigurationEntry.SERVER_IDP_LAST_CONNECTED.setValue(controller, entityID);
                String externalNames = ConfigurationEntry.SERVER_IDP_EXTERNAL_NAMES.getValue(controller);

                if (StringUtils.isBlank(entityID)) {
                    return null;
                } else if (ServerClient.SAML_EXTERNAL_NON_SAML_USERS.equals(entityID)
                        || (StringUtils.isNotBlank(externalNames) && externalNames.contains(entityID))) {
                    ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP
                            .setValue(controller, ServerClient.SAML_EXTERNAL_NON_SAML_USERS);
                    return null;
                }

                int port = controller.getRconManager().getPort();
                if (port > 0) {
                    browserLoginOpened = true;
                    String nodeConsumeTokenURL = "http://localhost:" + port + Constants.LOGIN_URI;
                    openSAMLLoginInBrowser(entityID, nodeConsumeTokenURL);
                } else {
                    retrieveECP(entityID);
                    DialogFactory.genericDialog(controller, Translation.get("login.saml.browser_init_problem.title"),
                            Translation.get("login.saml.browser_init_problem.message"), GenericDialogType.WARN);
                    LOG.warning("Unable to provide browser based SAML login. Trying to login via ECP.");
                }

                return null;
            }
        }
    }

    private void openSAMLLoginInBrowser(String entityID, String nodeConsumeTokenURL) {
        String spConsumeURL = ConfigurationEntry.SERVER_WEB_URL.getValue(controller) + Constants.LOGIN_SHIBBOLETH_BROWSER_URI;
        spConsumeURL += "?nodeID=" + URLEncoder.encode(controller.getMySelf().getId(), Convert.UTF8);
        spConsumeURL += "&nodeNick=" + URLEncoder.encode(controller.getMySelf().getNick(), Convert.UTF8);
        spConsumeURL += "&nodeConsumeTokenURL=" +  URLEncoder.encode(nodeConsumeTokenURL, Convert.UTF8);

        String idpWebLoginURL = ConfigurationEntry.SERVER_WEB_URL.getValue(controller) + "/Shibboleth.sso/Login";
        idpWebLoginURL += "?SAMLDS=1&";
        idpWebLoginURL += "entityID=" + URLEncoder.encode(entityID, Convert.UTF8);
        idpWebLoginURL += "&target=" + URLEncoder.encode(spConsumeURL, Convert.UTF8);

        try {
            BrowserLauncher.openURL(idpWebLoginURL);
        } catch (IOException ex) {
            // PFC-3494: On some systems (e.g. KDE-based Linux) no browser can be
            // launched automatically. The login still completes as soon as the
            // URL is opened in any browser (the token is posted back to the
            // local client), so instead of failing we let the user copy the link
            // and open it manually.
            LOG.log(Level.WARNING, "Could not open browser for SAML login,"
                    + " offering copy-link fallback", ex);
            showBrowserFallbackDialog(idpWebLoginURL);
            return;
        }

        DialogFactory.genericDialog(controller, Translation.get("login.saml.browser_login_ongoing.title"),
                Translation.get("login.saml.browser_login_ongoing.message"), new String[]{Translation
                .get("general.ok")}, 0, GenericDialogType.INFO);
    }

    /**
     * Shows a dialog offering the SAML login URL in a selectable text field
     * plus a copy-to-clipboard button, for the case where no system browser
     * could be launched automatically. The login completes normally once the
     * user opens the copied link in any browser. See PFC-3494.
     */
    private void showBrowserFallbackDialog(final String url) {
        final JTextField urlField = new JTextField(url);
        urlField.setEditable(false);
        urlField.setCaretPosition(0);

        final JButton copyButton = new JButton(
                Translation.get("login.saml.browser_copy_link.copy_button"));
        copyButton.addActionListener(e -> {
            Util.setClipboardContents(url);
            urlField.selectAll();
            copyButton.setText(Translation.get("login.saml.browser_copy_link.copied_button"));
        });

        JLabel message = new JLabel("<html><body style='width: 300px'>"
                + Translation.get("login.saml.browser_copy_link.message") + "</body></html>");

        // "180dlu" gives the URL field a fixed, sensible width instead of
        // stretching across the whole dialog.
        FormLayout layout = new FormLayout("180dlu, 3dlu, pref", "pref, 7dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(message, cc.xyw(1, 1, 3));
        builder.add(urlField, cc.xy(1, 3));
        builder.add(copyButton, cc.xy(3, 3));

        DialogFactory.genericDialog(controller,
                Translation.get("login.saml.browser_copy_link.title"), builder.getPanel(),
                new String[]{Translation.get("general.ok")}, 0, GenericDialogType.WARN);
    }

    private void retrieveECP(String entityID) {
        String idpLookupURL = ConfigurationEntry.SERVER_WEB_URL.getValue(controller)
                + "/api/idpd?entityID=" + URLEncoder.encode(entityID, Convert.UTF8);

        HttpGet getBindingURL = new HttpGet(idpLookupURL);
        // PFC-2669:
        HttpClientBuilder builder = Util.createHttpClientBuilder(controller);
        HttpClient client = builder.build();

        try {
            HttpResponse httpResponse = client.execute(getBindingURL);
            String ecpURL = EntityUtils.toString(httpResponse.getEntity());
            ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP.setValue(controller, ecpURL);
            LOG.info(entityID + ": ECP URL is " + ecpURL);
        } catch (IOException e1) {
            LOG.warning(e1.toString());
        }

        controller.saveConfig();
    }
}
