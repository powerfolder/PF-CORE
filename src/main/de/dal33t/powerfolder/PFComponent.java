/* $Id: PFComponent.java,v 1.13 2006/03/04 20:57:41 schaatser Exp $
 */
package de.dal33t.powerfolder;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import de.dal33t.powerfolder.util.Loggable;

/**
 * Base class for all classes, which use the Controller (most classes in PowerFolder do). Gives also access to loggin and PropertyChangeSupport.
 * After extending from this class make sure the Controller is set in the Contructor.<BR> 
 * Log example: 
 * <CODE>
 * log().debug("This is a debug log text");
 * </CODE>
 * see Logger for more info.
 * 
 * @see de.dal33t.powerfolder.Controller, de.dal33t.powerfolder.util.Logger
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public abstract class PFComponent extends Loggable {
    /** The controller instance this is liked to */
    private Controller controller;
    private PropertyChangeSupport changeSupport;
   
    /**
     * Constructor for Controller ONLY.
     * <p>
     * WARNING: Never us this contructor
     */
    protected PFComponent() {
        // controller not set
        changeSupport = new PropertyChangeSupport(this);   
    }

    protected PFComponent(Controller controller) {
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        this.controller = controller;

        // Init event supports
        changeSupport = new PropertyChangeSupport(this);        
    }

    /**
     * Returns the controller where this componentent belogs to
     * 
     * @return
     */
    public Controller getController() {
        return controller;
    }

    // Property change event codes ********************************************

    /**
     * Fires a property change event on a property
     * 
     * @param propName
     * @param oldValue
     * @param newValue
     */
    protected void firePropertyChange(String propName, Object oldValue,
        Object newValue)
    {
        changeSupport.firePropertyChange(propName, oldValue, newValue);
    }

    /**
     * Fires a property change event on a property for <code>boolean</code>
     * 
     * @param propName
     * @param oldValue
     * @param newValue
     */
    protected void firePropertyChange(String propName, boolean oldValue,
        boolean newValue)
    {
        changeSupport.firePropertyChange(propName, oldValue, newValue);
    }

    /**
     * Fires a property change event on a property for <code>int</code>
     * 
     * @param propName
     * @param oldValue
     * @param newValue
     */
    protected void firePropertyChange(String propName, int oldValue,
        int newValue)
    {
        changeSupport.firePropertyChange(propName, oldValue, newValue);
    }

    /**
     * Adds a property change listener
     * 
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Adds a property change listener on a property
     * 
     * @param propertyName
     * @param listener
     */
    public void addPropertyChangeListener(String propertyName,
        PropertyChangeListener listener)
    {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * removes a property change listener
     * 
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    // General listener code **************************************************
    /**
     * Method to remove all listener from this instance. Overwrite if you have
     * additional listers management
     */
    protected void removeAllListener() {
        // Remove all property change listener
        PropertyChangeListener[] listener = changeSupport
            .getPropertyChangeListeners();
        for (int i = 0; i < listener.length; i++) {
            removePropertyChangeListener(listener[i]);
        }
    }
}