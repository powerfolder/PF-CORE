package de.dal33t.powerfolder.test.util;

import de.dal33t.powerfolder.util.Partitions;
import de.dal33t.powerfolder.util.Range;

import junit.framework.TestCase;

public class PartitionTreeTest extends TestCase {
	public void testTree() {
		Partitions<Boolean> ds = new Partitions<Boolean>(Range.getRangeByNumbers(0, 9999), false);
		// Tree test
		// The depth of the tree shouldn't exceed log2(9999) = 13.xxx (With xxx > 0 therefore depth 14)
		for (int i = 0; i < 9999; i++) {
			ds.insert(Range.getRangeByNumbers(i, i), (i & 1) == 0);
		}
		assertEquals(14, ds.depth());
		for (int i = 0; i < 9999; i++) {
			ds.insert(Range.getRangeByNumbers(i, 9999), (i & 1) == 0);
		}
		assertEquals(14, ds.depth());
		for (int i = 0; i < 9999; i++) {
			ds.insert(Range.getRangeByNumbers(0, i), (i & 1) == 0);
		}
		assertEquals(0, ds.depth());
		for (int i = 0; i < 5000; i++) {
			ds.insert(Range.getRangeByNumbers(i, 9999 - i), (i & 1) == 0);
		}
		assertEquals(14, ds.depth());

		ds.insert(Range.getRangeByNumbers(0, 9999), false);
		for (int i = 0; i < 4999; i++) {
			ds.insert(Range.getRangeByNumbers(i, i), true);
			ds.insert(Range.getRangeByNumbers(9999 - i, 9999 - i), true);
			Range r = ds.search(Range.getRangeByNumbers(0, 9999), false);
			assertEquals(i + 1, r.getStart());
			assertEquals(9998 - i, r.getEnd());
		}
		assertEquals(9998, ds.count(Range.getRangeByNumbers(0, 9999), true));
		assertEquals(2, ds.count(Range.getRangeByNumbers(0, 9999), false));
	}
}
