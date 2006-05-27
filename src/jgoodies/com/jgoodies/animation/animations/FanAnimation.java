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

package com.jgoodies.animation.animations;

import com.jgoodies.animation.AbstractAnimation;
import com.jgoodies.animation.AnimationFunction;
import com.jgoodies.animation.AnimationFunctions;
import com.jgoodies.animation.components.FanComponent;


/**
 * An animation that rotates a fan that consists of a set 
 * of translucent sectors.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see com.jgoodies.animation.components.FanComponent
 * @see com.jgoodies.animation.renderer.FanRenderer
 */
public final class FanAnimation extends AbstractAnimation {
	
	public  static final double DEFAULT_CLOCKWISE_ROTATION     =  0.01; // rotations per second
	public  static final double DEFAULT_ANTICLOCKWISE_ROTATION = -0.02;
	
	
	private final FanComponent fan;
	private final AnimationFunctions.FloatFunction rotationFunction;
	
	
	/**
	 * Constructs an animation that rotates a fan using the given fan component,
	 * duration and rotation animation function.
     * 
     * @param fan                the fan component animation target
     * @param duration           the animation duration
     * @param rotationFunction   the rotation animation function
	 */
	public FanAnimation(FanComponent fan, long duration, AnimationFunction rotationFunction) {
		super(duration);	
		this.fan = fan;
		this.rotationFunction  = 
			AnimationFunctions.asFloat(rotationFunction != null 
			? rotationFunction
			: defaultRotationFunction(duration));
	}


	/**
	 * Creates and answers the default fan animation.
     * 
     * @param fan        the fan component animation target
     * @param duration   the animation duration
     * @return a default fan animation
	 */
	public static FanAnimation defaultFan(FanComponent fan, long duration) {
		return new FanAnimation(fan, duration, null);
	}


	/** 
	 * Creates and answers an animation function for the default rotation.
     * 
     * @param duration   the animation duration
     * @return an animation function for the default rotation
	 */
	public static AnimationFunction defaultRotationFunction(long duration) {
		return AnimationFunctions.fromTo(duration, 0f, (float) (Math.PI * 2.0f));
	}


	/**
	 * Applies the effect: sets the time-based rotation.
     * 
     * @param time    the render time
	 */
	protected void applyEffect(long time) {
		fan.setRotation(rotationFunction.valueAt(time));
	}
	
}