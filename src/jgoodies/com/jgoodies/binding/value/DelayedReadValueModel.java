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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Timer;

/**
 * A ValueModel that deferres updates and read-access for a specified delay.
 * Useful to coalesce frequent changes. For example if a heavy computation
 * shall be performed only for a "stable" selection after a series of
 * quick selection changes.<p>
 * 
 * Wraps a given subject ValueModel and observes subject value changes
 * and forwards them to listeners of this model after a delay. If the subject 
 * value changes, a Swing Timer is used to delay the change notification. 
 * A previously started timer - if any - will be stopped before.
 * Reading this model's value returns:
 *  a) the subject value if there's no pending update, or 
 *  b) this model's old value that will be updated after the delay.
 * If a value is set to this model, it immediately updates the subject value.<p>
 * 
 * TODO: Describe how and when listeners get notified about the delayed change.<p>
 * 
 * TODO: Write about the recommended delay time - above the double-click time
 * and somewhere below a second, e.g. 100ms to 200ms.<p>
 * 
 * TODO: Write about a slightly different commit handling. The current 
 * implementation defers the commit until the value is stable for the 
 * specified delay; it's a DelayUntilStableForXXXmsValueModel. Another
 * feature is to delay for a specified time but ensure that some commits
 * and change notifications happen. The latter is a CoalescingWriteValueModel.<p>
 * 
 * TODO: Summarize the differences between the DelayedReadValueModel, the
 * DelayedWriteValueModel, and this DelayedPropertyChangeHandler.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see javax.swing.Timer
 * 
 * @since 1.1
 */
public final class DelayedReadValueModel extends AbstractValueModel {
    
    /**
     * Refers to the underlying subject ValueModel.
     */
    private final ValueModel subject;
    
    /**
     * The Timer used to perform the delayed commit.
     */
    private final Timer timer;
    
    /**
     * If <code>true</code> all pending updates will be coalesced.
     * In other words, an update will be fired if no updates
     * have been received for this model's delay. 
     */
    private boolean coalesce;
    
    /**
     * Holds this model's old value that is returned in <code>getValue</code>
     * during a pending change.  most recent old value. It is set in 
     * <code>#fireDelayedValueChange</code>.
     */
    private Object oldValue;
    
    /**
     * Holds the most recent pending PropertyChangeEvent as provided
     * from the subject change notification that this model deferres.
     * <code>#fireDelayedValueChange</code>.
     */
    private PropertyChangeEvent pendingEvt;
    
    
    // Instance Creation ******************************************************
    
    /**
     * Constructs a DelayedReadValueModel for the given subject ValueModel
     * and the specified Timer delay in milliseconds with coalescing disabled.
     * 
     * @param subject   the underlying (or wrapped) ValueModel 
     * @param delay     the milliseconds to wait before a change
     *     shall be committed
     *     
     * @throws IllegalArgumentException if the delay is negative
     */
    public DelayedReadValueModel(ValueModel subject, int delay) {
        this(subject, delay, false);
    }
    
    
    /**
     * Constructs a DelayedReadValueModel for the given subject ValueModel
     * and the specified Timer delay in milliseconds using the given
     * coalesce mode.
     * 
     * @param subject   the underlying (or wrapped) ValueModel 
     * @param delay     the milliseconds to wait before a change
     *     shall be committed
     * @param coalesce  <code>true</code> to coalesce all pending changes,
     *     <code>false</code> to fire changes with the delay when an update
     *     has been received
     *     
     * @throws IllegalArgumentException if the delay is negative
     * 
     * @see #setCoalesce(boolean)
     */
    public DelayedReadValueModel(ValueModel subject, int delay, boolean coalesce) {
        this.subject = subject;
        this.coalesce = coalesce;
        this.timer = new Timer(delay, new ValueUpdateListener());
        timer.setRepeats(false);
        subject.addValueChangeListener(new SubjectValueChangeHandler());
        oldValue = subject.getValue();
    }
    
    
    // ValueModel Implementation ******************************************
    
    /**
     * Returns the subject's value or in case of a pending commit,
     * the pending new value.
     * 
     * @return the subject's current or future value.
     */
    public Object getValue() {
        return hasPendingChange()
            ? oldValue
            : subject.getValue();
    }
    
    
    /**
     * Sets the given new value immediately as the subject's new value.
     * Note that change notifications from the subject are deferred 
     * by this model. Therefore listeners registered with this model 
     * will be notified after this model's delay.
     * 
     * @param newValue   the value to set
     */
    public void setValue(Object newValue) {
        subject.setValue(newValue);
    }
    
    
    // Accessors **************************************************************
    
    /**
     * Returns the delay, in milliseconds, that is used to defer value change
     * notifications.
     *
     * @return the delay, in milliseconds, that is used to defer 
     *     value change notifications
     * 
     * @see #setDelay
     */
    public int getDelay() {
        return timer.getDelay();
    }

    /**
     * Sets the delay, in milliseconds, that is used to defer value change
     * notifications.
     *
     * @param delay   the delay, in milliseconds, that is used to defer 
     *     value change notifications
     * @see #getDelay
     */
    public void setDelay(int delay) {
        timer.setInitialDelay(delay);
        timer.setDelay(delay);
    }
    
    
    /**
     * Returns if this model coalesces all pending changes or not.
     * 
     * @return <code>true</code> if all pending changes will be coalesced,
     *     <code>false</code> if pending changes are fired with a delay
     *     when an update has been received.
     *     
     * @see #setCoalesce(boolean)
     */
    public boolean isCoalesce() {
        return coalesce;
    }
    
    /**
     * Sets if this model shall coalesce all pending changes or not.
     * In this case, a change event will be fired first,
     * if no updates have been received for this model's delay.
     * If coalesce is <code>false</code>, a change event will be fired
     * with this model's delay when an update has been received.<p>
     * 
     * The default value is <code>false</code>.<p>
     * 
     * Note that this value is not the #coalesce value
     * of this model's internal Swing timer.
     * 
     * @param b <code>true</code> to coalesce, 
     *     <code>false</code> to fire separate changes
     */
    public void setCoalesce(boolean b) {
        coalesce = b;
    }


    // Misc *******************************************************************
    
    /**
     * Sets the given new value after this model's delay.
     * Does nothing if the new value and the latest pending value are the same.
     * 
     * @param evt  the PropertyChangeEvent to be fired after this model's delay
     */
    private void fireDelayedValueChange(PropertyChangeEvent evt) {
        pendingEvt = evt;
        if (coalesce) {
            timer.restart();
        } else {
            timer.start();
        }
    }
    
    
    private boolean hasPendingChange() {
        return timer.isRunning();
    }
    
    
    // Event Handling *****************************************************
    
    /**
     * Describes the delayed action to be performed by the timer.
     */
    private class ValueUpdateListener implements ActionListener {
        
        /**  
         * An ActionEvent has been fired by the Timer after its delay.
         * Fires the pending PropertyChangeEvent, stops the timer, 
         * and updates this model's oldValue.
         */
        public void actionPerformed(ActionEvent e) {
            fireValueChange(pendingEvt.getOldValue(), pendingEvt.getNewValue(), true);
            timer.stop();
            oldValue = pendingEvt.getNewValue() != null
                ? pendingEvt.getNewValue()
                : subject.getValue();
        }
    }
    
    
    /** 
     * Forwards value changes in the subject to listeners of this model. 
     */
    private class SubjectValueChangeHandler implements PropertyChangeListener {
        
        public void propertyChange(PropertyChangeEvent evt) {
            fireDelayedValueChange(evt);
        }
    }
    
}
