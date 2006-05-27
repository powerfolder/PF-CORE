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

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import com.jgoodies.animation.Animation;
import com.jgoodies.animation.AnimationUtils;
import com.jgoodies.animation.Animations;
import com.jgoodies.animation.components.BasicTextLabel;


/**
 * Provides a text animation that shows an overlapping sequence of 
 * texts using a bunch of different effects: color fade, scaling, glyph spacing.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see BasicTextAnimation
 */
public final class BasicTextAnimations {
	
	private static final int FADE_TYPE	= 0;
	private static final int SCALE_TYPE	= 1;
	private static final int SPACE_TYPE	= 2;
	
	
	private BasicTextAnimations() {
        // Override default constructor; prevents instantiation.
    }
	

	/**
	 * Creates and answers the default color fade text sequence.
     * 
     * @param label1           a text label used to blend over
     * @param label2           a second text label used to blend over
     * @param singleDuration   the duration of a single animation
     * @param beginOffset      an initial animation time offset
     * @param separatedTexts   a sequence of texts in a string separated
     * by the | character
     * @param baseColor        the base color for the fade
     * @return a default fade animation
	 */
	public static Animation defaultFade(
		BasicTextLabel label1,
		BasicTextLabel label2,
		long singleDuration,
		long beginOffset,
		String separatedTexts,
		Color baseColor) {
			
		return createTextSequence(label1, label2, 
								   singleDuration, beginOffset, 
								   separatedTexts, baseColor, 
								   FADE_TYPE);
	}
	

	/**
	 * Creates and answers the default scaling text sequence.
     * 
     * @param label1           a text label used to blend over
     * @param label2           a second text label used to blend over
     * @param singleDuration   the duration of a single animation
     * @param beginOffset      an initial animation time offset
     * @param separatedTexts   a sequence of texts in a string separated
     * by the | character
     * @param baseColor        the base color for the fade
     * @return a default scaling blend over animation
	 */
	public static Animation defaultScale(
		BasicTextLabel label1,
		BasicTextLabel label2,
		long singleDuration,
		long beginOffset,
		String separatedTexts,
		Color baseColor) {
			
		return createTextSequence(label1, label2, 
								   singleDuration, beginOffset, 
								   separatedTexts, baseColor, 
								   SCALE_TYPE);
	}


	/**
	 * Creates and answers the default glyph spacing text sequence.
     * 
     * @param label1           a text label used to blend over
     * @param label2           a second text label used to blend over
     * @param singleDuration   the duration of a single animation
     * @param beginOffset      an initial animation time offset
     * @param separatedTexts   a sequence of texts in a string separated
     * by the | character
     * @param baseColor        the base color for the fade
     * @return a default space blend over animation
	 */
	public static Animation defaultSpace(
		BasicTextLabel label1,
		BasicTextLabel label2,
		long singleDuration,
		long beginOffset,
		String separatedTexts,
		Color baseColor) {
			
		return createTextSequence(label1, label2, 
								   singleDuration, beginOffset, 
								   separatedTexts, baseColor, 
								   SPACE_TYPE);
	}
	

	// Private Helper Code ****************************************************

	/**
	 * Creates and returns the default glyph spacing text sequence.
     * 
     * @param label1    the first label to render the sequence
     * @param label2    the second label to render
     * @param singleDuration  the duration of a step in the sequence
     * @param beginOffset     an offset in ms between to steps
     * @param separatedTexts  a comma separated lists of texts to display
     * @param baseColor       the color used as a basis for the text
     * @param type            the type of the effect used to change
     * @return a composed animation that displays a sequence of texts
	 */
	private static Animation createTextSequence(
		BasicTextLabel label1,
		BasicTextLabel label2,
		long singleDuration,
		long beginOffset,
		String separatedTexts,
		Color baseColor,
		int type) {
			
		Animation animation;
		String[] texts  = AnimationUtils.splitTexts(separatedTexts);
		List animations = new LinkedList();
		long beginTime	= 0;
		
		BasicTextLabel label = label1;
		for (int i = 0; i < texts.length; i++) {
			label = i % 2 == 0 ? label1 : label2;
			animation = animation(label, singleDuration, texts[i], baseColor, type);
			animations.add(Animations.offset(beginTime, animation));
			beginTime += singleDuration + beginOffset;
		}

		return Animations.parallel(animations);
	}
	
	
	private static Animation animation(BasicTextLabel label, 
								 long duration, 
								 String text, 
								 Color baseColor, 
								 int type) {
		switch (type) {
			case FADE_TYPE :
				return BasicTextAnimation.defaultFade(label, duration, text, baseColor);

			case SCALE_TYPE :
				return BasicTextAnimation.defaultScale(label, duration, text, baseColor);

			case SPACE_TYPE :
				return BasicTextAnimation.defaultSpace(label, duration, text, baseColor);

			default :
				return null;
		}
	}

	
}