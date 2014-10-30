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
 * $Id: NodeManager.java 7814 2009-04-28 07:52:20Z tot $
 */

package de.dal33t.powerfolder;

/**
 * Return value of connecting methods.
 */
public class ConnectResult {
    private boolean success;
    private String message;

    private ConnectResult(boolean success, String message) {
        super();
        this.success = success;
        this.message = message;
    }

    public static ConnectResult success() {
        return new ConnectResult(true, null);
    }

    public static ConnectResult failure(String message) {
        return new ConnectResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Connection " + (success ? "successful" : "failed")
            + (message != null ? ": " + message : "");
    }

}