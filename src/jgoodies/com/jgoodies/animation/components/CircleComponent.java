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

import java.awt.*;

import javax.swing.JComponent;

/**
 * A Swing component that paints a circle with a given center, radius and color.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 */
public final class CircleComponent extends JComponent {

    private Point center;
    private int   radius;
    private Color color;

    /**
     * Constructs a <code>CircleComponent</code>.
     */
    public CircleComponent() {
        center = new Point(0, 0);
        radius = 30;
        color = Color.black;
    }

    public void setCenter(Point p) {
        this.center = p;
    }
    
    public void setRadius(int radius) {
        this.radius = radius;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Sets the bounds and center point.
     * 
     * @param x   the horizontal origin
     * @param y   the vertical origin
     * @param w   the width, the horizontal extent
     * @param h   the height, the vertical extent
     */
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        setCenter(new Point(x + w / 2, y + h / 2));
    }

    /**
     * Paints the component: enables anti-aliasing and sets high quality hints,
     * then renderers the component via the underlying renderer.
     * 
     * @param g    the Graphics object to render on
     */
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        int diameter = radius * 2;
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g2.setStroke(new BasicStroke(4));
        g2.drawOval(center.x - radius, center.y - radius, diameter, diameter);
    }

}