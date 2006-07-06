package de.dal33t.powerfolder.event;

/** implement to receive events from the NavigationModel */
public interface NavigationListener {
    /**
     * fired if the current navigation item chanches, like the selected item in
     * the tree, or clicked on a navigation button (up/down/back)
     */
    public void navigationChanged(NavigationEvent event);
}
