package de.dal33t.powerfolder.util;

import java.util.Date;

public class StackDump extends RuntimeException {
    private static final long serialVersionUID = 100L;
    private Date created = new Date();

    public StackDump() {
        super();
    }

    public Date getCreated() {
        return created;
    }

    public StackDump(String message) {
        super(message);
    }
}
