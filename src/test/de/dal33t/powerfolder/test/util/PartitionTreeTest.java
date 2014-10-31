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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
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
