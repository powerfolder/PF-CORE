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

import com.jgoodies.animation.renderer.BasicTextRenderer;
import com.jgoodies.animation.renderer.HeightMode;

/**
 * A Swing text component that can change the text, x and y scaling, 
 * glyph space, x and y offset and alignment mode.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 */
public final class BasicTextLabel extends JComponent {

    private final BasicTextRenderer renderer;

    /**
     * Constructs a animation text Swing label for the given text.
     * 
     * @param text     the initial text to be displayed
     */
    public BasicTextLabel(String text) {
        renderer = new BasicTextRenderer(text);
    }

    public Color getColor() {
        return renderer.getColor();
    }
    
    public HeightMode getHeightMode() {
        return renderer.getHeightMode();
    }
    
    public float getScaleX() {
        return renderer.getScaleX();
    }
    
    public float getScaleY() {
        return renderer.getScaleY();
    }
    
    public float getSpace() {
        return renderer.getSpace();
    }
    
    public float getOffsetX() {
        return renderer.getOffsetX();
    }
    
    public float getOffsetY() {
        return renderer.getOffsetY();
    }
    
    public String getText() {
        return renderer.getText();
    }

    public void setColor(Color color) {
        renderer.setColor(color);
        repaint();
    }

    public void setHeightMode(HeightMode heightMode) {
        renderer.setHeightMode(heightMode);
    }

    public void setScale(float scale) {
        renderer.setScaleX(scale);
        renderer.setScaleY(scale);
        repaint();
    }

    public void setScaleX(float scaleX) {
        renderer.setScaleX(scaleX);
        repaint();
    }

    public void setScaleY(float scaleY) {
        renderer.setScaleY(scaleY);
        repaint();
    }

    public void setSpace(float space) {
        renderer.setSpace(space);
        repaint();
    }

    public void setOffsetX(float offsetX) {
        renderer.setOffsetX(offsetX);
        repaint();
    }

    public void setOffsetY(float offsetY) {
        renderer.setOffsetY(offsetY);
        repaint();
    }

    public void setText(String newText) {
        renderer.setText(newText);
        repaint();
    }

    /**
     * Paints the component. Enabled anti-aliasing and sets high quality hints,
     * then renderers the component via the underlying renderer.
     * 
     * @param g    the Graphics object to render on
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