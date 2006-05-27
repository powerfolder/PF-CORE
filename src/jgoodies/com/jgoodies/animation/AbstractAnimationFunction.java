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
 * An abstract class that minimizes the effort required to implement
 * the {@link AnimationFunction} interface.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.2 $
 * 
 * @see AnimationFunctions
 */

public abstract class AbstractAnimationFunction
    implements AnimationFunction {
    
    /**
     * Describes this animation function's duration.
     */
    private final long duration;
    
    
    // Instance Creation ******************************************************
    
    /**
     * Constructs an <code>AbstractAnimationFunction</code> using the given
     * duration.
     * 
     * @param duration    the function's duration
     * @throws IllegalArgumentException  if the duration is negative
     */
    protected AbstractAnimationFunction(long duration) {
        if (duration < 0)
            throw new IllegalArgumentException("The duration must not be negative.");

        this.duration = duration;
    }

    
    // ************************************************************************
    
    /**
     * Checks whether the given time is in the valid time range, that is
     * a non-negative time that is smaller than this function's duration.
     * If not, an <code>IllegalArgumentException</code> is thrown.
     * 
     * @param time   the time to be checked
     * @throws IllegalArgumentException  if the time is outside the valid time 
     *     range
     */
    protected void checkTimeRange(long time) {
        if ((time < 0) || (time >= duration()))
            throw new IllegalArgumentException(
                "The time must be larger than 0 and smaller than "
                    + duration()
                    + ".");
    }

    
    /**
     * Returns this animation function's duration.
     * 
     * @return this animation function's duration
     */
    public final long duration() {
        return duration;
    }

}