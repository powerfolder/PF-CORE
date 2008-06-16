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
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Quickinfo for the Online Storage.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class OnlineStorageQuickInfoPanel extends QuickInfoPanel {
    private JComponent picto;
    private JComponent headerText;
    private JLabel infoText1;
    private JLabel infoText2;

    public OnlineStorageQuickInfoPanel(Controller controller) {
        super(controller);
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
     * Registeres the listeners into the core components
     */
    private void registerListeners() {
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerLister());
    }

    /**
     * Updates the info fields
     */
    private void updateText() {
        ServerClient client = getController().getOSClient();
        boolean con = client.isConnected();
        String text1;
        if (getController().isLanOnly()) {
            text1 = Translation
                .getTranslation("quickinfo.webservice.notavailable");
        } else {
            text1 = con ? Translation
                .getTranslation("quickinfo.webservice.connected") : Translation
                .getTranslation("quickinfo.webservice.notconnected");
        }
        String text2;
        if (!con) {
            int nMirrored = client.getJoinedFolders().size();
            int nFolders = getController().getFolderRepository()
                .getFoldersCount();
            AccountDetails ad = client.getAccountDetails();
            String usedPerc = "? %";
            if (ad != null) {
                long storageSize = ad.getAccount().getOSSubscription().getType()
                        .getStorageSize();
                if (storageSize != 0) {
                    double perc = (double) ad.getSpaceUsed() * 100
                            / storageSize;
                    usedPerc = Format.formatNumber(perc) + " %";
                }
            }
            text2 = Translation.getTranslation(
                "quickinfo.webservice.foldersmirrored", nMirrored, nFolders,
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

    private class MyNodeManagerLister implements NodeManagerListener {

        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeConnected(NodeManagerEvent e) {
            if (getController().getOSClient().isServer(e.getNode())) {
                updateText();
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (getController().getOSClient().isServer(e.getNode())) {
                updateText();
            }
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }
}
