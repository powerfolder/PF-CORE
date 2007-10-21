package de.dal33t.powerfolder.message.clientserver;

import java.io.Serializable;

/**
 * Simple test echo response.
 * 
 * @see EchoRequest
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class EchoResponse extends Response {
    private static final long serialVersionUID = 100L;

    public Serializable payload;
}
