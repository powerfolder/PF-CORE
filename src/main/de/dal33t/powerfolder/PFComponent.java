/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder;

import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.logging.Loggable;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Base class for all classes, which use the Controller (most classes in
 * PowerFolder do).
 * 
 * @see Controller
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.13 $
 */
public abstract class PFComponent extends Loggable {
    /** The controller instance this is linked to */
    private Controller controller;
    private PropertyChangeSupport changeSupport;

    /**
     * Constructor for Controller ONLY.
     * <p>
     * WARNING: Never us this contructor
     */
    protected PFComponent() {
        // controller not set
    }

    protected PFComponent(Controller controller) {
        Reject.ifNull(controller, "Controller");
        this.controller = controller;
    }

    /**
     * Returns the controller where this component belongs to, gives access to
     * all PowerFolder core components.
     * 
     * @return the controller
     */
    public Controller getController() {
        return controller;
    }

    /**
     * Convenience method for <code>getController().getMySelf()</code>
     * 
     * @return myself
     */
    protected Member getMySelf() {
        return controller.getMySelf();
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
        getPropertyChangeSupport().firePropertyChange(propName, oldValue,
            newValue);
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
        getPropertyChangeSupport().firePropertyChange(propName, oldValue,
            newValue);
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
        getPropertyChangeSupport().firePropertyChange(propName, oldValue,
            newValue);
    }

    /**
     * Adds a property change listener
     * 
     * @param listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().addPropertyChangeListener(listener);
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
        getPropertyChangeSupport().addPropertyChangeListener(propertyName,
            listener);
    }

    /**
     * removes a property change listener
     * 
     * @param listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        getPropertyChangeSupport().removePropertyChangeListener(listener);
    }

    // General listener code **************************************************
    /**
     * Method to remove all listeners from this instance. Overwrite if you have
     * additional listers management
     */
    protected void removeAllListeners() {
        // Remove all property change listener
        PropertyChangeListener[] listener = getPropertyChangeSupport()
            .getPropertyChangeListeners();
        for (int i = 0; i < listener.length; i++) {
            removePropertyChangeListener(listener[i]);
        }
    }

    private synchronized PropertyChangeSupport getPropertyChangeSupport() {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        return changeSupport;
    }
}