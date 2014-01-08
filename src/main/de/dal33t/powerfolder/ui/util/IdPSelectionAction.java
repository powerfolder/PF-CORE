package de.dal33t.powerfolder.ui.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.SwingWorker;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Convert;

@SuppressWarnings("unchecked")
public class IdPSelectionAction extends PFComponent implements ActionListener {

    List<String> idPList;

    public IdPSelectionAction(Controller controller, List<String> idPList) {
        super(controller);

        this.idPList = idPList;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws UnsupportedEncodingException {
                JComboBox<String> source = (JComboBox<String>) e.getSource();

                int index = source.getSelectedIndex();
                String entity = idPList.get(index);

                ConfigurationEntry.SERVER_IDP_LAST_CONNECTED.setValue(
                    getController(), entity);

                String idpLookupURL = ConfigurationEntry.SERVER_WEB_URL
                    .getValue(getController())
                    + "/api/idpd?entityID="
                    + URLEncoder.encode(entity, Convert.UTF8.toString());

                HttpGet getBindingURL = new HttpGet(idpLookupURL);
                DefaultHttpClient client = new DefaultHttpClient();
                HttpResponse httpResponse;

                try {
                    httpResponse = client.execute(getBindingURL);
                    String ecpURL = EntityUtils.toString(httpResponse
                        .getEntity());

                    ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP.setValue(
                        getController(), ecpURL);
                } catch (IOException e1) {
                    logSevere("Could not receive List of Identity Provider. "
                        + e1);
                }

                getController().saveConfig();

                return null;
            }
        };

        worker.execute();
    }
}
