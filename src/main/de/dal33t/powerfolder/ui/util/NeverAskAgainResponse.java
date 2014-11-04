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
package de.dal33t.powerfolder.ui.util;

/**
 * Response from a NeverAskAgain dialog.
 */
public class NeverAskAgainResponse {

    private final boolean neverAskAgain;
    private final int buttonIndex;

    /**
     * Response from a NeverAskAgain dialog.
     *
     * @param buttonIndex 0 = first button, 1 = second, etc.
     * -1 if dialog cancelled.
     * @param neverAskAgain true if never ask again checked.
     */
    public NeverAskAgainResponse(int buttonIndex, boolean neverAskAgain) {
        this.buttonIndex = buttonIndex;
        this.neverAskAgain = neverAskAgain;
    }

    /**
     * Selected index bitton.
     * 0 = first button, 1 = second, etc.
     * -1 if dialog cancelled.
     *
     * @return button index.
     */
    public int getButtonIndex() {
        return buttonIndex;
    }

    /**
     * If never show again.
     *
     * @return true if never askagain checked.
     */
    public boolean isNeverAskAgain() {
        return neverAskAgain;
    }
}
