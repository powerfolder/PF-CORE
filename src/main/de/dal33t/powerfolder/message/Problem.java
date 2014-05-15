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
package de.dal33t.powerfolder.message;

/**
 * General problem response
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class Problem extends Message {
    private static final long serialVersionUID = 100L;

    // The problem codes
    public static final int DISCONNECTED = 7;
    public static final int DO_NOT_LONGER_CONNECT = 666;
    public static final int DUPLICATE_CONNECTION = 777;
    public static final int NETWORK_ID_MISMATCH = 888;

    // The problem code
    public int problemCode;

    public String message;
    /** Indicates, that this is a fatal problem, will disconnect */
    public boolean fatal;

    public Problem() {
        // Serialisation constructor
    }

    /**
     * Initalizes a problem with fatal flag
     *
     * @param message
     * @param fatal
     */
    public Problem(String message, boolean fatal) {
        this.message = message;
        this.fatal = fatal;
    }

    /**
     * Constructs a problem with an problem code
     *
     * @param message
     * @param fatal
     * @param pCode
     */
    public Problem(String message, boolean fatal, int pCode) {
        this(message, fatal);
        this.problemCode = pCode;
    }

    public String toString() {
        return "Problem: '" + message + "'";
    }
}