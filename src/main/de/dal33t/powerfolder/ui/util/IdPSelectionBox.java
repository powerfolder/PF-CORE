package de.dal33t.powerfolder.ui.util;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.ui.StyledComboBox;
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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class IdPSelectionBox extends StyledComboBox<String> {
    private final Controller controller;
    private List<String> idPList;
    private boolean listLoaded;

    public IdPSelectionBox(Controller controller) {
        super(new String[]{Translation.get("general.loading")});
        Reject.ifNull(controller, "Controller");
        this.controller = controller;
        this.idPList = new ArrayList<>();
        this.listLoaded = false;
        setEnabled(false);

        javax.swing.SwingWorker<Void, Void> worker = new SwingWorker<>() {
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
        };

        worker.execute();
    }

    public boolean isListLoaded() {
        return listLoaded;
    }

    private class IdPSelectionAction implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws UnsupportedEncodingException {
                    int index = getSelectedIndex();
                    String entity = idPList.get(index);

                    ConfigurationEntry.SERVER_IDP_LAST_CONNECTED.setValue(controller, entity);
                    String externalNames = ConfigurationEntry.SERVER_IDP_EXTERNAL_NAMES.getValue(controller);

                    if (StringUtils.isBlank(entity)) {
                        return null;
                    } else if (ServerClient.SAML_EXTERNAL_NON_SAML_USERS.equals(entity)
                            || (StringUtils.isNotBlank(externalNames) && externalNames.contains(entity)))
                    {
                        ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP
                                .setValue(controller, ServerClient.SAML_EXTERNAL_NON_SAML_USERS);
                        return null;
                    }

                    // TODO: Sign with private key...
                    String spConsumeURL = ConfigurationEntry.SERVER_WEB_URL.getValue(controller) + Constants.LOGIN_SHIBBOLETH_ANDROID_URI;
                    spConsumeURL += "?nodeID=" + controller.getMySelf().getId();
                    spConsumeURL += "?nodeNick=" + controller.getMySelf().getNick();
                    String idpWebLoginURL = ConfigurationEntry.SERVER_WEB_URL.getValue(controller) + "/Shibboleth.sso/Login";
                    idpWebLoginURL += "?SAMLDS=1&";
                    idpWebLoginURL += "entityID=" + URLEncoder.encode(entity, Convert.UTF8);
                    idpWebLoginURL += "&target=" + URLEncoder.encode(spConsumeURL, Convert.UTF8);

                    try {
                        BrowserLauncher.openURL(idpWebLoginURL);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }

                    String idpLookupURL = ConfigurationEntry.SERVER_WEB_URL.getValue(controller)
                            + "/api/idpd?entityID=" + URLEncoder.encode(entity, Convert.UTF8);

                    HttpGet getBindingURL = new HttpGet(idpLookupURL);
                    // PFC-2669:
                    HttpClientBuilder builder = Util.createHttpClientBuilder(controller);
                    HttpClient client = builder.build();

                    try {
                        HttpResponse httpResponse = client.execute(getBindingURL);
                        String ecpURL = EntityUtils.toString(httpResponse.getEntity());
                        ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP.setValue(controller, ecpURL);
                    } catch (IOException e1) {
                    }

                    controller.saveConfig();
                    return null;
                }
            };

            worker.execute();
        }
    }
}
