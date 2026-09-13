/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.util.compare;

/**
 * Compares names the way a person reads them: a run of digits counts as the number it spells, so
 * "2. Sitzung" comes before "10. Sitzung" instead of after it. Everything else is compared character
 * by character, ignoring case, exactly as before.
 * <p>
 * Two names that differ only in the leading zeros of a number ("01" and "1") are ordered by the plain
 * comparison, so this stays a total order and answers 0 for the same names a case-insensitive compare
 * answers 0 for - a sorted set keeps both entries.
 */
public final class NaturalOrder {

    private NaturalOrder() {
    }

    /**
     * @return a negative number, zero or a positive number as {@code s1} sorts before, with or after
     *         {@code s2}; 0 exactly when the two are equal ignoring case
     */
    public static int compareIgnoreCase(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return s1 == s2 ? 0 : (s1 == null ? -1 : 1);
        }

        int i1 = 0;
        int i2 = 0;
        int len1 = s1.length();
        int len2 = s2.length();

        while (i1 < len1 && i2 < len2) {
            char c1 = s1.charAt(i1);
            char c2 = s2.charAt(i2);

            if (isDigit(c1) && isDigit(c2)) {
                int end1 = endOfDigits(s1, i1);
                int end2 = endOfDigits(s2, i2);
                int numbers = compareNumbers(s1, i1, end1, s2, i2, end2);
                if (numbers != 0) {
                    return numbers;
                }
                i1 = end1;
                i2 = end2;
                continue;
            }

            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if (u1 != u2) {
                // Lower case too: the same fold String.compareToIgnoreCase applies, for the
                // alphabets where upper case alone maps two distinct letters onto one.
                char l1 = Character.toLowerCase(u1);
                char l2 = Character.toLowerCase(u2);
                if (l1 != l2) {
                    return l1 - l2;
                }
            }
            i1++;
            i2++;
        }

        if (i1 < len1 || i2 < len2) {
            return (len1 - i1) - (len2 - i2);
        }
        // Equal apart from leading zeros ("01" vs "1"): the plain comparison decides, so that only
        // names that really are the same answer 0.
        return s1.compareToIgnoreCase(s2);
    }

    /** The two digit runs as numbers, of any length - no parsing, so no overflow. */
    private static int compareNumbers(String s1, int start1, int end1, String s2, int start2, int end2) {
        int from1 = skipZeros(s1, start1, end1);
        int from2 = skipZeros(s2, start2, end2);

        int digits1 = end1 - from1;
        int digits2 = end2 - from2;
        if (digits1 != digits2) {
            return digits1 - digits2;
        }

        for (int i = 0; i < digits1; i++) {
            char d1 = s1.charAt(from1 + i);
            char d2 = s2.charAt(from2 + i);
            if (d1 != d2) {
                return d1 - d2;
            }
        }
        return 0;
    }

    private static int skipZeros(String s, int from, int end) {
        int i = from;
        while (i < end - 1 && s.charAt(i) == '0') {
            i++;
        }
        return i;
    }

    private static int endOfDigits(String s, int from) {
        int i = from;
        while (i < s.length() && isDigit(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
