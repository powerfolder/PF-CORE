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
package de.dal33t.powerfolder.ui.webservice;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Quickinfo for the Online Storage.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class OnlineStorageQuickInfoPanel extends QuickInfoPanel {
    private ServerClient client;

    private JComponent picto;
    private JComponent headerText;
    private JLabel infoText1;
    private JLabel infoText2;

    public OnlineStorageQuickInfoPanel(Controller controller) {
        this(controller, controller.getOSClient());
    }

    public OnlineStorageQuickInfoPanel(Controller controller,
        ServerClient client)
    {
        super(controller);
        Reject.ifNull(client, "Server client is null");
        this.client = client;
    }

    /**
     * Initializes the components
     */
    @Override
    protected void initComponents() {
        headerText = SimpleComponentFactory.createBiggerTextLabel(Translation
            .getTranslation("quickinfo.webservice.title"));

        infoText1 = SimpleComponentFactory.createBigTextLabel("");
        infoText2 = SimpleComponentFactory.createBigTextLabel("");

        picto = new JLabel(Icons.WEBSERVICE_QUICK_INFO_PICTO);

        updateText();
        registerListeners();
    }

    /**
     * Registers the listeners into the core components
     */
    private void registerListeners() {
        client.addListener(new MyServerClientListener());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        boolean con = client.isConnected();
        String text1;
        if (getController().isLanOnly()) {
            text1 = Translation
                .getTranslation("quickinfo.webservice.not_available");
        } else {
            text1 = con ? Translation
                .getTranslation("quickinfo.webservice.connected") : Translation
                .getTranslation("quickinfo.webservice.not_connected");
            if (getController().isVerbose()) {
                text1 += " (" + client.getServer().getNick() + ", "
                    + client.getServer().getHostName() + ":"
                    + client.getServer().getPort() + ")";
            }
        }
        String text2;
        if (con) {
            int nMirrored = client.getJoinedFolders().size();
            int nFolders = getController().getFolderRepository()
                .getFoldersCount();
            AccountDetails ad = client.getAccountDetails();
            String usedPerc = "? %";
            if (ad != null && ad.getAccount().isValid()) {
                long storageSize = ad.getAccount().getOSSubscription()
                    .getType().getStorageSize();
                if (storageSize != 0) {
                    double perc = (double) ad.getSpaceUsed() * 100
                        / storageSize;
                    usedPerc = Format.formatNumber(perc) + " %";
                }
            }
            text2 = Translation.getTranslation(
                "quickinfo.webservice.folders_mirrored", nMirrored, nFolders,
                usedPerc);
        } else {
            text2 = "";
        }
        infoText1.setText(text1);
        infoText2.setText(text2);
    }

    // Overridden stuff *******************************************************

    @Override
    protected JComponent getPicto() {
        return picto;
    }

    @Override
    protected JComponent getHeaderText() {
        return headerText;
    }

    @Override
    protected JComponent getInfoText1() {
        return infoText1;
    }

    @Override
    protected JComponent getInfoText2() {
        return infoText2;
    }

    // Inner logic ************************************************************

    private class MyServerClientListener implements ServerClientListener {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        public void accountUpdated(ServerClientEvent event) {
            updateText();
        }

        public void login(ServerClientEvent event) {
            updateText();
        }

        public void serverConnected(ServerClientEvent event) {
            updateText();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateText();
        }
    }
}
