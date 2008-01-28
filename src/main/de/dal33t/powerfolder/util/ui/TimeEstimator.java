package de.dal33t.powerfolder.util.ui;

import java.util.LinkedList;

/**
 * Generic class to estimate progress.
 * Using an optional sliding window, this class tries to estimate the time at which a given value is reached.
 * To do this it requires updates on the real value - the estimation is done using linear extrapolation.
 * @author Dennis "Bytekeeper" Waldherr
 *
 */
public class TimeEstimator {
	private LinkedList<TimeStamp> window;
	private long windowMillis;
	private double accum;
	private double lastValue = 0;
	
	public TimeEstimator(long windowMillis) {
		window = new LinkedList<TimeStamp>();
		this.windowMillis = windowMillis;
	}
	
	public TimeEstimator() {
	}
	
	public synchronized void addValue(double val) {
		TimeStamp t = new TimeStamp();
		t.timeIndex = System.currentTimeMillis();
		t.delta = val - lastValue;
		lastValue = val;
		if (window != null) {
			while (t.delta < 0 && !window.isEmpty()) {
				t.delta += window.removeLast().delta;
			}
			window.add(t);
			purgeOld();
		}
		accum += t.delta;
	}
	
	public synchronized long estimatedMillis(double toValue) {
		// We need at least one stamp to calculate anything
		if (window.isEmpty()) {
			return -1;
		}
		
		double tDelta = System.currentTimeMillis() - window.getFirst().timeIndex;
		// Have at least 1ms before calculating anything
		if (tDelta < 1) {
			return -1;
		}
		double tmp = (toValue - lastValue) / (accum / tDelta);
		return Math.round(tmp);
	}
	
	private void purgeOld() {
		if (window == null) {
			return;
		}
		TimeStamp t = window.getLast();
		// Make sure that there's always one stamp left, even if we're out of the window
		while (window.size() > 1 && window.getFirst().timeIndex + windowMillis < t.timeIndex) {
			accum -= window.removeFirst().delta;
		}
	}

	private static class TimeStamp {
		private long timeIndex;
		private double delta;
	}
}
