/* $Id: ReconnectAction.java,v 1.10 2005/08/03 09:21:19 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.ui.dialog.ConnectDialog;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Reconnects to the member
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.10 $
 */
public class ReconnectAction extends SelectionBaseAction {

    public ReconnectAction(Controller controller, SelectionModel selectionModel) {
        super("reconnect", controller, selectionModel);
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();

        if (selection instanceof Member) {
            // Enable if not myself
            setEnabled(!((Member) selection).isMySelf());
        }

    }

    public void actionPerformed(ActionEvent e) {
        Object item = getUIController().getControlQuarter().getSelectedItem();
        if (!(item instanceof Member)) {
            return;
        }

        // Build new connect dialog
        final ConnectDialog connectDialog = new ConnectDialog(getController());
        final Member member = (Member) item;

        Runnable connector = new Runnable() {
            public void run() {
                // Open connect dialog if ui is open
                connectDialog.open(member.getNick());

                // Close connection first
                member.shutdown();

                // Now execute the connect
                try {
                    if (!member.reconnect()) {
                        throw new ConnectionException(Translation.getTranslation("dialog.unable_to_connect_to_member", 
                            member.getNick()));
                    }
                } catch (ConnectionException e) {
                    connectDialog.close();
                    log().verbose(e);
                    if (!connectDialog.isCanceled() && !member.isConnected()) {
                        // Show if user didn't canceled
                        e.show(getController());
                    }
                }

                // Close dialog
                connectDialog.close();
                log().verbose("Re-connector thread finished");
            }
        };

        // Start connect in anonymous thread
        new Thread(connector, "Reconnector to " + member.getNick()).start();
    }
}