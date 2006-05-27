package de.dal33t.powerfolder.util;
/**
 * Helper class to build strings fast, String + String is very slow 
 * and StringBuffer is synchronized (also slow). 
 * Not synchronized (like StringBuilder in Java 5.0) but for Java 1.x 
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.3 $
 */
public class BuildStrings {
	private static final int INITIAL_SIZE = 40;
	private static final int GROW_FACTOR = 2;

	char value[];
	int lastCharIndex;
	
	public BuildStrings() {
		this(INITIAL_SIZE);
	}
	
	public BuildStrings(int capacity) {
		value = new char[capacity];
	}
	
	public BuildStrings(String str) {
		this(str.length()+INITIAL_SIZE);
		append(str);
	}
	
	public void append(String str) {
		if (str != null) {
			int length = str.length();
			if (length != 0) {
				int newSize = length + lastCharIndex;
				if (newSize > value.length) {
					grow(newSize);
				}
				str.getChars(0, length, value, lastCharIndex);
				lastCharIndex = newSize;
			}
		}
	}
	
	private final void grow(int minimumSize) {
		int newSize = GROW_FACTOR * (value.length+1);
		if (newSize < 0) { //wow this was way to big!
			newSize = Integer.MAX_VALUE;		
		} else if (newSize < minimumSize) {
			newSize = minimumSize;
		}
		char[] newValue = new char[newSize];
		System.arraycopy(value, 0, newValue, 0, lastCharIndex);
		value = newValue;
	}
	
	public String toString() {
        return new String(value, 0, lastCharIndex);
	}
}
