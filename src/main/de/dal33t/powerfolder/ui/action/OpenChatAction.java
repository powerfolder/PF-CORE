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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.InformationQuarter;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;

/**
 * Open a Chat Frame on a Folder or Member
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.10 $
 */

public class OpenChatAction extends SelectionBaseAction {

    InformationQuarter informationQuarter;

    public OpenChatAction(Controller controller, SelectionModel selectionModel)
    {
        super("open_chat", controller, selectionModel);
        informationQuarter = controller.getUIController()
            .getInformationQuarter();
    }

    public void selectionChanged(SelectionChangeEvent selectionChangeEvent) {
        Object selection = selectionChangeEvent.getSelection();        
        if (selection instanceof Member) {
            Member member = (Member) selection;
            setEnabled(member.isCompleteyConnected());
        } else if (selection instanceof Folder) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }
    
    public void actionPerformed(ActionEvent e) {
        Object selection = getSelectionModel().getSelection(); 
        if (selection instanceof Folder) {
            informationQuarter.displayChat((Folder) selection);
        } else if (selection instanceof Member) {
            informationQuarter.displayChat((Member) selection);
        }
    }
}