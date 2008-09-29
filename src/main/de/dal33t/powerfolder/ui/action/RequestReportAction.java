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
package de.dal33t.powerfolder.ui.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.RequestNodeInformation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Loggable;
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
                Loggable.logFinerStatic(RequestReportAction.class,
                        "Requesting node information from " + member);
                member.sendMessageAsynchron(new RequestNodeInformation(),
                    Translation.getTranslation("node_info.error"));
                getUIController().getInformationQuarter().displayText(
                    Translation.getTranslation("node_info.requesting"));
            }
        }
    }
}