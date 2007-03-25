/* $Id: Reject.java,v 1.2 2006/03/12 23:12:43 totmacherr Exp $
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
