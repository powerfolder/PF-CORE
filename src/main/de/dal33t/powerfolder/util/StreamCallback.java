/* $Id: StreamCallback.java,v 1.1 2004/10/30 12:24:37 totmacherr Exp $
 */
package de.dal33t.powerfolder.util;

/**
 * A callback interface for methods, which acts on an input stream activity. May
 * break the read in of the inputstream
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public interface StreamCallback {
    /**
     * Indicates that a position of a stream has been reached
     * 
     * @param position
     *            the position in the stream
     * @param totalAvailable
     *            the total available bytes. might not be filled
     * @return if the stream read should be broken
     */
    public boolean streamPositionReached(int position, int totalAvailable);
}