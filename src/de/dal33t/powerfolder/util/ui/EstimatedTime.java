package de.dal33t.powerfolder.util.ui;

/**
 * Container for time estimation information.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.1 $
 */
public class EstimatedTime {
    private long deltaTimeMillis;
    private boolean active;
    
    public EstimatedTime(long deltaTimeMillis, boolean active) {
        this.deltaTimeMillis = deltaTimeMillis;
        this.active = active;
    }

    /**
     * Returns if the time estimation was calculated "in progress"
     * @return
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Returns the estimated time in milliseconds 
     * @return
     */
    public long getDeltaTimeMillis() {
        return deltaTimeMillis;
    }
    
}
