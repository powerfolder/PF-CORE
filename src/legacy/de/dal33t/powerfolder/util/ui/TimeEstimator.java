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
* $Id$
*/
package de.dal33t.powerfolder.util.ui;

import java.util.LinkedList;

import de.dal33t.powerfolder.Constants;

/**
 * Generic class to estimate progress.
 * Using an optional sliding window, this class tries to estimate the time at which a given value is reached.
 * To do this it requires updates on the real value - the estimation is extrapolated using a configurable function.
 * @author Dennis "Bytekeeper" Waldherr
 *
 */
public class TimeEstimator {
	public enum Function {
		LINEAR {
			long estimate(TimeEstimator est, double target) {
				double tDelta = System.currentTimeMillis() - est.window.getFirst().timeIndex;
				// Have at least 1ms before calculating anything
				if (tDelta < 1) {
					return -1;
				}
				TimeStamp first = est.window.getFirst();
				TimeStamp last = est.window.getLast();
				// Check if there was actually something "measured"
				if (last.value <= first.value) {
					return -1;
				}
				double tmp =  (last.timeIndex - first.timeIndex) * (target - last.value) / (last.value - first.value);
				return Math.round(tmp);
			}
		};
		
		abstract long estimate(TimeEstimator est, double target);
	}

    private static final double EPSILON = 0.00001;

	private final LinkedList<TimeStamp> window;
	private final long windowMillis;
	private volatile Function usedFunc = Function.LINEAR;
	
	private boolean filterDecreasingValues;
	
	/**
	 * Creates a TimeEstimator which uses a window of the given length.
	 * In addition, the estimation function is set to LINEAR
	 * @param windowMillis
	 */
	public TimeEstimator(long windowMillis) {
        window = new LinkedList<TimeStamp>();
		this.windowMillis = windowMillis;
	}
	
	/**
	 * Creates a TimeEstimator.
	 * The function used is LINEAR.
	 */
	public TimeEstimator() {
	    this(-1);
	}

	/**
	 * Sets a new function for estimation.
	 * @param func the function to use
	 */
	public void setEstimationFunction(Function func) {
		if (func == null) {
			throw new NullPointerException("Function parameter is null.");
		}
		usedFunc = func;
	}
	
	/**
	 * Updates the "real" progress.
	 * Time is an implicit parameter, aka it is retrieved using System.currentTimeMillis()
	 * @param val the value reached 
	 */
	public synchronized void addValue(double val) {
	    if (isFilterDecreasingValues() && !window.isEmpty() && window.getLast().value > val) {
	        return;
	    }
		TimeStamp t = new TimeStamp();
		t.timeIndex = System.currentTimeMillis();
		t.value = val;
		if (window != null) {
			while (!window.isEmpty() && window.getLast().value > val) {
				window.removeLast();
			}
			window.add(t);
			purgeOld();
		}
	}
	
	/**
	 * Returns the estimated time in milliseconds at which the value given in the parameter is reached. 
	 * @param toValue the value to be reached
	 * @return the time in milliseconds
	 */
	public synchronized long estimatedMillis(double toValue) {
	    if (!window.isEmpty() && window.getLast().value + EPSILON > toValue) {
	        return 0;
	    }
		// We need at least one stamp to calculate anything
		if (window.size() < Constants.ESTIMATION_MINVALUES) {
			return -1;
		}
		
		return usedFunc.estimate(this, toValue);
	}
	
	private void purgeOld() {
		if (windowMillis <= 0) {
			return;
		}
		TimeStamp t = window.getLast();
		// Make sure that there's always one stamp left, even if we're out of the window
		while (window.size() > Constants.ESTIMATION_MINVALUES && window.getFirst().timeIndex + windowMillis < t.timeIndex) {
			window.removeFirst();
		}
	}

	/**
	 * Returns true if updates with decreasing values should be ignored.
	 * @return
	 */
	public boolean isFilterDecreasingValues() {
        return filterDecreasingValues;
    }

    /**
     * Sets the value filtering to filter out decreasing value updates.
     * @param filterDecreasingValues
     */
    public void setFilterDecreasingValues(boolean filterDecreasingValues) {
        this.filterDecreasingValues = filterDecreasingValues;
    }

	private static class TimeStamp {
		private long timeIndex;
		private double value;
	}
}
