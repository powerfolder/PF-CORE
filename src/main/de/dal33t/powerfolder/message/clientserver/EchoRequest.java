package de.dal33t.powerfolder.message.clientserver;

import java.io.Serializable;

/**
 * Simple test message. Request to echo the payload
 * 
 * @see EchoResponse
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class EchoRequest extends Request {
    private static final long serialVersionUID = 100L;

    public Serializable payload;
}
