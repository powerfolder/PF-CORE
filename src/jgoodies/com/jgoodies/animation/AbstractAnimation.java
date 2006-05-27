/*
 * Copyright (c) 2001-2004 JGoodies Karsten Lentzsch. All Rights Reserved.
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

package com.jgoodies.animation;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * An abstract class that minimizes the effort required to implement 
 * the {@link Animation} interface. Defines the duration and freezed state
 * and provides a listener list.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 */
public abstract class AbstractAnimation implements Animation {

    private final long     duration;
    private final boolean  freezed;
    private final List     listenerList = new LinkedList();

    private boolean active = false;


    // Instance Creation *****************************************************
    
    /**
     * Constructs an <code>Animation</code> with the specified duration.
     * 
     * @param duration  the animation's duration
     */
    protected AbstractAnimation(long duration) {
        this(duration, false);
    }

    /**
     * Constructs an <code>Animation</code> with the specified duration 
     * and freezed mode.
     * 
     * @param duration  the animation's duration
     * @param freezed   true indicates that the effect will be retained after
     * the animation is finished, false resets the effect to the time 0
     */
    protected AbstractAnimation(long duration, boolean freezed) {
        this.duration = duration;
        this.freezed  = freezed;
    }

    
    // ***********************************************************************
    
    /**
     * Returns this animation's duration.
     * 
     * @return this animation's duration
     */
    public final long duration() {
        return duration;
    }

    /**
     * Answers whether the animation effect should be freezed after 
     * we exceeded the animation's duration. If this is not the case,
     * the animation effect of time 0 will be set.
     * 
     * @return true indicates that the effect will be retained if the 
     * animation duration exceeded; false indicates that the effect 
     * will be reset
     */
    public final boolean isFreezed() {
        return freezed;
    }

    /**
     * Applies the animation effect for the given time to the animation target.
     * 
     * @param time   the time used to determine the animation effect
     */
    abstract protected void applyEffect(long time);

    /**
     * Performs the animation at the given time: applies the animation
     * effect to the animation target, fires appropriate events, 
     * and resets the effect if we exceeded the animations duration.
     * 
     * @param time   the time used to determine the animation effect
     */
    public void animate(long time) {
        if (time >= duration) {
            if (active) {
                applyEffect(isFreezed() ? duration - 1 : 0);
                fireAnimationStopped(time);
                active = false;
            }
            return;
        }

        if (!active) {
            active = true;
            fireAnimationStarted(time);
        }
        applyEffect(time);
    }

    /**
     * Adds an <code>AnimationListener</code> to this animation.
     * 
     * @param listener  the <code>AnimationListener</code> to add
     */
    public final void addAnimationListener(AnimationListener listener) {
        listenerList.add(listener);
    }

    /**
     * Removes an <code>AnimationListener</code> to this animation.
     * 
     * @param listener  the <code>AnimationListener</code> to remove
     */
    public final void removeAnimationListener(AnimationListener listener) {
        listenerList.remove(listener);
    }

    /**
     * Fires an event that indicates that the animation has been started 
     * at the specified time.
     * 
     * @param time   the time that will be reported in the event
     */
    protected final void fireAnimationStarted(long time) {
        AnimationEvent e =
            new AnimationEvent(this, AnimationEvent.STARTED, time);
        for (Iterator i = listenerList.iterator(); i.hasNext();) {
            AnimationListener listener = (AnimationListener) i.next();
            listener.animationStarted(e);
        }
    }

    /**
     * Fires an event that indicates that the animation has been stopped
     * at the specified time.
     * 
     * @param time   the time that will be reported in the event
     */
    protected final void fireAnimationStopped(long time) {
        AnimationEvent e =
            new AnimationEvent(this, AnimationEvent.STOPPED, time);
        for (Iterator i = listenerList.iterator(); i.hasNext();) {
            AnimationListener listener = (AnimationListener) i.next();
            listener.animationStopped(e);
        }
    }

    /**
     * Returns a string representation for this animation.
     * 
     * @return a string representation for this animation
     */
    public String toString() {
        return "["
            + getClass().getName()
            + "; duration="
            + duration
            + "; active="
            + active+
            + ']';
    }
}