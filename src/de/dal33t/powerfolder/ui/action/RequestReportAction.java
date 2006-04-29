/* $Id: RequestReportAction.java,v 1.8 2005/10/27 03:22:43 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.RequestNodeInformation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Requests the debug report from a member
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.8 $
 */
public class RequestReportAction extends SelectionBaseAction {

    public RequestReportAction(Controller controller,
        SelectionModel selectionModel)
    {
        super("Request debug report", Icons.MAC, controller, selectionModel);
        putValue(Action.SHORT_DESCRIPTION,
            "Request a debug report for this user");
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();

        if (selection instanceof Member) {
            setEnabled(((Member) selection).isConnected());
        }
    }

    public void actionPerformed(ActionEvent e) {
        Object selection = getSelectionModel().getSelection();
        if (selection instanceof Member) {
            Member member = (Member) selection;
            if (member.isConnected() || member.isMySelf()) {
                log().debug("Requesting node information from " + member);
                member.sendMessageAsynchron(new RequestNodeInformation(),
                    Translation.getTranslation("nodeinfo.error"));
                getUIController().getInformationQuarter().displayText(
                    Translation.getTranslation("nodeinfo.requesting"));
            }
        }
    }
}