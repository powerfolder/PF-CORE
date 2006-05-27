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

/**
 * This interface describes time based animations. Basically, 
 * the <code>#animate</code> method applies an animation effect 
 * to an animation target over the animation's duration.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.3 $
 * 
 * @see AbstractAnimation
 * @see Animations
 */
public interface Animation {

    /**
     * Returns this animation's duration.
     * 
     * @return this animation duration
     */
    long duration();
    

    /**
     * Animates for the given time, that is, applies the animation effect 
     * to the animation target and fires events when the animation is 
     * started, stopped or repeated.
     * 
     * @param time  the time used to determine the animation effect
     */
    void animate(long time);
    

    /**
     * Adds the given <code>AnimationListener</code> to this animation.
     * 
     * @param listener  the <code>AnimationListener</code> to add
     */
    void addAnimationListener(AnimationListener listener);

    
    /**
     * Removes the given <code>AnimationListener</code> from this animation.
     * 
     * @param listener  the <code>AnimationListener</code> to remove
     */
    void removeAnimationListener(AnimationListener listener);

}