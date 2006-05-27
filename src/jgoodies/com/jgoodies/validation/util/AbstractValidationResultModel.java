/*
 * Copyright (c) 2003-2006 JGoodies Karsten Lentzsch. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of JGoodies Karsten Lentzsch nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package com.jgoodies.validation.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.jgoodies.validation.Severity;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.ValidationResultModel;

/**
 * An abstract class that minimizes the effort required to implement
 * the {@link ValidationResultModel} interface. It provides a property
 * change support 
 * behavior to add and remove methods
 * to convert boolean, double, float, int, and long to their 
 * corresponding Object values.<p>
 * 
 * Subclasses must implement <code>getResult()</code> and 
 * <code>setResult(ValidationResult)</code> to get and set the observable 
 * validation result.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.3 $
 * 
 * @see com.jgoodies.validation.util.DefaultValidationResultModel
 * 
 * @since 1.0.1
 */
public abstract class AbstractValidationResultModel implements ValidationResultModel {

    /**
     * If any <code>PropertyChangeListeners</code> have been registered,
     * the <code>changeSupport</code> field describes them.
     *
     * @serial
     * @see #addPropertyChangeListener(PropertyChangeListener)
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     * @see #removePropertyChangeListener(PropertyChangeListener)
     * @see #removePropertyChangeListener(String, PropertyChangeListener)
     * @see #firePropertyChange(String, boolean, boolean)
     * @see #firePropertyChange(String, Object, Object)
     */
    private PropertyChangeSupport changeSupport;
    
    
    // Accessors **************************************************************

    /**
     * Looks up and returns the severity of the validation result,
     * one of error, warning, or <code>null</code>.
     * 
     * @return the severity of the validation result
     */
    public final Severity getSeverity() {
        return getResult().getSeverity();
    }
    
    
    /**
     * Checks and answers whether the validation result has errors.
     * 
     * @return true if the validation result has errors, false otherwise
     */
    public final boolean hasErrors() {
        return getResult().hasErrors();
    }
    
    
    /**
     * Checks and answers whether the validation result has messages.
     * 
     * @return true if the validation result has messages, false otherwise
     */
    public final boolean hasMessages() {
        return getResult().hasMessages();
    }
    
    
    // Convenience Behavior ***************************************************
    
    /**
     * Sets a new validation result and notifies all registered listeners
     * about changes of the result itself and the properties for severity,
     * errors and messages.
     * 
     * @param newResult  the validation result to be set
     * 
     * @see #setResult(ValidationResult)
     * @see ValidationResultModelContainer#setResult(ValidationResult)
     */
    protected final void firePropertyChanges(
            ValidationResult oldResult, ValidationResult newResult) {
        Severity oldSeverity = oldResult.getSeverity();
        boolean oldErrors    = oldResult.hasErrors();
        boolean oldMessages  = oldResult.hasMessages();
        Severity newSeverity = newResult.getSeverity();
        boolean newErrors    = newResult.hasErrors();
        boolean newMessages  = newResult.hasMessages();
        firePropertyChange(PROPERTYNAME_RESULT,   oldResult,   newResult);
        firePropertyChange(PROPERTYNAME_ERRORS,   oldErrors,   newErrors);
        firePropertyChange(PROPERTYNAME_MESSAGES, oldMessages, newMessages);
        firePropertyChange(PROPERTYNAME_SEVERITY, oldSeverity, newSeverity);
    }
    

    // Managing Property Change Listeners *************************************

