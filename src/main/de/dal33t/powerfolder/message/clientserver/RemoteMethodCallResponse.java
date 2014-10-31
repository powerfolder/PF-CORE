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
package de.dal33t.powerfolder.message.clientserver;

import de.dal33t.powerfolder.util.Reject;

/**
 * Answer to a <code>ServiceMethodCall</code>
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class RemoteMethodCallResponse extends Response {
    private static final long serialVersionUID = 100L;

    private boolean exception;
    private Object result;

    public RemoteMethodCallResponse(RemoteMethodCallRequest call,
        Object result)
    {
        super(call);
        this.exception = false;
        this.result = result;
    }

    public RemoteMethodCallResponse(RemoteMethodCallRequest call,
        Throwable throwable)
    {
        super(call);
        Reject.ifNull(throwable, "Throwable is null");
        this.exception = true;
        this.result = throwable;
    }

    public boolean isException() {
        return exception;
    }

    public Object getResult() {
        return result;
    }

    public Throwable getException() {
        return (Throwable) result;
    }


    public String toString() {
        return "RemoteResponse result: " + result + ", exception: " + exception;
    }
}
