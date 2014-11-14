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
package de.dal33t.powerfolder.ui.preferences;

import javax.swing.*;

public interface PreferenceTab {

    /** true if PowerFolder should restart for all changed to be in effect */
    public boolean needsRestart();

    /**
     * Tells the tab to save it's settings now. Afterwards
     * Controller.saveConfig() is called, so you don't need to do it.
     */
    public void save();

    /**
     * Shoud return false if some validation failed.
     */
    public boolean validate();

    /** returns a localized tabname for this tab */
    public String getTabName();

    /** the UI component that should appear in this tab */
    public JPanel getUIPanel();

    /**
     * undo those changes that where done immediately like laf change *
     */
    public void undoChanges();
}
