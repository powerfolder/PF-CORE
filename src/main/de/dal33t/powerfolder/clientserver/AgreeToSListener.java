/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.clientserver;

import java.io.IOException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.notices.WarningNotice;
import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.Translation;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class AgreeToSListener extends PFComponent implements ServerClientListener {

    private boolean wasPaused = false;
    private boolean agreedOnToS = true;
    private ToSNotice tosn;

    public AgreeToSListener(Controller controller) {
        super(controller);
        tosn = new ToSNotice(Translation.get("dialog.tos.title"),
            Translation.get("dialog.tos.summary"),
            Translation.get("dialog.tos.text"),
            controller.getOSClient().getToSURL());
    }

    @Override
    public boolean fireInEventDispatchThread() {
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
            final ServerClient client = event.getClient();

            if (event.getAccountDetails().needsToAgreeToS()) {
                wasPaused = getController().isPaused();
                agreedOnToS = false;
                getController().setPaused(true);
                getController().getUIController().getApplicationModel()
                    .getNoticesModel().handleNotice(tosn);

                // open Wizard with client.getToSURL();
                DialogFactory.genericDialog(getController(),
                    Translation.get("dialog.tos.title"),
                    Translation.get("dialog.tos.text"),
                    new String[]{"OK"}, 0, GenericDialogType.INFO);

                getController().getIOProvider().startIO(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            BrowserLauncher.openURL(client.getToSURL());
                        } catch (IOException ioe) {
                            logWarning("Could not open browser to view ToS. " + ioe);
                        }
                    }
                });
            } else {
                agreedOnToS = true;
                getController().getUIController().getApplicationModel()
                    .getNoticesModel().clearNotice(tosn);
            }
        } catch (RuntimeException re) {
            logWarning("Error during check if terms of service apply. " + re);
        }
    }

    @Override
    public void accountUpdated(ServerClientEvent event) {
        if (!event.getAccountDetails().needsToAgreeToS()) {
            getController().setPaused(wasPaused);
            getController().getUIController().getApplicationModel()
                .getNoticesModel().clearNotice(tosn);
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
        getController().getIOProvider().startIO(new Runnable() {
            @Override
            public void run() {
                if (!event.getClient().isLoggedIn()) {
                    return;
                }
                AccountDetails ad = getController().getOSClient()
                    .refreshAccountDetails();
                if (ad.needsToAgreeToS()) {
                    wasPaused = getController().isPaused();
                    agreedOnToS = false;
                    getController().setPaused(true);
                    getController().getUIController().getApplicationModel()
                        .getNoticesModel().handleNotice(tosn);
                }
            }
        });
    }

    private class ToSNotice extends WarningNotice {
        private static final long serialVersionUID = 100L;
        private final String tosURL;

        public ToSNotice(String title, String summary, String message,
            String tosURL)
        {
            super(title, summary, message);
            this.tosURL = tosURL;
        }

        @Override
        public Runnable getPayload(Controller controller) {
            return new Runnable() {

                @Override
                public void run() {
                    DialogFactory.genericDialog(getController(), getTitle(),
                        getMessage(), new String[]{"OK"}, 0,
                        GenericDialogType.INFO);

                    getController().getIOProvider().startIO(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                BrowserLauncher.openURL(tosURL);
                            } catch (IOException ioe) {
                                logWarning(
                                    "Could not open browser to view ToS. "
                                        + ioe);
                            }
                        }
                    });
                }
            };
        }
    }
}
