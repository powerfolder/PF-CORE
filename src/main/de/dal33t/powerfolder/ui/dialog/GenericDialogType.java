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
package de.dal33t.powerfolder.ui.dialog;

/**
 * Utility enum that is used to define icons in {@link de.dal33t.powerfolder.ui.dialog.DialogFactory}.
 */
public enum GenericDialogType {

    /** the default icon reference */
    DEFAULT("default"),
    /** the error icon reference */
    ERROR("error"),
    /** the warn icon reference */
    WARN("warn"),
    /** the info icon reference */
    INFO("info"),
    /** the question icon reference */
    QUESTION("question");

    /** name of the enum */
    private String name;

    /**
     * Constructor for creating local enums.
     *
     * @param name name of the enum
     */
    GenericDialogType(String name) {
        this.name = name;
    }

    /**
     * gets the name of the enum
     */
    public String getName() {
        return name;
    }
}
