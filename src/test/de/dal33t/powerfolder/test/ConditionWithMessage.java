
package de.dal33t.powerfolder.test;

public interface ConditionWithMessage extends Condition {
    /**
     * @return the message to be displayed when not reaching the condition.
     */
    String message();
}
