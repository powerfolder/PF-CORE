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

import com.jgoodies.animation.AbstractAnimation;
import com.jgoodies.animation.AnimationFunction;
import com.jgoodies.animation.AnimationFunctions;
import com.jgoodies.animation.components.BasicTextLabel;

/**
 * A basic text animation that fades in a text, changes the x and y scaling,
 * the position, and the space between glyphs.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see com.jgoodies.animation.Animation
 * @see BasicTextLabel
 * @see AnimationFunction
 * @see com.jgoodies.animation.AnimationFunctions
 */
public final class BasicTextAnimation extends AbstractAnimation {

    private final BasicTextLabel label;
    private final String         text;
    private final AnimationFunction                colorFunction;
    private final AnimationFunctions.FloatFunction offsetFunction;
    private final AnimationFunctions.FloatFunction scaleXFunction;
    private final AnimationFunctions.FloatFunction scaleYFunction;
    private final AnimationFunctions.FloatFunction spaceFunction;

    private boolean offsetEnabled = false;
    
    
    // Instance Creation *****************************************************

    /**
     * Constructs a text animation, that fades in a text, scales it and 
     * fades it out. Uses the given color and scaling functions.
     * 
     * @param label           the animation target component
     * @param duration        the animation duration 
     * @param text            the text to fade in
     * @param colorFunction   the animation function for the color
     * @param scaleXFunction  the animation function for the horizontal scale
     * @param scaleYFunction  the animation function for the vertical scale
     * @param spaceFunction   the animation function for the glyph space
     */
    public BasicTextAnimation(
        BasicTextLabel label,
        long duration,
        String text,
        AnimationFunction colorFunction,
        AnimationFunction scaleXFunction,
        AnimationFunction scaleYFunction,
        AnimationFunction spaceFunction) {

        super(duration);
        this.label = label;
        this.text = text;
        this.colorFunction =
            colorFunction != null
                ? colorFunction
                : defaultFadeColorFunction(duration, Color.darkGray);
        this.scaleXFunction =
            AnimationFunctions.asFloat(
                scaleXFunction != null
                    ? scaleXFunction
                    : AnimationFunctions.ONE);
        this.scaleYFunction =
            AnimationFunctions.asFloat(
                scaleYFunction != null
                    ? scaleYFunction
                    : AnimationFunctions.ONE);
        this.spaceFunction =
            AnimationFunctions.asFloat(
                spaceFunction != null
                    ? spaceFunction
                    : AnimationFunctions.ZERO);
        this.offsetFunction =
            AnimationFunctions.asFloat(defaultOffsetFunction());
    }

    /**
     * Creates and answers an animation, that provides a text fade-in and -out.
     * 
     * @param label           the animation target component
     * @param duration        the animation duration 
     * @param text            the text to fade in
     * @param baseColor       the base color for the fade effect
     * @return an animation with a default text fade
     */
    public static BasicTextAnimation defaultFade(
        BasicTextLabel label,
        long duration,
        String text,
        Color baseColor) {

        return new BasicTextAnimation(
            label,
            duration,
            text,
            cinemaFadeColorFunction(duration, baseColor),
            //defaultFadeColorFunction(duration, baseColor),
            null,
            null,
            null);
    }

    /**
     * Creates and answers an animation, that provides a text fade-in 
     * and -out and scales the text while fading out.
     * 
     * @param label           the animation target component
     * @param duration        the animation duration 
     * @param text            the text to fade in
     * @param baseColor       the base color for the fade effect
     * @return an animation with a default scaling text effect
     */
    public static BasicTextAnimation defaultScale(
        BasicTextLabel label,
        long duration,
        String text,
        Color baseColor) {

        return new BasicTextAnimation(
            label,
            duration,
            text,
            defaultScaleColorFunction(duration, baseColor),
            defaultScaleFunction(duration),
            defaultScaleFunction(duration),
            null);
    }

    /**
     * Creates and answers an animation, that provides a text fade-in 
     * and -out and increases the glyph spacing. 
     * 
     * @param label           the animation target component
     * @param duration        the animation duration 
     * @param text            the text to fade in
     * @param baseColor       the base color for the fade effect
     * @return an animation with a default glyph spacing effect
     */
    public static BasicTextAnimation defaultSpace(
        BasicTextLabel label,
        long duration,
        String text,
        Color baseColor) {

        return new BasicTextAnimation(
            label,
            duration,
            text,
            defaultSpaceColorFunction(duration, baseColor),
            null,
            null,
            defaultSpaceFunction(duration));
    }

