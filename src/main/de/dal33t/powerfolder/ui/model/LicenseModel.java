/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: ApplicationModel.java 9221 2009-08-28 00:09:15Z tot $
 */
package de.dal33t.powerfolder.ui.model;

import javax.swing.Action;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

/**
 * @author sprajc
 */
public class LicenseModel {
    private ValueModel daysValidModel;
    private Action activationAction;
    private ValueModel licenseKeyModel;
    private ValueModel gbAllowedModel;

    public ValueModel getLicenseKeyModel() {
        return licenseKeyModel;
    }

    public LicenseModel() {
        super();
        this.daysValidModel = new ValueHolder(-1, true);
        this.licenseKeyModel = new ValueHolder(null, false);
        this.gbAllowedModel = new ValueHolder(-1, true);
    }

    public ValueModel getGbAllowedModel() {
        return gbAllowedModel;
    }

    public Action getActivationAction() {
        return activationAction;
    }

    public void setActivationAction(Action activationAction) {
        this.activationAction = activationAction;
    }

    public ValueModel getDaysValidModel() {
        return daysValidModel;
    }

}
