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

package com.jgoodies.binding.beans;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Timer;


/**
 * A PropertyChangeListener that is intended to handle property changes
 * after a specified delay. Useful to defer changes until a stable state
 * is reached. For example if you look up a persistent object for a selection
 * in a list. You don't want to find and transport objects that the user
 * selects temporarily; you want to get only a stable selection.<p>
 * 
 * If this handler is notified about a property change it stores 
 * the PropertyChangeEvent it has received in <code>#propertyChange</code>, 
 * and starts a Swing Timer will call <code>#delayedPropertyChange</code> after 
 * a delay. A previously started timer - if any - will be stopped before.<p>
 * 
 * TODO: Write about the recommended delay time - above the double-click time
 * and somewhere below a second, e.g. 100ms to 200ms.<p>
 * 
 * TODO: Write about a slightly different operation. The current implementation 
 * defers the delayed property change until no change has been handled for the 
 * specified delay; it's a DelayUntilStableForXXXmsPropertyChangeHandler.
 * Another feature is to delay for a specified time but ensure that some 
 * change notifications happen. The latter is a CoalescingPropertyChangeHandler.<p>
 * 
 * TODO: Summarize the differences between the DelayedReadValueModel, the
 * DelayedWriteValueModel, and this DelayedPropertyChangeHandler.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see com.jgoodies.binding.value.DelayedReadValueModel
 * @see com.jgoodies.binding.extras.DelayedWriteValueModel
 * @see javax.swing.Timer
 * 
 * @since 1.1
 */
public abstract class DelayedPropertyChangeHandler implements PropertyChangeListener {
    
    /**
     * The delay in milliseconds used as default in the no-arg constructor.
     */
    public static final int DEFAULT_DELAY = 200; // ms
    
    
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
     * Holds the most recent pending PropertyChangeEvent as stored
     * when this handler is notified about a property change, i. e.
     * <code>#propertyChange</code> is called. This event will be
     * used to call <code>#delayedPropertyChange</code> after a delay.
     */
    private PropertyChangeEvent pendingEvt;
    
    
    // Instance Creation ******************************************************
    
    /**
     * Constructs a DelayedPropertyChangeHandler with a default delay.
     */
    public DelayedPropertyChangeHandler() {
        this(DEFAULT_DELAY);
    }
    
    
    /**
     * Constructs a DelayedPropertyChangeHandler with the specified Timer delay 
     * and the coalesce disabled.
     * 
     * @param delay     the milliseconds to wait before the delayed property change
     *     will be performed
     *     
     * @throws IllegalArgumentException if the delay is negative
     */
    public DelayedPropertyChangeHandler(int delay) {
        this(delay, false);
    }
    
    
    /**
     * Constructs a DelayedPropertyChangeHandler with the specified Timer delay 
     * and the given coalesce mode.
     * 
     * @param delay     the milliseconds to wait before the delayed property change
     *     will be performed
     * @param coalesce  <code>true</code> to coalesce all pending changes,
     *     <code>false</code> to fire changes with the delay when an update
     *     has been received
     *     
     * @throws IllegalArgumentException if the delay is negative
     * 
     * @see #setCoalesce(boolean)
     */
    public DelayedPropertyChangeHandler(int delay, boolean coalesce) {
        this.coalesce = coalesce;
        this.timer = new Timer(delay, new DelayHandler());
        timer.setRepeats(false);
    }
    
    
    // Accessors **************************************************************
    
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
     * This handler has been notified about a change in a bound property.
     * Stops a running timer - if any -, stores the given event, then starts
     * a Swing timer that'll call <code>#delayedPropertyChange</code>
     * after this handler's delay.
     * 
     * @param evt  the PropertyChangeEvent describing the event source
     *     and the property that has changed
     */
    public final void propertyChange(PropertyChangeEvent evt) {
        pendingEvt = evt;
        if (coalesce) {
            timer.restart();
        } else {
            timer.start();
        }
    }
    
    
    /**
     * This method gets called after this handler's delay 
     * if a bound property has changed. The event is the 
     * pending event as stored in <code>#propertyChange</code>.<p>
     * 
     * This method is invoked only if this handler hasn't received 
     * subsequent property changes. In other words, it is called only
     * if the observed bound property is stable during the delay time. 
     * 
     * @param evt  the PropertyChangeEvent describing the event source
     *     and the property that has changed
     */
    public abstract void delayedPropertyChange(PropertyChangeEvent evt);
    
    
    // Event Handling *****************************************************
    
    /**
     * Describes the delayed action to be performed by the timer.
     */
    private class DelayHandler implements ActionListener {
        
        /**  
         * An ActionEvent has been fired by the Timer after its delay.
         * Invokes #delayedPropertyChange with the pending PropertyChangeEvent,
         * then stops the timer.
         */
        public void actionPerformed(ActionEvent e) {
            delayedPropertyChange(pendingEvt);
            timer.stop();
        }
    }
    
}