    /**
     * Creates and answers the color animation function for the default fade.
     * 
     * @param duration        the animation duration 
     * @param baseColor       the base color for the fade effect
     * @return a Color-valued animation function for the default fade
     */
    public static AnimationFunction defaultFadeColorFunction(
        long duration,
        Color baseColor) {
        return AnimationFunctions.alphaColor(
            AnimationFunctions.linear(
                duration,
                new Integer[] {
                    new Integer(0),
                    new Integer(255),
                    new Integer(255),
                    new Integer(0)},
                new float[] { 0.0f, 0.3f, 0.7f, 1.0f }),
            baseColor);
    }

    /**
     * Creates and answers the color animation function for the default fade.
     * 
     * @param duration        the animation duration 
     * @param baseColor       the base color for the fade effect
     * @return a Color-valued animation function for the default fade
     */
    public static AnimationFunction cinemaFadeColorFunction(
        long duration,
        Color baseColor) {
        return AnimationFunctions.alphaColor(
            AnimationFunctions.linear(
                duration,
                new Integer[] {
                    new Integer(0),
                    new Integer(255),
                    new Integer(255),
                    new Integer(0)},
                new float[] { 0.0f, 0.3f, 0.85f, 1.0f }),
            baseColor);
    }

    /**
     * Creates and answers the animation function for the default scaling.
     * 
     * @param duration        the animation duration 
     * @param baseColor       the base color for the fade effect
     * @return a Color-valued animation function for the default scaling
     */
    public static AnimationFunction defaultScaleColorFunction(
        long duration,
        Color baseColor) {
        return AnimationFunctions.alphaColor(
            AnimationFunctions.linear(
                duration,
                new Integer[] {
                    new Integer(0),
                    new Integer(255),
                    new Integer(255),
                    new Integer(0)},
                new float[] { 0.0f, 0.2f, 0.85f, 1.0f }),
            baseColor);
    }

    /**
     * Creates and answers the color animation function for 
     * the default spacing animation.
     * 
     * @param duration        the animation duration 
     * @param baseColor       the base color for the fade effect
     * @return a Color-valued animation function for the default spacing
     */
    public static AnimationFunction defaultSpaceColorFunction(
        long duration,
        Color baseColor) {
        return AnimationFunctions.alphaColor(
            AnimationFunctions.linear(
                duration,
                new Integer[] {
                    new Integer(0),
                    new Integer(255),
                    new Integer(255),
                    new Integer(0)},
                new float[] { 0.0f, 0.2f, 0.8f, 1.0f }),
            baseColor);
    }

    /** 
     * Returns the animation function for the default random position offset.
     * 
     * @return an animation function for a default random offset
     */
    public static AnimationFunction defaultOffsetFunction() {
        return AnimationFunctions.random(-2, 2, 0.5f);
    }

    /** 
     * Creates and answers the default scaling animation function.
     * 
     * @param duration    the animation duration
     * @return an animation function for the default scaling effect
     */
    public static AnimationFunction defaultScaleFunction(long duration) {
        return AnimationFunctions.linear(
            duration,
            new Float[] { new Float(1.0f), new Float(1.0f), new Float(1.8f)},
            new float[] { 0.0f, 0.85f, 1.0f });
    }

    /** 
     * Creates and answers the default spacing animation function.
     * 
     * @param duration    the animation duration
     * @return an animation function for the default spacing effect
     */
    public static AnimationFunction defaultSpaceFunction(long duration) {
        return AnimationFunctions.fromTo(duration, 0, 10);
    }
    
    
    // ************************************************************************

    /**
     * Applies the effect: sets color, spacing, scaling and offset,
     * the latter only if enabled.
     * 
     * @param time    the render time
     */
    protected void applyEffect(long time) {
        label.setText(time == 0 ? " " : text);
        label.setColor((Color) colorFunction.valueAt(time));
        label.setScaleX(scaleXFunction.valueAt(time));
        label.setScaleY(scaleYFunction.valueAt(time));
        label.setSpace(spaceFunction.valueAt(time));
        if (isOffsetEnabled()) {
            label.setOffsetX(offsetFunction.valueAt(time));
            label.setOffsetY(offsetFunction.valueAt(time));
        }
    }

    /**
     * Answers whether the random position offset is enabled.
     * 
     * @return true indicates offset enabled, false disabled 
     */
    public boolean isOffsetEnabled() {
        return offsetEnabled;
    }
    
    /**
     * Enables or disables the random position offset
     * 
     * @param b  the new enablement
     */
    public void setOffsetEnabled(boolean b) {
        offsetEnabled = b;
    }

}