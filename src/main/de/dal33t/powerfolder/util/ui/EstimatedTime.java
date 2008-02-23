package de.dal33t.powerfolder.util.ui;

import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

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

	@Override
	public String toString() {
		if (isActive()) {
			if (deltaTimeMillis < 0) {
				return Translation.getTranslation("estimation.indefinite");
			} else {
				return Format.formatDeltaTime(deltaTimeMillis);
			}
		}
		return "";
	}
}
