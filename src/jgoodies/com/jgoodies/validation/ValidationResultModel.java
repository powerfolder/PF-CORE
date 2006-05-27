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

package com.jgoodies.validation;

import java.beans.PropertyChangeListener;


/**
 * Describes a model that holds a {@link ValidationResult} and provides bound 
 * read-only properties for the result, severity, error and messages state.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.1 $
 * 
 * @see     com.jgoodies.validation.util.DefaultValidationResultModel
 * 
 * @since 1.1
 */
public interface ValidationResultModel {

    /**
     * The name of the bound property for the validation result.
     * 
     * @see #getResult()
     */
    String PROPERTYNAME_RESULT = "result";
    
    
    /**
     * The name of the bound property for the validation result severity.
     * 
     * @see #getSeverity()
     */
    String PROPERTYNAME_SEVERITY = "severity";
    
    
    /**
     * The name of the bound property that indicates whether there are errors.
     * 
     * @see #hasErrors()
     */
    String PROPERTYNAME_ERRORS = "errors";
    
    
    /**
     * The name of the bound property that indicates whether there are messages.
     * 
     * @see #hasMessages()
     */
    String PROPERTYNAME_MESSAGES = "messages";
    
    
    // Accessors **************************************************************

    /**
     * Returns this model's validation result.
     * 
     * @return the current validation result
     * 
     * @see #setResult(ValidationResult)
     */
    ValidationResult getResult();
    
    
    /**
     * Sets a new validation result and notifies all registered listeners
     * if the result changed. This is typically invoked at the end of
     * the <code>#validate()</code> method.
     * 
     * @param newResult  the validation result to be set
     * 
     * @see #getResult()
     */
    void setResult(ValidationResult newResult);
    

    /**
     * Looks up and returns the Severity of this model's validation result,
     * one of <code>Severity.ERROR</code>, <code>Severity.WARNING</code>, 
     * or <code>Severity.OK</code>.
     * 
     * @return the severity of this model's validation result
     * 
     * @see #hasErrors()
     * @see #hasMessages()
     */
    Severity getSeverity();
    
    
    /**
     * Checks and answers whether this model's validation result has errors.
     * 
     * @return true if the validation result has errors, false otherwise
     * 
     * @see #getSeverity()
     * @see #hasMessages()
     */
    boolean hasErrors();
    
    
    /**
     * Checks and answers whether this model's validation result has messages.
     * 
     * @return true if the validation result has messages, false otherwise
     * 
     * @see #getSeverity()
     * @see #hasErrors()
     */
    boolean hasMessages();
    
    
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
    * @see #removePropertyChangeListener(String, PropertyChangeListener)
    * @see #addPropertyChangeListener(String, PropertyChangeListener)
    * @see #getPropertyChangeListeners()
    */
   void addPropertyChangeListener(PropertyChangeListener listener);
    
   
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
    * @see #addPropertyChangeListener(String, PropertyChangeListener)
    * @see #removePropertyChangeListener(String, PropertyChangeListener)
    * @see #getPropertyChangeListeners()
    */
   void removePropertyChangeListener(PropertyChangeListener listener);
    
   
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
    * @see #removePropertyChangeListener(PropertyChangeListener)
    * @see #removePropertyChangeListener(String, PropertyChangeListener)
    * @see #addPropertyChangeListener(PropertyChangeListener)
    * @see #getPropertyChangeListeners(String)
    */
   void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);
    
    
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
    * @see #addPropertyChangeListener(PropertyChangeListener)
    * @see #addPropertyChangeListener(String, PropertyChangeListener)
    * @see #removePropertyChangeListener(PropertyChangeListener)
    * @see #getPropertyChangeListeners(String)
    */
   void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);
    
    
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
    * @see java.beans.PropertyChangeSupport#getPropertyChangeListeners()
    */
   PropertyChangeListener[] getPropertyChangeListeners();

   
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
   PropertyChangeListener[] getPropertyChangeListeners(String propertyName);
   

   
}
