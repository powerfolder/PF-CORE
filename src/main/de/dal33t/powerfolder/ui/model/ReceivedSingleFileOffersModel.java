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
 * $Id: ReceivedInvitationsModel.java 5975 2008-12-14 05:23:32Z harry $
 */
package de.dal33t.powerfolder.ui.model;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.message.SingleFileOffer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class to manage received single file offers. Offers can be added and removed.
 * Also a value model is available to count offers.
 * Listeners can be added to be notified of changes to the number of offers
 * in the model.
 */
public class ReceivedSingleFileOffersModel extends PFComponent
        implements SingleFileOfferHandler {

    private final ValueModel receivedSingleFileOffersVM = new ValueHolder();

    private List<SingleFileOffer> offers =
            new CopyOnWriteArrayList<SingleFileOffer>();

    /**
     * Constructor
     *
     * @param controller
     */
    public ReceivedSingleFileOffersModel(Controller controller) {
        super(controller);
        receivedSingleFileOffersVM.setValue(0);
        getController().addSingleFileOfferHandler(this);
    }

    /**
     * Value model with integer count of received invitations.
     *
     * @return
     */
    public ValueModel getReceivedSingleFileOfferCountVM() {
        return receivedSingleFileOffersVM;
    }

    public void gotOffer(SingleFileOffer offer) {
        offers.add(offer);
        receivedSingleFileOffersVM.setValue(offers.size());
    }

    /**
     * Remove an notification from the model for display, etc.
     *
     * @return
     */
    public SingleFileOffer popOffer() {
        if (!offers.isEmpty()) {
            SingleFileOffer offer = offers.remove(0);
            receivedSingleFileOffersVM.setValue(
                    offers.size());
            return offer;
        }
        return null;
    }

}