   /**
    * Adds a PropertyChangeListener to the listener list. The listener is
    * registered for all bound properties of this class.<p>
    *  
    * If listener is null, no exception is thrown and no action is
    * performed.
    *
    * @param listener       the PropertyChangeListener to be added
    *
    * @see #removePropertyChangeListener(PropertyChangeListener)
    * @see #getPropertyChangeListeners()
    * @see #addPropertyChangeListener(String, PropertyChangeListener)
    */
   public final synchronized void addPropertyChangeListener(
                                           PropertyChangeListener listener) {
       if (listener == null) {
           return;
       }
       if (changeSupport == null) {
           changeSupport = new PropertyChangeSupport(this);
       }
       changeSupport.addPropertyChangeListener(listener);
   }
    
    
   /**
    * Removes a PropertyChangeListener from the listener list. This method
    * should be used to remove PropertyChangeListeners that were registered
    * for all bound properties of this class.<p>
    * 
    * If listener is null, no exception is thrown and no action is performed.
    *
    * @param listener       the PropertyChangeListener to be removed
    *
    * @see #addPropertyChangeListener(PropertyChangeListener)
    * @see #getPropertyChangeListeners()
    * @see #removePropertyChangeListener(String, PropertyChangeListener)
    */
   public final synchronized void removePropertyChangeListener(
                                       PropertyChangeListener listener) {
       if (listener == null || changeSupport == null) {
           return;
       }
       changeSupport.removePropertyChangeListener(listener);
   }
    
    
   /**
    * Adds a PropertyChangeListener to the listener list for a specific
    * property. The specified property may be user-defined.<p>
    * 
    * Note that if this Model is inheriting a bound property, then no event
    * will be fired in response to a change in the inherited property.<p>
    * 
    * If listener is null, no exception is thrown and no action is performed.
    *
    * @param propertyName       one of the property names listed above
    * @param listener           the PropertyChangeListener to be added
    *
    * @see #removePropertyChangeListener(String, PropertyChangeListener)
    * @see #getPropertyChangeListeners(String)
    * @see #addPropertyChangeListener(String, PropertyChangeListener)
    */
   public final synchronized void addPropertyChangeListener(
                                       String propertyName,
                                       PropertyChangeListener listener) {
       if (listener == null) {
           return;
       }
       if (changeSupport == null) {
           changeSupport = new java.beans.PropertyChangeSupport(this);
       }
       changeSupport.addPropertyChangeListener(propertyName, listener);
   }
    
    
   /**
    * Removes a PropertyChangeListener from the listener list for a specific
    * property. This method should be used to remove PropertyChangeListeners
    * that were registered for a specific bound property.<p>
    * 
    * If listener is null, no exception is thrown and no action is performed.
    *
    * @param propertyName       a valid property name
    * @param listener           the PropertyChangeListener to be removed
    *
    * @see #addPropertyChangeListener(String, PropertyChangeListener)
    * @see #getPropertyChangeListeners(String)
    * @see #removePropertyChangeListener(PropertyChangeListener)
    */
   public final synchronized void removePropertyChangeListener(
                                       String propertyName,
                                       PropertyChangeListener listener) {
       if (listener == null || changeSupport == null) {
           return;
       }
       changeSupport.removePropertyChangeListener(propertyName, listener);
   }
    
    
   /**
    * Returns an array of all the property change listeners
    * registered on this component.
    *
    * @return all of this component's <code>PropertyChangeListener</code>s
    *         or an empty array if no property change
    *         listeners are currently registered
    *
    * @see #addPropertyChangeListener(PropertyChangeListener)
    * @see #removePropertyChangeListener(PropertyChangeListener)
    * @see #getPropertyChangeListeners(String)
    * @see PropertyChangeSupport#getPropertyChangeListeners()
    */
   public final synchronized PropertyChangeListener[] getPropertyChangeListeners() {
       if (changeSupport == null) {
           return new PropertyChangeListener[0];
       }
       return changeSupport.getPropertyChangeListeners();
   }

   
   /**
    * Returns an array of all the listeners which have been associated 
    * with the named property.
    *
    * @param propertyName   the name of the property to lookup listeners
    * @return all of the <code>PropertyChangeListeners</code> associated with
    *         the named property or an empty array if no listeners have 
    *         been added
    *
    * @see #addPropertyChangeListener(String, PropertyChangeListener)
    * @see #removePropertyChangeListener(String, PropertyChangeListener)
    * @see #getPropertyChangeListeners()
    */
   public final synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
       if (changeSupport == null) {
           return new PropertyChangeListener[0];
       }
       return changeSupport.getPropertyChangeListeners(propertyName);
   }
   
   
   // Firing Changes for Bound Properties *************************************

   /**
    * Support for reporting bound property changes for Object properties. 
    * This method can be called when a bound property has changed and it will
    * send the appropriate PropertyChangeEvent to any registered
    * PropertyChangeListeners.
    *
    * @param propertyName       the property whose value has changed
    * @param oldValue           the property's previous value
    * @param newValue           the property's new value
    */
   protected final void firePropertyChange(String propertyName,
                                       Object oldValue,
                                       Object newValue) {
       PropertyChangeSupport aChangeSupport = this.changeSupport;
       if (aChangeSupport == null) {
           return;
       }
       aChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
   }
    
    
   /**
    * Support for reporting bound property changes for boolean properties. 
    * This method can be called when a bound property has changed and it will
    * send the appropriate PropertyChangeEvent to any registered
    * PropertyChangeListeners.
    *
    * @param propertyName       the property whose value has changed
    * @param oldValue           the property's previous value
    * @param newValue           the property's new value
    */
   protected final void firePropertyChange(String propertyName,
                                       boolean oldValue,
                                       boolean newValue) {
       PropertyChangeSupport aChangeSupport = this.changeSupport;
       if (aChangeSupport == null) {
           return;
       }
       aChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
   }
    

}
