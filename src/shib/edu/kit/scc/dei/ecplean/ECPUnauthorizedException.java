package edu.kit.scc.dei.ecplean;

/**
 * Thrown when a user could not be authorized.
 *
 * @author Sprajc
 */
public class ECPUnauthorizedException extends ECPAuthenticationException {

    private static final long serialVersionUID = 1L;

    public ECPUnauthorizedException() {
    }

    public ECPUnauthorizedException(String message) {
        super(message);
    }

    public ECPUnauthorizedException(Throwable cause) {
        super(cause);
    }

    public ECPUnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

}
