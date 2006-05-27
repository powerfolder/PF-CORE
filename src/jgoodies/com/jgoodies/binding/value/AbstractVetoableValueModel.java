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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;



/**
 * A ValueModel that allows to accept or reject proposed value changes.
 * Useful to request information from the user or to perform operations
 * before a value is changed.<p>
 * 
 * Wraps a given subject ValueModel and always returns the subject value
 * as this model's value. Observes subject value changes and forwards
 * them to listeners of this model. If a value is set to this model,
 * the abstract method <code>#proposedChange</code> is invoked. In this method
 * implementors define how to accept or reject value changes.<p>
 * 
 * Implementors may veto against a proposed change based on the application
 * state or by asking the user, and may also perform additional operations 
 * during the check, for example to save editor contents. Here's an example:
 * <pre>
 * public class CheckPendingEditValueModel extends AbstractVetoableValueModel {
 * 
 *     public CheckPendingEditValueModel(ValueModel subject) {
 *         super(subject);
 *     }
 * 
 *     public boolean proposedChange(Object oldValue, Object proposedNewValue) {
 *         if (equals(oldValue, proposedNewValue))
 *             return true;
 *         int option = JOptionPane.showConfirmDialog(
 *             Application.getDefaultParentFrame(),
 *             "Do you want to save the editor contents.");
 *         if (option == JOptionPane.YES_OPTION)
 *             model.save();
 *         return option != JOptionPane.CANCEL_OPTION;    
 *     }
 * }
 * </pre>
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @since 1.1
 */
public abstract class AbstractVetoableValueModel extends AbstractValueModel {
    
    /**
     * Holds the wrapped subject ValueModel that is used to read values from
     * and commit accepted changes to.
     */
    private final ValueModel subject;
    
    
    // Instance Creation ******************************************************
    
    /**
     * Constructs an AbstractVetoableValueModel for the given ValueModel.
     * 
     * @param subject   the underlying (or wrapped) ValueModel 
     *     
     * @throws NullPointerException if the subject is <code>null</code>
     */
    protected AbstractVetoableValueModel(ValueModel subject) {
        this.subject = subject;
        subject.addValueChangeListener(new SubjectValueChangeHandler());
    }
    
    
    // Abstract Behavior ******************************************************
    
    /**
     * Checks and answers whether the proposed value change shall be
     * accepted or rejected. Implementors may perform additional
     * operations, for example to save a pending editor content.
     * 
     * @param oldValue          the value before the change
     * @param proposedNewValue  the new value if the change is accepted
     * @return true to accept the proposed value, false to veto against it.
     */
    public abstract boolean proposedChange(Object oldValue, Object proposedNewValue);
    
    
    // ValueModel Implementation **********************************************
    
    /**
     * Returns this model's current subject value.
     * 
     * @return this model's current subject value.
     */
    public final Object getValue() {
        return subject.getValue();
    }
    
    
    /**
     * Sets the given value as new subject value if and only if
     * 1) the new value differs from the old value and 
     * 2) the proposed change is accepted as checked by 
     * <code>proposedChange(oldValue, newValue)</code>.
     * 
     * @param newValue   the value to set
     */
    public final void setValue(Object newValue) {
        Object oldValue = getValue();
        if (oldValue == newValue)
            return;
        if (proposedChange(oldValue, newValue))
            subject.setValue(newValue);
    }
    
    
    // Event Handling *****************************************************
    
    /** 
     * Forwards value changes in the subject to listeners of this model. 
     */
    private class SubjectValueChangeHandler implements PropertyChangeListener {
        
        public void propertyChange(PropertyChangeEvent evt) {
            fireValueChange(evt.getOldValue(), evt.getNewValue(), true);
        }
    }
    
}
