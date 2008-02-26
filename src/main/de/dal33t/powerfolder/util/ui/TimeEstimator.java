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
	};

	private LinkedList<TimeStamp> window;
	private long windowMillis;
	private volatile Function usedFunc = Function.LINEAR;
	
	/**
	 * Creates a TimeEstimator which uses a window of the given length.
	 * In addition, the estimation function is set to LINEAR
	 * @param windowMillis
	 */
	public TimeEstimator(long windowMillis) {
		this();
		this.windowMillis = windowMillis;
	}
	
	/**
	 * Creates a TimeEstimator.
	 * The function used is LINEAR.
	 */
	public TimeEstimator() {
		window = new LinkedList<TimeStamp>();
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
		// We need at least one stamp to calculate anything
		if (window.size() < Constants.ESTIMATION_MINVALUES) {
			return -1;
		}
		
		return usedFunc.estimate(this, toValue);
	}
	
	private void purgeOld() {
		if (window == null) {
			return;
		}
		TimeStamp t = window.getLast();
		// Make sure that there's always one stamp left, even if we're out of the window
		while (window.size() > Constants.ESTIMATION_MINVALUES && window.getFirst().timeIndex + windowMillis < t.timeIndex) {
			window.removeFirst();
		}
	}

	private static class TimeStamp {
		private long timeIndex;
		private double value;
	}
}
