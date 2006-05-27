package de.dal33t.powerfolder.util.ui;

import javax.swing.JProgressBar;

import de.dal33t.powerfolder.util.FileCopier;

/**
 * Custom progressbar for copying files using the FileCopier.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.2 $
 */
public class FileCopierProgressBar extends JProgressBar {

    private FileCopier fileCopier;
    private double maxTotal;
    private double totalFilePos;

    public FileCopierProgressBar(int orient, FileCopier fileCopier) {
        super(orient, 0, fileCopier.calculateSize());
        this.fileCopier = fileCopier;
    }

    /**
     * Returns the percent complete for the progress bar. Note that this number
     * is between 0.0 and 1.0.
     * 
     * @return the percent complete for this progress bar
     */
    public double getPercentComplete() {
        // update the total size, files maybe added in the meantime
        double total = fileCopier.calculateSize();
        if (total > maxTotal) {
            maxTotal = total;
        }
        if (total == 0) {
            return 1.0;
        }
        return totalFilePos / maxTotal;        
    }

    /** called by the copy method, counts the bytes that are written. */
    public void bytesWritten(int nrBytes) {
        totalFilePos += nrBytes;
        repaint();
    }

    /** reset so next use of this progress bar will start at 0 again */
    public void reset() {
        totalFilePos = 0;
        maxTotal = 0;
    }

}
