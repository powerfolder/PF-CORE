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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

/**
 * Starts and stops an animation and triggers
 * the animation at a given frame rate.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.3 $
 */
public final class Animator implements ActionListener {

    private final Animation animation;
    private final Timer     timer;
    private final int       framesPerSecond;

    private long startTime;
    private long elapsedTime  = 0;

    
    // Instance Creation *****************************************************
    
    /**
     * Constructs an <code>Animator</code> for the given animation and 
     * frame rate.
     * 
     * @param animation         the animation to animate
     * @param framesPerSecond   the desired frame rate
     * @throws NullPointerException if the animation is <code>null</code>
     * @throws IllegalArgumentException if the frame rate is non-positive
     */
    public Animator(Animation animation, int framesPerSecond) {
        if (animation == null)
            throw new NullPointerException("The animation must not be null.");

        if (framesPerSecond <= 0)
            throw new IllegalArgumentException("The frame rate must be positive.");

        this.animation = animation;
        this.framesPerSecond = framesPerSecond;
        this.timer = createTimer(framesPerSecond);
    }
    
    
    // ************************************************************************

    /**
     * Returns the animator's animation.
     * 
     * @return the animator's animation 
     */
    public Animation animation() {
        return animation;
    }
    

    /**
     * Returns the desired frame rate.
     * 
     * @return the desired frame rate per second
     */
    public int framesPerSecond() {
        return framesPerSecond;
    }
    

    /**
     * Returns the elapsed time since animation start.
     * 
     * @return time elapsed since the animation start
     */
    public long elapsedTime() {
        return timer.isRunning()
            ? System.currentTimeMillis() - startTime + elapsedTime
            : elapsedTime;
    }
    

    /**
     * Starts the animator and in turn the animation.
     */
    public void start() {
        if (!timer.isRunning()) {
            registerStopListener();
            startTime = System.currentTimeMillis();
            timer.start();
        } /*else {
               	System.out.println("Animator is already running");
        		} */
    }
    

    /**
     * Stops the animator.
     */
    public void stop() {
        if (timer.isRunning()) {
            elapsedTime = elapsedTime();
            timer.stop();
        }
    }


    /**
     * Implements the ActionListener interface for use by 
     * the <code>Timer</code>.
     * 
     * @param e  the action event
     */
    public void actionPerformed(ActionEvent e) {
        animation.animate(elapsedTime());
    }
    

    /**
     * Returns a string representation for the animator.
     * 
     * @return a string representation for the animator
     */
    public String toString() {
        return "elapsedTime=" + elapsedTime() + "; fps=" + framesPerSecond;
    }
    
    
    // Helper Code ************************************************************
    
    /**
     * Creates and configures a <code>Timer</code> object.
     * 
     * @param fps    the frames per second
     * @return a <code>Timer</code> with the specified frame rate
     */
    private Timer createTimer(int fps) {
        int delay = 1000 / fps;

        Timer aTimer = new Timer(delay, this);
        aTimer.setInitialDelay(0);
        aTimer.setCoalesce(true);
        return aTimer;
    }

    
    /**
     * Registers a listener that stops the animator if the animation stopped.
     */
    private void registerStopListener() {
        animation.addAnimationListener(new AnimationAdapter() {
            public void animationStopped(AnimationEvent e) {
                //System.out.println("All animations stopped.");
                stop();
                //animation.animate(animation.duration());
            }
        });
    }
    
    
}