package de.dal33t.powerfolder.clientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;

public class AgreeToSListener extends PFComponent implements ServerClientListener {

    private boolean wasPaused = false;
    private boolean agreedOnToS = true;
    private int tosVersion = -1;

    public AgreeToSListener(Controller controller) {
        super(controller);
    }

    @Override
    public boolean fireInEventDispatchThread() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void login(ServerClientEvent event) {
        Account account = event.getAccountDetails().getAccount();

        if (!(event.isLoginSuccess()
            && account.isValid()))
        {
            return;
        }

        try {
            ServerClient client = event.getClient();

            URL tosFileURL = new URL(client.getToSFileURL());
            HttpURLConnection tosFileCon = (HttpURLConnection) tosFileURL.openConnection();
            tosFileCon.setRequestMethod("GET");
            tosFileCon.connect();

            int tosFileStatus = tosFileCon.getResponseCode();

            if (tosFileStatus != HttpURLConnection.HTTP_OK) {
                return;
            }

            tosFileCon.disconnect();

            URL tosVersionFileURL = new URL(client.getToSVersionFileURL());
            HttpURLConnection tosVersionFileCon = (HttpURLConnection) tosVersionFileURL
                .openConnection();
            tosVersionFileCon.setRequestMethod("GET");
            tosVersionFileCon.connect();

            int tosVersionFileStatus = tosVersionFileCon.getResponseCode();

            if (tosVersionFileStatus != HttpURLConnection.HTTP_OK) {
                return;
            }

            tosVersion = 0;

            try (BufferedReader br = new BufferedReader(
                new InputStreamReader(tosVersionFileCon.getInputStream())))
            {
                tosVersion = Integer.parseInt(br.readLine());
            } catch (RuntimeException re) {
                logWarning("Could not parse the version of the Terms of Service. " + re);
                return;
            }

            if (account.getAppliedToSVersion() < tosVersion) {
                wasPaused = getController().isPaused();
                agreedOnToS = false;
                getController().setPaused(true);

                // open Wizard with client.getToSURL();
                DialogFactory.genericDialog(getController(),
                    Translation.get("dialog.tos.title"),
                    Translation.get("dialog.tos.text"),
                    new String[]{"OK"}, 0, GenericDialogType.INFO);

                BrowserLauncher.openURL(client.getToSURL());
            } else {
                agreedOnToS = true;
            }
        } catch (RuntimeException | IOException re) {
            logWarning("Error during check if terms of service apply. " + re);
        }
    }

    @Override
    public void accountUpdated(ServerClientEvent event) {
        if (event.getAccountDetails().getAccount().getAppliedToSVersion() >= tosVersion)
        {
            getController().setPaused(wasPaused);
            agreedOnToS = true;
        }
    }

    public boolean hasAgreedOnToS() {
        return agreedOnToS;
    }
    
    @Override
    public void serverConnected(ServerClientEvent event) {
        // NOP
    }

    @Override
    public void serverDisconnected(ServerClientEvent event) {
        // NOP
    }

    @Override
    public void nodeServerStatusChanged(ServerClientEvent event) {
        // NOP
    }
}
