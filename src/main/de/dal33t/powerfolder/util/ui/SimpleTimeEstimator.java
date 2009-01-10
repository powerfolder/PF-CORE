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
package de.dal33t.powerfolder.util.ui;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.binding.value.ValueHolder;

import java.util.Date;

/**
 * This class is estimates the time at which a process will reach 100% complete.
 * The process needs to regularly supply updates of the percentage that it has
 * reached. The class uses the current and previous updates to calculate
 * linearly the completion time.
 */
public class SimpleTimeEstimator {

    private double lastPercentage;
    private long lastTime;
    private final ValueModel estimatedDateVM;

    public SimpleTimeEstimator() {
        estimatedDateVM = new ValueHolder(); // <Date>... may be null.
    }

    /**
     * Update the estimate with the current completion percentage.
     *
     * @param thisPercentageArg
     */
    public void updateEstimate(double thisPercentageArg) {

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
        if (thisPercentage > lastPercentage && lastTime > 0) {
            if (Double.compare(thisPercentage, lastPercentage) == 0) {
                // Duh, no percentage change from last time? Theoretically
                // the target time would be infinity. Probably updating too
                // fast. Ignore this estimate.
            } else {
                // . . . --> T I M E - L I N E --> . . .
                // lastTime       ... thisTime       ... targetTime
                // lastPercentage ... thisPercentage ... 100%
                //
                // (thisTime - lastTime) / (thisPercentage - lastPercentage) ==
                // (targetTime - thisTime) / (100% - thisPercentage)
                long targetTime = thisTime + (long) ((100.0 - thisPercentage) *
                    (thisTime - lastTime) / (thisPercentage - lastPercentage));
                estimatedDateVM.setValue(new Date(targetTime));
                lastPercentage = thisPercentage;
                lastTime = thisTime;
            }
        } else {
            // Presumably this is the first value or the time sequence was
            // recalculated or something. Cannot estimate a time before now
            // (percentages going backward)!
            lastPercentage = thisPercentage;
            lastTime = thisTime;
            estimatedDateVM.setValue(null);
        }
    }

    /**
     * Value model with the target completion date.
     * NOTE - the value may be null if it was not possible to calculate the
     * target date.
     *
     * @return
     */
    public ValueModel getEstimatedDateVM() {
        return estimatedDateVM;
    }
}
