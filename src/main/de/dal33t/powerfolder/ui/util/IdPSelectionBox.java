package de.dal33t.powerfolder.ui.util;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.StyledComboBox;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.SwingWorker;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class IdPSelectionBox extends StyledComboBox<String> {
    private boolean listLoaded;

    public IdPSelectionBox(Controller controller) {
        super(new String[]{Translation.get("general.loading")});
        Reject.ifNull(controller, "Controller");
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
                List<String> idPList = new ArrayList<>(resp.length());

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
                    idPList.add("ext");
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
                    ConfigurationEntry.SERVER_IDP_LAST_CONNECTED.setValue(controller, "ext");
                    ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP.setValue(controller, "ext");
                }

                addActionListener(new IdPSelectionAction(controller, idPList));
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
}
