/* $Id: Problem.java,v 1.6 2005/05/03 03:39:04 totmacherr Exp $
 */
package de.dal33t.powerfolder.message;

/**
 * General problem response
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class Problem extends Message {
    private static final long serialVersionUID = 100L;

    // The problem codes
    public static final int DISCONNECTED = 7;
    public static final int YOU_ARE_BORING = 666;

    // The problem code
    public int problemCode;

    public String message;
    /** Indicates, that this is a fatal problem, will disconnect */
    public boolean fatal;

    public Problem() {
        // Serialisation constructor
    }

    /**
     * Initalizes a problem with fatal flag
     * 
     * @param message
     * @param fatal
     */
    public Problem(String message, boolean fatal) {
        this.message = message;
        this.fatal = fatal;
    }

    /**
     * Constructs a problem with an problem code
     * 
     * @param message
     * @param fatal
     * @param pCode
     */
    public Problem(String message, boolean fatal, int pCode) {
        this(message, fatal);
        this.problemCode = pCode;
    }

    public String toString() {
        return "Problem: '" + message + "'";
    }
}