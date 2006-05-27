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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

import com.jgoodies.animation.*;

/**
 * An abstract superclass that helps implementing typographic animation renderers.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 */
public abstract class AbstractTextRenderer implements AnimationRenderer {

    private String text;
    private Font font;
    private Color color;
    private HeightMode heightMode = HeightMode.CAPITAL_ASCENT;

    // Cached data -------------------------------------------
    protected GlyphVector cachedGlyphVector;
    protected Shape[]     cachedGlyphShapes;
    protected float      cachedTextWidth;
    protected float      cachedTextAscent;
    protected float      cachedTextHeight;
    protected float      capitalMAscent = -1f; // Ascent of a capital M
    private   boolean    cacheValid = false;

    
    // Instance Creation ******************************************************
    
    AbstractTextRenderer(String text) {
        this(text, null);
    }

    AbstractTextRenderer(String text, Font font) {
        this.text = text == null ? "Karsten Lentzsch" : text;
        this.font = font == null ? createDefaultFont() : font;
    }

    /**
     * Creates and returns a default font object.
     * 
     * @return a default font object
     */
    private static Font createDefaultFont() {
        return new Font("dialog", Font.BOLD, 12);
    }
    
    
    // Accessors **************************************************************

    public Color getColor() {
        return color;
    }
    
    public Font getFont() {
        return font;
    }
    
    public String getText() {
        return text;
    }
    
    public HeightMode getHeightMode() {
        return heightMode;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    public void setHeightMode(HeightMode heightMode) {
        this.heightMode = heightMode;
    }

    /**
     * Sets the renderer's font.
     * 
     * @param newFont   the font to be set
     */
    public void setFont(Font newFont) {
        if (newFont == null)
            throw new NullPointerException("The font must not be null.");

        if (newFont.equals(font))
            return;

        font = newFont;
        invalidateCache();
    }

    /**
     * Sets the renderer's text.
     * 
     * @param newText   the text to be set
     */
    public void setText(String newText) {
        if (newText == null)
            throw new NullPointerException("The text must not be null.");

        if (newText.equals(text))
            return;

        text = newText;
        invalidateCache();
    }

    /**
     * Computes and answers the text ascent using the current height mode.
     * 
     * @return the ascent adjusted using the current height mode
     * @see #getHeightMode()
     */
    protected float getAdjustedAscent() {
        if (heightMode == HeightMode.CAPITAL_ASCENT)
            return capitalMAscent;
        else if (heightMode == HeightMode.TEXT_ASCENT)
            return cachedTextAscent;
        else
            return cachedTextHeight;
    }

    /**
     * Computes and answers the text descent using the current height mode.
     * 
     * @return the descent adjusted to the current height mode
     * @see #getHeightMode()
     */
    protected float getAdjustedDescent() {
        if (heightMode == HeightMode.CAPITAL_ASCENT)
            return 0;
        else if (heightMode == HeightMode.TEXT_ASCENT)
            return 0;
        else
            return cachedTextHeight - cachedTextAscent;
    }

    // Caching ****************************************************************

    protected boolean isCacheValid() {
        return cacheValid;
    }

    protected void setCacheValid(boolean b) {
        cacheValid = b;
    }

    protected void ensureValidCache(Graphics2D g2) {
        if (!isCacheValid())
            validateCache(g2);
    }

    /**
     * Validates the cache, here: creates a <code>GlyphVector</code>
     * and computes and stores its size information.
     * 
     * @param g2   the Graphics object used to get the font render context
     */
    protected void validateCache(Graphics2D g2) {
        FontRenderContext frc = g2.getFontRenderContext();

        ensureCapitalMAscentComputed(frc);

        cachedGlyphVector = font.createGlyphVector(frc, text);
        Rectangle2D bounds = cachedGlyphVector.getVisualBounds();
        cachedTextWidth  = (float) bounds.getWidth();
        cachedTextAscent = (float) - bounds.getY();
        cachedTextHeight = (float) bounds.getHeight();

        int glyphCount = cachedGlyphVector.getNumGlyphs();
        cachedGlyphShapes = new Shape[glyphCount];
        for (int i = 0; i < glyphCount; i++) {
            cachedGlyphShapes[i] = cachedGlyphVector.getGlyphOutline(i);
        }
        setCacheValid(true);

        /* Debug lines
        System.out.println("Text = " + text);
        System.out.println("GV visual bounds = " + glyphVector.getVisualBounds());
        */
    }

    /**
     * Ensures that the ascent of a capital M has been computed.
     * 
     * @param frc   the font render context used to create the glyph vector
     */
    private void ensureCapitalMAscentComputed(FontRenderContext frc) {
        if (capitalMAscent > 0)
            return;
        GlyphVector mGlyphVector = font.createGlyphVector(frc, "M");
        capitalMAscent = (float) - mGlyphVector.getVisualBounds().getY();
    }

    /**
     * Invalidates the cache.
     */
    protected void invalidateCache() {
        setCacheValid(false);
        cachedGlyphVector = null;
        cachedGlyphShapes = null;
    }

}