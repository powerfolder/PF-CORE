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

/**
 * A ValueModel implementation that is intended to be used as trigger channel 
 * for instances of BufferedValueModel. API users shall trigger commit and flush 
 * events using <code>#triggerCommit</code> and <code>#triggerFlush</code>.<p>
 * 
 * This Trigger class works around an inconvenient situation when using
 * a general ValueHolder as trigger channel of a BufferedValueModel. 
 * BufferedValueHolder performs commit and flush events only if the trigger 
 * channel value reports a change. And a ValueHolder doesn't report a change 
 * if <code>#setValue</code> tries to set the current value. For example
 * if you set <code>Boolean.TRUE</code> twice, the latter doesn't fire 
 * a property change event. The methods <code>#triggerCommit</code> and 
 * <code>#triggerFlush</code> check for the current state and guarantee
 * that the appropriate <code>PropertyChangeEvent</code> is fired. 
 * On the other hand, the implementation minimizes the number of events 
 * necessary to commit or flush buffered values.<p>
 * 
 * <strong>Constraints:</strong> The value is of type <code>Boolean</code>.
 * <p>
 * The following example delays the commit of a buffered value:
 * <pre>
 * ValueModel subject = new ValueHolder();
 * Trigger trigger = new Trigger();
 * BufferedValueModel buffer = new BufferedValueModel(subject, trigger);
 * 
 * buffer.setValue("value");
 * ...
 * trigger.triggerCommit();
 * </pre>
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.2 $
 * 
 * @see     BufferedValueModel
 */
public final class Trigger extends AbstractValueModel {

    private static final Boolean COMMIT  = Boolean.TRUE;
    private static final Boolean FLUSH   = Boolean.FALSE;
    private static final Boolean NEUTRAL = null;
    
    /** 
     * Holds the current trigger state.
     */
    private Boolean value;
    

    // Instance Creation ******************************************************

    /**
     * Constructs a Trigger set to neutral.
     */
    public Trigger() {
        value = NEUTRAL;
    }
    

    // ValueModel Implementation **********************************************
    
    /**
     * Returns a Boolean that indicates the current trigger state.
     * 
     * @return a Boolean that indicates the current trigger state 
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets a new Boolean value and rejects all non-Boolean values.
     * Fires no change event if the new value is equal to the 
     * previously set value.<p>
     * 
     * This method is not intended to be used by API users.
     * Instead you should trigger commit and flush events by invoking
     * <code>#triggerCommit</code> or <code>#triggerFlush</code>. 
     * 
     * @param newValue  the Boolean value to be set
     * @throws IllegalArgumentException   if the newValue is not a Boolean
     */
    public void setValue(Object newValue) {
        if ((newValue != null) && !(newValue instanceof Boolean))
            throw new IllegalArgumentException(
                "Trigger values must be of type Boolean.");
        
        Object oldValue = value;
        value = (Boolean) newValue;
        fireValueChange(oldValue, newValue);
    }


    // Triggering *************************************************************
    
    /**
     * Triggers a commit event in BufferedValueModels that share this Trigger.
     * Sets the value to <code>Boolean.TRUE</code> and ensures that dependents 
     * are notified about a value change to this new value. Only if necessary 
     * the value is temporarily set to <code>null</code>. This way it minimizes
     * the number of PropertyChangeEvents fired by this Trigger.
     */
    public void triggerCommit() {
        if (COMMIT.equals(getValue()))
            setValue(NEUTRAL);
        setValue(COMMIT);
    }
    
    /**
     * Triggers a flush event in BufferedValueModels that share this Trigger.
     * Sets the value to <code>Boolean.FALSE</code> and ensures that dependents 
     * are notified about a value change to the new value. Only if necessary 
     * the value is temporarily set to <code>null</code>.  This way it minimizes
     * the number of PropertyChangeEvents fired by this Trigger.
     */
    public void triggerFlush() {
        if (FLUSH.equals(getValue()))
            setValue(NEUTRAL);
        setValue(FLUSH);
    }
    
    
}
