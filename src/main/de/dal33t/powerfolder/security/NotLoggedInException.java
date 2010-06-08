package de.dal33t.powerfolder.security;

public class NotLoggedInException extends SecurityException {

    private static final long serialVersionUID = 100L;

    public NotLoggedInException() {
        super("Not logged in");
    }

}
