package de.dal33t.powerfolder.util;

public class StackDump extends RuntimeException {
    private static final long serialVersionUID = 100L;

    public StackDump() {
        super();
    }

    public StackDump(String message) {
        super(message);
    }
}
