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

package com.jgoodies.animation.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import com.jgoodies.animation.AnimationFunction;
import com.jgoodies.animation.AnimationFunctions;
import com.jgoodies.animation.renderer.GlyphRenderer;
import com.jgoodies.animation.renderer.HeightMode;

/**
 * A Swing component that can transform a text's individual glyphs.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 */
public final class GlyphLabel extends JComponent {

    private final GlyphRenderer renderer;

    
    // Instance Creation ******************************************************

    /**
     * Creates a <code>GlyphLabel</code> for the given text, duration and
     * delay between the individual glyphs.
     * 
     * @param text        the initial text
     * @param duration    the duration of the whole animation
     * @param glyphDelay  a delay between the animation of the individual glyphs
     */
    public GlyphLabel(String text, long duration, long glyphDelay) {
        this(text, duration, glyphDelay, Color.darkGray);
    }

    /**
     * Creates a <code>GlyphLabel</code> for the given text, duration, base color
     * and delay between the individual glyphs.
     * 
     * @param text        the initial text
     * @param duration    the duration of the whole animation
     * @param glyphDelay  a delay between the animation of the individual glyphs
     * @param baseColor   the color used as a basis for the translucent 
     *     glyph foreground colors
     */
    public GlyphLabel(
        String text,
        long duration,
        long glyphDelay,
        Color baseColor) {
        renderer =
            new GlyphRenderer(
                text,
                defaultScaleFunction(duration),
                AnimationFunctions.ZERO,
                defaultColorFunction(duration, baseColor),
                glyphDelay);
    }

    /** 
     * Creates and returns the default scale function for the given duration.
     * 
     * @param duration   the duration of the whole animation
     * @return an animation function that maps times to glyph scales
     */
    public static AnimationFunction defaultScaleFunction(long duration) {
        return AnimationFunctions.linear(
            duration,
            new Float[] {
                new Float(5.0f),
                new Float(0.8f),
                new Float(1.0f),
                new Float(1.0f)},
            new float[] { 0.0f, 0.1f, 0.12f, 1.0f });
    }

    /**
     * Creates and returns the default color function for the given duration
     * and base color.
     *
     * @param duration    the duration of the animation
     * @param baseColor   the color used as a basis for the translucent colors 
     * @return an animation function that maps times to translucent glyph colors
     */
    public static AnimationFunction defaultColorFunction(
        long duration,
        Color baseColor) {
        return AnimationFunctions.alphaColor(
            AnimationFunctions.linear(
                duration,
                new Integer[] {
                    new Integer(0),
                    new Integer(255),
                    new Integer(255)},
                new float[] { 0.0f, 0.15f, 1.0f }),
            baseColor);
    }
    
    
    // Accessors **************************************************************

    public HeightMode getHeightMode() {
        return renderer.getHeightMode();
    }
    
    public String getText() {
        return renderer.getText();
    }

    public void setHeightMode(HeightMode heightMode) {
        renderer.setHeightMode(heightMode);
    }

    public void setText(String newText) {
        renderer.setText(newText);
        repaint();
    }

    public void setTime(long time) {
        renderer.setTime(time);
        repaint();
    }
    
    
    // Rendering **************************************************************

    /**
     * Paints the component. Sets high-fidelity rendering hints, 
     * then invoke the renderer to render the glyphs.
     * 
     * @param g   the Graphics object to render on
     */
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY);

        renderer.setFont(getFont());
        renderer.render(g2, getWidth(), getHeight());
    }

}