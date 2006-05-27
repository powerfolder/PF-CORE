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
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

import javax.swing.JComponent;

import com.jgoodies.animation.renderer.FanRenderer;

/**
 * A Swing component that paints a set of triangles as a fan.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see com.jgoodies.animation.animations.FanAnimation
 * @see com.jgoodies.animation.renderer.FanRenderer
 */
public final class FanComponent extends JComponent {

    private final FanRenderer renderer;
    
    // Instance Creation ****************************************************

    /**
     * Constructs a <code>FanComponent</code> for the specified 
     * number of triangles and base color.
     * 
     * @param triangleCount   the number of triangles to be displayed
     * @param baseColor       the base color used to build the translucent
     *     triangle colors from
     */
    public FanComponent(int triangleCount, Color baseColor) {
        this.renderer = new FanRenderer(triangleCount, baseColor);
    }

    // Accessors ************************************************************

    public Point2D getOrigin() {
        return renderer.getOrigin();
    }
    
    public double getRotation() {
        return renderer.getRotation();
    }

    /**
     * Sets a new origin of the fan.
     * 
     * @param origin    the origin to be set
     */
    public void setOrigin(Point2D origin) {
        renderer.setOrigin(origin);
        repaint();
    }

    /**
     * Sets a new rotation.
     * 
     * @param rotation    the rotation to be set
     */
    public void setRotation(double rotation) {
        renderer.setRotation(rotation);
        repaint();
    }

    /**
     * Delegates painting to the fan renderer. Switches on anti-aliasing
     * and the high quality mode before invoking the renderer.
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

        Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;
        int w = getWidth()  - x - insets.right;
        int h = getHeight() - y - insets.bottom;
        g2.translate(x, y);
        renderer.render(g2, w, h);
        g2.translate(-x, -y);
    }

}