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
* $Id: OpenInvitationReceivedWizardAction.java 5419 2008-09-29 12:18:20Z harry $
*/
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.message.SingleFileOffer;
import de.dal33t.powerfolder.ui.model.ReceivedSingleFileOffersModel;

import java.awt.event.ActionEvent;

public class SingleFileTransferOfferAction extends BaseAction {

    public SingleFileTransferOfferAction(Controller controller) {
        super("action_single_file_transfer_offer", controller);
    }

    public void actionPerformed(ActionEvent e) {

        if (!getController().isUIOpen()) {
            return;
        }

        ReceivedSingleFileOffersModel model = getUIController()
                .getApplicationModel().getReceivedSingleFileOffersModel();
        if ((Integer) model.getReceivedSingleFileOfferCountVM().getValue()
                <= 0) {
            return;
        }

        SingleFileOffer offer = model.popOffer();
        

    }
}