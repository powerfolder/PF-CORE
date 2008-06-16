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
