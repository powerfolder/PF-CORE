/*
* Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
* $Id: SimpleTimeEstimator.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.util;


import java.util.Date;

/**
 * This class estimates the time at which a process is expected to reach 100%
 * complete. The process needs to regularly supply updates of the percentage
 * that it has reached. The class uses the current and previous updates to
 * calculate linearly the completion time.
 */
public class SimpleTimeEstimator {

    private double lastPercentage;
    private long lastTime;
    private Date estimatedDate;

    /**
     * Update the estimate with the current completion percentage.
     *
     * @param thisPercentageArg
     */
    public Date updateEstimate(double thisPercentageArg) {

        Date now = new Date();

        double thisPercentage;
        if (thisPercentageArg > 100.0) {
            thisPercentage = 100.0;
        } else if (thisPercentageArg < 0) {
            thisPercentage = 0.0;
        } else {
            thisPercentage = thisPercentageArg;
        }

        long thisTime = now.getTime();
        if (Double.compare(thisPercentage, 100.0) == 0 &&
                Double.compare(lastPercentage, 100.0) != 0) {
            // Yey! Reached 100%. Set the estimated date to NOW.
            estimatedDate = now;
            lastPercentage = thisPercentage;
        } else if (Double.compare(thisPercentage, lastPercentage) == 0) {
            // No change
        } else if (thisPercentage > lastPercentage && lastTime > 0) {
            // . . . --> T I M E - L I N E --> . . .
            // lastTime       ... thisTime       ... targetTime
            // lastPercentage ... thisPercentage ... 100%
            //
            // (thisTime - lastTime) / (thisPercentage - lastPercentage) ==
            // (targetTime - thisTime) / (100% - thisPercentage)

            // Skip updates less than 5 seconds ago.
            // This prevents rapid sequeces giving bad results.
            if (thisTime - lastTime > 5000) {
                long targetTime = thisTime + (long) ((100.0 - thisPercentage) *
                    (thisTime - lastTime) / (thisPercentage - lastPercentage));
                lastPercentage = thisPercentage;
                lastTime = thisTime;
                estimatedDate = new Date(targetTime);
            }
        } else {
            // Presumably this is the first value or the time sequence was
            // recalculated or something. Can not estimate a time before now,
            // percentages going backward.
            lastPercentage = thisPercentage;
            lastTime = thisTime;
            estimatedDate = null;
        }

        return estimatedDate;
    }
}
