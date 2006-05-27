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
import com.jgoodies.animation.components.GlyphLabel;

/**
 * A text based animation that changes the scaling of the text's 
 * individual glyphs over the time.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 */
public final class GlyphAnimation extends AbstractAnimation {

    private final GlyphLabel label;
    private final String text;

    /**
     * Constructs an animation that changes the scaling of individual
     * glyphs over the duration.
     * 
     * @param label       the animation target label
     * @param duration    the animation duration
     * @param glyphDelay  a delay used to scale glyphs differently
     * @param text        the text to animation
     */
    public GlyphAnimation(
        GlyphLabel label,
        long duration,
        long glyphDelay,
        String text) {
        super(duration);
        this.label = label;
        this.text = text;
    }

    /**
     * Applies the effect: sets the text and time. 
     * 
     * @param time    the render time position
     */
    protected void applyEffect(long time) {
        label.setText(time == 0 ? " " : text);
        if (time < duration())
            label.setTime(time);
    }

}