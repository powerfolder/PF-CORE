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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import javax.swing.SwingWorker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IdPSelectionBox extends StyledComboBox<String> {
    private static final Logger LOG = Logger.getLogger(IdPSelectionBox.class.getName());
    private final Controller controller;
    private final List<String> idPList;
    private final List<String> samlIdPList;
    private boolean listLoaded;
    private boolean browserLoginOpened;

    public IdPSelectionBox(Controller controller) {
        super(new String[]{Translation.get("general.loading")});
        Reject.ifNull(controller, "Controller");

        this.controller = controller;
        this.idPList = new ArrayList<>();
        this.samlIdPList = new ArrayList<>();
        this.listLoaded = false;
        setEnabled(false);
        new Initializer().execute();
    }

    public boolean isSAMLIDPSelected() {
        int index = getSelectedIndex();
        String entityID = idPList.get(index);
        return entityID != null && samlIdPList.contains(entityID);
    }

    private class Initializer extends SwingWorker<Void, Void> {
        private JSONArray retrieve0() throws IOException, JSONException {
            URL url = new URL(ConfigurationEntry.SERVER_IDP_DISCO_FEED_URL.getValue(controller));

            HttpURLConnection con;
            if (url.toString().startsWith("https")) {
                con = (HttpsURLConnection) url.openConnection();
            } else {
                con = (HttpURLConnection) url.openConnection();
            }

            BufferedReader is = new BufferedReader(new InputStreamReader(con.getInputStream(), Convert.UTF8));
            String line = is.readLine();
            StringBuilder body = new StringBuilder();

            while (line != null) {
                body.append(line);
                line = is.readLine();
            }

            return new JSONArray(body.toString());
        }

        private JSONArray retrieve() {
            Exception lastException = null;
            for (int i = 0; i < 10; i++) {
                try {
                    return retrieve0();
                } catch (Exception e) {
                    lastException = e;
                }
                Waiter.waitRandom(500);
            }
            throw new RuntimeException(lastException);
        }

        public void addEntry(String item) {
            SwingUtilities.invokeLater(() -> addItem(item));
        }

        public void setSelectedIndex(int i) {
            SwingUtilities.invokeLater(() -> setSelectedIndex(i));
        }

        @Override
        protected Void doInBackground() throws Exception {
            JSONArray idps = retrieve();
            removeAllItems();

            // PFS-2006
            addEntry(Translation.get("wizard.login_online_storage.pre_selection_entry"));
            idPList.add(0, "");

            if (ConfigurationEntry.SERVER_IDP_EXTERNAL_NAMES.hasNonBlankValue(controller)) {
                String[] extNames = ConfigurationEntry.SERVER_IDP_EXTERNAL_NAMES.getValue(controller).split(",");

                for (String name : extNames) {
                    if (StringUtils.isNotBlank(name)) {

                        if (name.startsWith("!")) {
                            name = name.substring(1);
                        }

                        addEntry(name.trim());
                        idPList.add(name.trim());
                    }
                }
            } else {
                addEntry(Translation.get("wizard.login.external_users"));
                idPList.add(ServerClient.SAML_EXTERNAL_NON_SAML_USERS);
            }

            for (int i = 0; i < idps.length(); i++) {
                JSONObject obj = idps.getJSONObject(i);

                String entity = obj.getString("entityID");
                String name = obj.getJSONArray("DisplayNames").getJSONObject(0).getString("value");

                addEntry(name);
                idPList.add(entity);
                samlIdPList.add(entity);
            }

            setSelectedIndex(0);
            ConfigurationEntry.SERVER_IDP_LAST_CONNECTED.setValue(controller, ServerClient.SAML_EXTERNAL_NON_SAML_USERS);
            ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP.setValue(controller, ServerClient.SAML_EXTERNAL_NON_SAML_USERS);

            addActionListener(new IdPSelectionAction());
            listLoaded = true;
            return null;
        }

        @Override
        protected void done() {
            if (listLoaded) {
                setEnabled(true);
            }
        }
    }

    public boolean isListLoaded() {
        return listLoaded;
    }

    public boolean isBrowserLoginOpened() {
        return browserLoginOpened;
    }

    private class IdPSelectionAction implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            SwingWorker<Void, Void> worker = new IdPSelectionWorker();
            worker.execute();
        }

        private class IdPSelectionWorker extends SwingWorker<Void, Void> {
            @Override
            protected Void doInBackground() {
                int index = getSelectedIndex();
                String entityID = idPList.get(index);

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
