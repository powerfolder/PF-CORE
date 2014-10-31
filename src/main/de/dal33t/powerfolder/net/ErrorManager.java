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
package de.dal33t.powerfolder.net;

import de.dal33t.powerfolder.PFComponent;

import java.util.HashMap;
import java.util.Map;

/*
 * The ErrorManager manages the errors
 * recieved from the updated dyndns server
 *
 * @author albena roshelova
 */
public class ErrorManager extends PFComponent {

    // display
    public static final int NO_ERROR = 0;
    public static final int WARN = 1;
    public static final int ERROR = 2;
    public static final int UNKNOWN = 3;

    public Map<String, ErrorInfo> errors;

    public ErrorManager() {
        errors = new HashMap<String, ErrorInfo>();
    }

    public int getType(String errorCode) {

        ErrorInfo info = errors.get(errorCode);
        if (info != null) {
            return info.getType();
        }
        logFiner("Uknown errorCode received: " + errorCode);
        return UNKNOWN;

    }

    public int getCode(String errorCode) {
        ErrorInfo info = errors.get(errorCode);
        if (info != null) {
            return info.getCode();
        }
        logFiner("Uknown errorCode received: " + errorCode);
        return UNKNOWN;
    }

    public String getText(String errorCode) {
        ErrorInfo info = errors.get(errorCode);
        if (info != null) {
            return info.getText();
        }
        logFiner("Uknown errorCode received: " + errorCode);
        return "";
    }

    public String getShortText(String errorCode) {
        ErrorInfo info = errors.get(errorCode);
        if (info != null) {
            return info.getShortText();
        }
        return "";
    }
}