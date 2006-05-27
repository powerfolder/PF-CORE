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
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Random;

import com.jgoodies.animation.AnimationRenderer;


/**
 * Paints two colored and often translucent fans that can be rotated.
 *
 * @author Karsten Lentzsch
 * @version $Revision: 1.4 $
 * 
 * @see com.jgoodies.animation.animations.FanAnimation
 * @see com.jgoodies.animation.components.FanComponent
 */
public final class FanRenderer implements AnimationRenderer {
	
	private static final Random random = new Random(System.currentTimeMillis());
	
	
	private final Triangle[]	triangles;
	
	private Point2D origin;
	private double rotation; 

	
	public FanRenderer(Triangle[] triangles) {
		this.triangles   = triangles;
	}


	public FanRenderer(int triangleCount, Color baseColor) {
		this(createSectors(triangleCount, baseColor));
	}
	
	
	public static Triangle[] createSectors(int count, Color baseColor) {
		Triangle[] result = new Triangle[count];
		double sectorAngle = Math.PI * 2 / count;

		for (int i = 0; i < count; i++) {
			double rotation = i * sectorAngle + (random.nextFloat() - 0.5) * Math.PI/10;
			double angle    = sectorAngle * (0.2 + random.nextFloat() * 0.4);
			result[i] = new Triangle(rotation, angle, nextColor(baseColor));
		}
		return result;
	}
	
	private static Color nextColor(Color baseColor) {
		float[] hsb = new float[3];
		Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), hsb);
		float brightness = 0.8f + random.nextFloat() * 0.2f;
		return Color.getHSBColor(hsb[0], hsb[1], brightness);
	}
	
	
	public Point2D getOrigin()						{ return origin;			}
	public void   setOrigin(Point2D origin)		{ this.origin = origin;	}
	public double getRotation() 					{ return rotation; 		}
	public void   setRotation(double rotation)	{ this.rotation = rotation; }
	
	
	public void render(Graphics2D g2, int width, int height) {
		double radius = Math.sqrt(width * width + height * height);
		
		Point2D p = getOrigin() != null ? getOrigin() : getDefaultOrigin(width, height);
		
		g2.translate(p.getX(), p.getY());
		g2.rotate(rotation);
		for (int i = 0; i < triangles.length; i++) {
			triangles[i].render(g2, radius);
		}
		g2.rotate(-rotation);
		g2.translate(-p.getX(), -p.getY());
	}
	
	
	private Point2D getDefaultOrigin(int width, int height) {
		return new Point2D.Double(width * 0.75, height * 0.75);
	}
	

	// Helper Class ***********************************************************
	
		
	// A helper class that models and renders a single sector.
	private static class Triangle {
		
		private final double aRotation;
		private final double angle;
		private final Color   color;
		
		private Triangle(double rotation, double angle, Color color) {
			this.aRotation = rotation;
			this.angle    = angle;
			this.color    = color;
		}
		
		private static Shape createPolygon(double rotation, double angle, double radius) {
			double startAngle   = rotation - angle / 2;
			double stopAngle    = startAngle + angle;
			double hypothenusis = radius / Math.cos(angle / 2);
		
			float x0 = 0.0f;
			float y0 = 0.0f;
			float x1 = (float) (x0 - hypothenusis * Math.cos(startAngle));
			float y1 = (float) (y0 - hypothenusis * Math.sin(startAngle));
			float x2 = (float) (x0 - hypothenusis * Math.cos(stopAngle));
			float y2 = (float) (y0 - hypothenusis * Math.sin(stopAngle));
			
			GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
			polygon.moveTo(x0, y0);
			polygon.lineTo(x1, y1);
			polygon.lineTo(x2, y2);
			polygon.closePath();
			
			return polygon;
		}
		
		void render(Graphics2D g2, double radius) {
			g2.setColor(color);
			g2.fill(createPolygon(aRotation, angle, radius));
		}
	}
	
}