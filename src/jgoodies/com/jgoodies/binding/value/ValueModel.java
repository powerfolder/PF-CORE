/*
 * Copyright (c) 2002-2006 JGoodies Karsten Lentzsch. All Rights Reserved.
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

package com.jgoodies.binding.value;

import java.beans.PropertyChangeListener;

/**
 * Describes models with a generic access to a single value that allow
 * to observe value changes. The value can be accessed using the 
 * <code>#getValue()</code>/<code>#setValue(Object)</code> methods.
 * Observers can register instances of <code>PropertyChangeListener</code> 
 * to be notified if the value changes.<p>
 * 
 * If the value is read-only or write-only, an implementor may choose
 * to reject an operation using an <code>UnsupportedOperationException</code> 
 * or may do nothing or perform an appropriate action, or may return an
 * appropriate value.<p>
 * 
 * The listeners registered with this ValueModel using #addValueChangeListener
 * will be invoked only with PropertyChangeEvents that have the name set to 
 * "value". 
 * In other words, the listeners won't get notified when a PropertyChangeEvent 
 * is fired that has a null object as the name to indicate an arbitrary set 
 * of the event source's properties have changed. This is the case 
 * if you use the PropertyChangeSupport, either directly or indirectly, 
 * to fire property changes with the property name "value" specified.
 * This constraint ensures that all ValueModel implementors behave 
 * like the AbstractValueModel subclasses. 
 * In the rare case, where you want to notify a PropertyChangeListener
 * even with PropertyChangeEvents that have no property name set,
 * you can register the listener with #addPropertyChangeListener,
 * not #addValueChangeListener.<p>
 *
 * AbstractValueModel minimizes the effort required to implement this interface. 
 * It uses the PropertyChangeSupport to fire PropertyChangeEvents, and it adds 
 * PropertyChangeListeners for the specific property name "value". This ensures
 * that the constraint mentioned above is met.<p>
 * 
 * Implementors are encouraged to provide non-null values for the 
 * PropertyChangeEvent's old and new values. However, both may be null.
 * 
 * @author  Karsten Lentzsch
 * @version $Revision: 1.2 $
 * 
 * @see AbstractValueModel
 * @see ValueHolder
 * @see com.jgoodies.binding.beans.PropertyAdapter
 */
public interface ValueModel {
    
    /**
     * Returns this model's value. In case of a write-only value,
     * implementors may choose to either reject this operation or 
     * or return <code>null</code> or any other appropriate value.
     * 
     * @return this model's value
     */
    Object getValue();
    
    
    /**
     * Sets a new value and notifies any registered value listeners 
     * if the value has changed. In case of a read-only value
     * implementors may choose to either reject this operation
     * or to do nothing.
     * 
     * @param newValue  the value to be set
     */
    void setValue(Object newValue);
    

    /**
     * Registers the given PropertyChangeListener with this 
     * ValueModel. The listener will be notified if the value has changed.
     * The PropertyChangeEvents delivered to the listener must have the name 
     * set to "value". The latter ensures that all ValueModel implementors
     * behave like the AbstractValueModel subclasses.<p>
     * 
     * To comply with the above specification implementors can use
     * the PropertyChangeSupport's #addPropertyChangeListener method
     * that accepts a property name, so that listeners will be invoked only
     * if that specific property has changed.
     * 
     * @param listener  the listener to be added
     * 
     * @see AbstractValueModel#addValueChangeListener(PropertyChangeListener)
     */
    void addValueChangeListener(PropertyChangeListener listener);
    
    
    /**
     * Deregisters the given PropertyChangeListener from this ValueModel.
     * 
     * @param listener  the listener to be removed
     */
    void removeValueChangeListener(PropertyChangeListener listener);
    
    
}
