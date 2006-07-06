package de.dal33t.powerfolder.event;

/**
 * Base class for all Listeners in PowerFolder that are handled with the
 * ListenerSupportFactory. This provides an general interface so that it is
 * posible for the class that listens to the event can decide if the event
 * should be fired in the event DispathThread. Use with care if your
 * fireInEventDispathThread() method return true, alway make it a quick
 * implemetation because userimput will be frozen during excution. Note that if
 * there is No "awtAvailable" the events are fired in the current thread
 * regardless of the return value of fireInEventDispathThread.
 */
public interface CoreListener {
    /**
     * Overwrite this method to indicate if the ListenerSupportFactory should
     * fire this event in the Swing Event Dispath Thread. Use with care if you
     * return true, alway make it a quick implemetation because user input and
     * the user inerface will be frozen during execution. Don't make the
     * implmentation of this method dynamic, when adding the listener to the
     * class that fires the events this method is evaluated.<BR>
     * Also when you return true when this method is added and false if the
     * method is removed from the class that fires the events will result in
     * undefined behaviour.
     * 
     * @return true if the events should be fired in the Swing Event Dispath
     *         Thread.
     */
    public boolean fireInEventDispathThread();
}
