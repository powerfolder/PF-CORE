package de.dal33t.powerfolder.test.util;

import org.apache.commons.lang.math.IntRange;

import de.dal33t.powerfolder.util.Partitions;

import junit.framework.TestCase;

public class PartitionTreeTest extends TestCase {
	public void testTree() {
		Partitions<Boolean> ds = new Partitions<Boolean>(new IntRange(0, 9999), false);
		// Tree test
		// The depth of the tree shouldn't exceed log2(9999) = 13.xxx (With xxx > 0 therefore depth 14)
		for (int i = 0; i < 9999; i++) {
			ds.insert(new IntRange(i, i), (i & 1) == 0);
		}
		assertEquals(14, ds.depth());
		for (int i = 0; i < 9999; i++) {
			ds.insert(new IntRange(i, 9999), (i & 1) == 0);
		}
		assertEquals(11, ds.depth());
		for (int i = 0; i < 9999; i++) {
			ds.insert(new IntRange(0, i), (i & 1) == 0);
		}
		assertEquals(0, ds.depth());
		for (int i = 0; i < 5000; i++) {
			ds.insert(new IntRange(i, 9999 - i), (i & 1) == 0);
		}
		assertEquals(14, ds.depth());

	}
}
