package de.dal33t.powerfolder.util.net;

import java.net.Inet4Address;

/**
 * This class represents an IP range in a subnet.
 * Instances are immutable. 
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision$
 */
public final class AddressRange {
	private Inet4Address start;
	private Inet4Address end;
	
	public AddressRange(Inet4Address start, Inet4Address end) {
		super();
		this.start = start;
		this.end = end;
	}
	
	public Inet4Address getEnd() {
		return end;
	}
	
	/**
	 * Checks if the given address is in range of the interval [start, end]. 
	 * @param addr the address to check
	 * @return true if the address is contained in this range
	 */
	public boolean contains(Inet4Address addr) {
		byte[] s = start.getAddress(), e = end.getAddress(), a = addr.getAddress();
		// Lexicographic compare
		for (int i = 0; i < s.length; i++) {
			int av = a[i] & 0xff, sv = s[i] & 0xff, ev = e[i] & 0xff;
			if (av > ev || av < sv) {
				return false;
			}
			if (av > sv && av < ev) {
				return true;
			} // else av == ev or av == sv=> continue
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((end == null) ? 0 : end.hashCode());
		result = PRIME * result + ((start == null) ? 0 : start.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AddressRange other = (AddressRange) obj;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		return true;
	}

	public Inet4Address getStart() {
		return start;
	}

	@Override
	public String toString() {
		return "[" + start + " - " + end + "]";
	}
}
