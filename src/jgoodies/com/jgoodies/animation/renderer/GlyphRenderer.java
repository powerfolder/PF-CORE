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

package com.jgoodies.animation.renderer;

import java.awt.Color;
import java.awt.Graphics2D;

import com.jgoodies.animation.AnimationFunction;
import com.jgoodies.animation.AnimationFunctions;

/**
 * Renders the glyphs of a string with individual scaling, transform, and color.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 */
public final class GlyphRenderer extends AbstractTextRenderer {

    private final AnimationFunction colorFunction;
    private final AnimationFunctions.FloatFunction scaleFunction;
    private final long glyphDelay;

    private long time;
    
    
    // Instance Creation ******************************************************

    /**
     * Constructs a <code>GlyphRenderer</code> that paints 
     * individual glyphs with different transforms.
     * 
     * @param text               the initial text
     * @param scaleFunction      maps times to glyph scales
     * @param translateFunction  maps times to glyph translations
     * @param colorFunction      maps times to colors
     * @param glyphDelay         a time delay between the glyph animations
     */
    public GlyphRenderer(
        String text,
        AnimationFunction scaleFunction,
        AnimationFunction translateFunction,
        AnimationFunction colorFunction,
        long glyphDelay) {
        super(text);
        this.scaleFunction = AnimationFunctions.asFloat(scaleFunction);
        this.colorFunction = colorFunction;
        this.glyphDelay = glyphDelay;
        this.time = 0;
    }

    
    // Accessors **************************************************************
    
    public void setTime(long time) {
        this.time = time;
    }

    private long relativeTime(int glyphIndex) {
        return Math.max(0, time - glyphDelay * glyphIndex);
    }

    private float scaleAt(int glyphIndex) {
        return scaleFunction.valueAt(relativeTime(glyphIndex));
    }

    private Color colorAt(int glyphIndex) {
        return (Color) colorFunction.valueAt(relativeTime(glyphIndex));
    }

    /**
     * Renders the text. Firstly, ensures a valid cache,
     * then sets the color, and finally paints the cached glyph shaped,
     * using individual transforms.
     * 
     * @param g2     the graphics object to render on
     * @param width  the width of the graphics area
     * @param height the height of the graphics area
     */
    public void render(Graphics2D g2, int width, int height) {
        ensureValidCache(g2);

        int glyphCount = cachedGlyphShapes.length;
        float offsetX = (width - cachedTextWidth) / 2.0f;
        float offsetY = (height + cachedTextHeight) / 2.0f - getAdjustedDescent();

        g2.translate(offsetX, offsetY);
        for (int i = glyphCount - 1; i >= 0; i--) {
            float scale = scaleAt(i);
            //float translate = translateAt(i);

            g2.setColor(colorAt(i));
            //g2.translate(translate, 0);

            double glyphX = cachedGlyphVector.getGlyphPosition(i).getX();
            double glyphY =
                cachedGlyphVector.getGlyphVisualBounds(i).getBounds2D().getHeight();
            double adjustX = -glyphX * (scale - 1.0f);
            double adjustY = glyphY * (scale - 1.0f) / 2.0f;
            g2.translate(adjustX, adjustY);
            g2.scale(scale, scale);
            g2.fill(cachedGlyphShapes[i]);
            g2.scale(1.0f / scale, 1.0f / scale);
            g2.translate(-adjustX, -adjustY);
            //g2.translate(-translate, 0);
        }
        g2.translate(-offsetX, -offsetY);
    }
}