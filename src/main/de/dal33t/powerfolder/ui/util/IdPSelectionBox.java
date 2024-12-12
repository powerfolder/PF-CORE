package de.dal33t.powerfolder.ui.util;

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
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
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
import java.util.logging.Logger;

public class IdPSelectionBox extends StyledComboBox<String> {
    private static final Logger LOG = Logger.getLogger(IdPSelectionBox.class.getName());
    private final Controller controller;
    private final List<String> idPList;
    private boolean listLoaded;
    private boolean browserLoginOpened;

    public IdPSelectionBox(Controller controller) {
        super(new String[]{Translation.get("general.loading")});
        Reject.ifNull(controller, "Controller");

        this.controller = controller;
        this.idPList = new ArrayList<>();
        this.listLoaded = false;
        setEnabled(false);
        new Initializer().execute();
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
            throw new RuntimeException(ex);
        }
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

    private class Initializer extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() throws Exception {
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

            JSONArray resp = new JSONArray(body.toString());

            String lastIdP = ConfigurationEntry.SERVER_IDP_LAST_CONNECTED.getValue(controller);
            boolean lastIdPSet = false;

            removeAllItems();

            // PFS-2006
            addItem(Translation.get("wizard.login_online_storage.pre_selection_entry"));
            idPList.add(0, "");

            if (ConfigurationEntry.SERVER_IDP_EXTERNAL_NAMES.hasNonBlankValue(controller)) {
                String[] extNames = ConfigurationEntry.SERVER_IDP_EXTERNAL_NAMES.getValue(controller).split(",");

                for (String name : extNames) {
                    if (StringUtils.isNotBlank(name)) {

                        if (name.startsWith("!")) {
                            name = name.substring(1);
                        }

                        addItem(name.trim());
                        idPList.add(name.trim());
                        if (!lastIdPSet && name.equals(lastIdP)) {
                            setSelectedIndex(getItemCount() - 1);
                            lastIdPSet = true;
                        }
                    }
                }
            } else {
                addItem(Translation.get("wizard.login.external_users"));
                idPList.add(ServerClient.SAML_EXTERNAL_NON_SAML_USERS);
            }

            for (int i = 0; i < resp.length(); i++) {
                JSONObject obj = resp.getJSONObject(i);

                String entity = obj.getString("entityID");
                String name = obj.getJSONArray("DisplayNames").getJSONObject(0).getString("value");

                addItem(name);
                idPList.add(entity);

                if (!lastIdPSet && entity.equals(lastIdP)) {
                    setSelectedIndex(getItemCount() - 1);
                    lastIdPSet = true;
                }
            }

            if (!lastIdPSet) {
                setSelectedIndex(0);
                ConfigurationEntry.SERVER_IDP_LAST_CONNECTED.setValue(controller, ServerClient.SAML_EXTERNAL_NON_SAML_USERS);
                ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP.setValue(controller, ServerClient.SAML_EXTERNAL_NON_SAML_USERS);
            }

            addActionListener(new IdPSelectionAction());
            setEnabled(true);
            listLoaded = true;
            return null;
        }
    }
}
