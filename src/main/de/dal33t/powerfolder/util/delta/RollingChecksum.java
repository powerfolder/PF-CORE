package de.dal33t.powerfolder.util.delta;

import java.util.zip.Checksum;

/**
 * Interface for checksum algorithms which supports "rolling".
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: $ 
 * */
public interface RollingChecksum extends Checksum {
	/**
	 * Returns the size of frame used to "roll" over the data. 
	 * @return the framesize
	 */
	int getFrameSize();
	
	/**
	 * Updates the checksum.
	 * @param data the data to add to the checksum
	 */
	void update(byte[] data);
}
