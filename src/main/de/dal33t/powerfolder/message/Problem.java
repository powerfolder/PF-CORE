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

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.ProblemProto;

/**
 * General problem response
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class Problem extends Message implements D2DObject {
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

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof ProblemProto.Problem) {
            ProblemProto.Problem proto = (ProblemProto.Problem)mesg;
            
            this.message     = proto.getMessage();
            this.fatal       = proto.getFatal();
            this.problemCode = proto.getProblemCodeValue();
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage toD2D() {
        ProblemProto.Problem.Builder builder = ProblemProto.Problem.newBuilder();
        
        builder.setClazzName("Problem");
        builder.setFatal(this.fatal);
        builder.setProblemCodeValue(this.problemCode);

        if(null != this.message) builder.setMessage(this.message);
        
        return builder.build();
    }
}