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
 * $Id$
 */
package de.dal33t.powerfolder.util;

/**
 * Used to easily add gurading clauses.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class Reject {
    private Reject() {
        // No instance allowed
    }

    /**
     * Checks if the given object is null. If yes a
     * <code>NullPointException</code> will be thrown with the given message
     *
     * @param obj
     *            the object to check
     * @param message
     *            the message for the NPE
     * @throws NullPointerException
     *             if obj is null
     */
    public static void ifNull(Object obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
    }

    /**
     * Checks if the given argument of a method is null. If true, will throw a
     * <code>IllegalArgumentException</code> stating the name of the argument
     * that was null.
     *
     * @param obj
     * @param argumentName
     */
    public static void notNull(Object obj, String argumentName) {
        if (obj == null) {
            throw new IllegalArgumentException("Argument '" + argumentName
                + "' was null!");
        }
    }

    /**
     * Checks if any of the given objects is null. If one is null, a
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param objs
     *            the objects where each is expected to be not null
     */
    public static void noNullElements(Object... objs) {
        Validate.noNullElements(objs);
    }

    /**
     * Checks if the given string is blank. If yes a
     * <code>IllegalArgumentException</code> will be thrown with the given
     * message
     *
     * @param str
     *            the string to check
     * @param message
     *            the message for the IAE
     * @throws IllegalArgumentException
     *             if str is blank
     */
    public static void ifBlank(String str, String message) {
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks if the given expression is true. If yes a
     * <code>IllegalArgumentException</code> will be thrown with the given
     * message
     *
     * @param expression
     *            the expression to check
     * @param message
     *            the message of the execption
     * @throws IllegalArgumentException
     *             if expression is true
     */
    public static void ifTrue(boolean expression, String message) {
        if (expression) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks if the given expression is false. If yes a
     * <code>IllegalArgumentException</code> will be thrown with the given
     * message
     *
     * @param expression
     *            the expression to check
     * @param message
     *            the message of the execption
     * @throws IllegalArgumentException
     *             if expression is true
     */
    public static void ifFalse(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }
}
