package de.dal33t.powerfolder.transfer;

/**
 * Streams which implement this interface are able to throttle their throughput.
 * @author Dante
 * $Id: LimitedStream.java,v 1.1 2006/03/05 23:53:35 bytekeeper Exp $
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.1 $
 *
 */
public interface LimitedStream {
    BandwidthLimiter getBandwidthLimiter();
    
    /**
     * Sets a new limiter.
     * You should NOT change the BandwidthLimiter while any limited operation
     * is ongoing. (= This method is NOT Thread safe)
     * @param limiter the new limiter
     */
    void setBandwidthLimiter(BandwidthLimiter limiter);
}
