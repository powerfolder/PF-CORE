package de.dal33t.powerfolder.util.test;

public abstract class EqualsCondition implements ConditionWithMessage {

    public String message() {
        return "Expected: " + expected() + ", Actual: " + actual();
    }

    public boolean reached() {
        Object a = expected();
        Object b = actual();
        if (a == null) {
            return false;
        }
        if (b == null) {
            return false;
        }
        if (a == b) {
            return true;
        }
        return a.equals(b);
    }

    public abstract Object expected();

    public abstract Object actual();
}
