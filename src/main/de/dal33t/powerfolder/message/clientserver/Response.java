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

import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.util.Reject;

/**
 * Represents the reponse for the request - response logic.
 * <p>
 * Subclass this for concrete responses.
 *
 * @see Request
 * @see de.dal33t.powerfolder.clientserver.RequestExecutor
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public abstract class Response extends Message {
    private static final long serialVersionUID = 100L;

    /**
     * The corresponding id of the request.
     */
    public String requestId;

    protected Response(Request request) {
        associate(request);
    }

    /**
     * Associate the response with the given request.
     *
     * @param request
     */
    private void associate(Request request) {
        Reject.ifNull(request, "Request is null");
        Reject.ifBlank(request.getRequestId(), "Request id is blank");
        requestId = request.getRequestId();
    }
}
