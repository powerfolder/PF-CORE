package de.dal33t.powerfolder.ui.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JComboBox;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

@SuppressWarnings("unchecked")
public class IdPSelectionAction extends PFComponent implements ActionListener {

    List<String> idPList;

    public IdPSelectionAction(Controller controller, List<String> idPList) {
        super(controller);

        this.idPList = idPList;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JComboBox<String> source = (JComboBox<String>) e.getSource();

        int index = source.getSelectedIndex();
        String entity = idPList.get(index);

        ConfigurationEntry.SERVER_IDP_LAST_CONNECTED.setValue(getController(),
            entity);

        HttpGet getECPBinding = new HttpGet(entity);
        DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse httpResponse;
        try {
            httpResponse = client.execute(getECPBinding);
            String responseBody = EntityUtils
                .toString(httpResponse.getEntity());

            int singleSOSBegin = responseBody.indexOf("SingleSignOnService");
            int soapBegin = responseBody.indexOf(
                "urn:oasis:names:tc:SAML:2.0:bindings:SOAP", singleSOSBegin);
            int locationBegin = responseBody.indexOf("Location", soapBegin);
            int locationURLBegin = responseBody.indexOf("\"", locationBegin);
            int locationURLEnd = responseBody.indexOf("\"",
                locationURLBegin + 1);

            String locationURL = responseBody.substring(locationURLBegin + 1,
                locationURLEnd);

            ConfigurationEntry.SERVER_IDP_LAST_CONNECTED_ECP.setValue(
                getController(), locationURL);

        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        getController().saveConfig();
    }
}
