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

package com.jgoodies.animation;

import java.awt.Color;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * This class consists only of static methods that construct and operate on 
 * {@link AnimationFunction}s.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.6 $
 * 
 * @see AnimationFunction
 */
public final class AnimationFunctions {
        
    /**
     * A constant {@link AnimationFunction} that returns 
     * <code>1</code> all the time.
     */
    public static final AnimationFunction ONE =
        constant(Integer.MAX_VALUE, new Float(1.0f));

    /**
     * A constant {@link AnimationFunction} that returns 
     * <code>0.0f</code> all the time.
     */
    public static final AnimationFunction ZERO =
        constant(Integer.MAX_VALUE, new Float(0.0f));


    private AnimationFunctions() {
        // Overrides the default constructor; prevents instantiation.
    }

    
    /**
     * Creates and returns an animation function that returns time-based 
     * sRGB colors that are built from a given base color and 
     * an animation function of alpha values.
     * <p>
     * Useful for fading effects.
     * 
     * @param f  the animation function of alpha values
     * @param baseColor  the base color
     * @return an animation function of colors
     */
    public static AnimationFunction alphaColor(
        AnimationFunction f,
        Color baseColor) {
        return new AlphaColorAnimationFunction(f, baseColor);
    }

    /**
     * Creates a time-based function that wraps the given Float-based animation
     * function to return the corresponding float values.
     * 
     * @param f  the underlying animation function of floats
     * @return an animation function the returns Float objects
     */
    public static FloatFunction asFloat(AnimationFunction f) {
        return new FloatFunction(f);
    }

    /**
     * Concatenates the fiven animation functions and returns a compound
     * animation function that represents the concatenation.
     * 
     * @param first     the concatenation's first AnimationFunction
     * @param second    the concatenation's second AnimationFunction
     * @return the concatenated animation function
     */
    public static AnimationFunction concat(
        AnimationFunction first,
        AnimationFunction second) {
        List sequence = new LinkedList();
        sequence.add(first);
        sequence.add(second);
        return new SequencedAnimationFunction(sequence);
    }

    /**
     * Creates and returns an animation function that returns a constant value
     * over the given duration.
     * 
     * @param duration  the function's duration
     * @param value     the Object that will be returned all the time
     * @return a constant animation function
     */
    public static AnimationFunction constant(long duration, Object value) {
        return discrete(duration, new Object[] { value });
    }

    /**
     * Creates and returns a discrete animation function for the 
     * given duration and values. The values are equally distributed
     * over the duration.
     * 
     * @param duration  the function's duration
     * @param values    an array of discrete result values
     * @return a discrete animation function
     */
    public static AnimationFunction discrete(
        long duration,
        Object[] values) {
        return discrete(duration, values, null);
    }

    /**
     * Creates and returns a discrete animation function for the given duration,
     * values and interpolation key times.
     * 
     * @param duration  the function's duration
     * @param values    an array of discrete result values
     * @param keyTimes  an array of key times used to distribute the 
     *   result values over the time
     * @return a discrete animation function
     */
    public static AnimationFunction discrete(
        long duration,
        Object[] values,
        float[] keyTimes) {
        return new InterpolatedAnimationFunction(
            duration,
            values,
            keyTimes,
            InterpolatedAnimationFunction.MODE_DISCRETE);
    }

    /**
     * Creates and returns a linear animation function for the given duration 
     * that returns Float in interval [from, from + by].
     * 
     * @param duration   the animation duration
     * @param from       the initial result value
     * @param by         the difference that is added to the initial value
     * @return a linear animation function with values in [from, from + by]
     */
    public static AnimationFunction fromBy(
        long duration,
        float from,
        float by) {
        return fromTo(duration, from, from + by);
    }

    /**
     * Ceates and returns a linear animation function with the given duration.
     * The function values are Floats in the intervall [from, to].
     * 
     * @param duration   the animation duration
     * @param from       the initial result value
     * @param to         the last result value
     * @return a linear animation function with values in [from, to]
     */
    public static AnimationFunction fromTo(
        long duration,
        float from,
        float to) {
        return linear(
            duration,
            new Float[] { new Float(from), new Float(to)});
    }

    /**
     * Creates and returns a linear animation function that is defined
     * by an array of numeric values; these are distributed equally 
     * over the duration.
     * 
     * @param duration   the animation duration
     * @param values     an array of values
     * @return a linear animation function for the given values
     */
    public static AnimationFunction linear(long duration, Object[] values) {
        return linear(duration, values, null);
    }

    /**
     * Creates and returns a linear animation function that is defined
     * by an array of numeric values and an array of relative key times.
     * 
     * @param duration   the animation duration
     * @param values     an array of values
     * @param keyTimes   an array of key times used to distribute the 
     * result values over the time
     * @return a linear animation function for the given values
     */
    public static AnimationFunction linear(
        long duration,
        Object[] values,
        float[] keyTimes) {
        return new InterpolatedAnimationFunction(
            duration,
            values,
            keyTimes,
            InterpolatedAnimationFunction.MODE_LINEAR);
    }
    

    /**
     * Creates an <code>AnimationFunction</code> that maps times
     * to instances of <code>Color</code>. The mapping is interpolated
     * from an array of Colors using an array of key times.
     * 
     * @param duration   the duration of this animation function
     * @param colors     the colors to interpolate.
     * @param keyTimes   an array of key times used to distribute 
     *                    the result values over the time.
     * @return An {@link AnimationFunction} that maps times to sRGB colors.
     *     This mapping is defined by an arry of <code>Color</code> values 
     *     and a corresponding array of key times that is used to interpolate
     *     sub-AnimationFunction for the red, green, blue and alpha values.
     */
    public static AnimationFunction linearColors(
                long duration, Color[] colors, float[] keyTimes) {
        return new ColorFunction(duration, colors, keyTimes);
    }
    
    
    /**
     * Creates and returns an animation function that returns random values
     * from the interval [min, max] with a given change probability.
     * 
     * @param min                the minimum result value
     * @param max                the maximum result value
     * @param changeProbability  the probability that the value changes
     * @return an animation function with random values in [min, max]
     */
    public static AnimationFunction random(
        int min,
        int max,
        float changeProbability) {
        return new RandomAnimationFunction(min, max, changeProbability);
    }

    /**
     * Creates and returns an animation function that is defined 
     * by repeating the specified animation function.
     * 
     * @param f            the animation function to repeat
     * @param repeatTime   the time to repeat the function
     * @return the repeated animation function
     */
    public static AnimationFunction repeat(
        AnimationFunction f,
        long repeatTime) {
        return new RepeatedAnimationFunction(f, repeatTime);
    }

    /**
     * Creates and returns an animation function that is defined 
     * by reverting the given animation function in time.
     * 
     * @param f     the animation function to reverse
     * @return the reversed animation function
     */
    public static AnimationFunction reverse(AnimationFunction f) {
        return new ReversedAnimationFunction(f);
    }
    
    
    // Helper Classes *********************************************************

    // Helper class for animation functions that answer translucent colors.
    private static class AlphaColorAnimationFunction
        implements AnimationFunction {
        private final Color baseColor;
        private final AnimationFunction fAlpha;

        private AlphaColorAnimationFunction(
            AnimationFunction fAlpha,
            Color baseColor) {
            this.fAlpha = fAlpha;
            this.baseColor = baseColor;
        }

        public long duration() {
            return fAlpha.duration();
        }

        // Constructs colors from the sRGB color space.
        public Object valueAt(long time) {
            return new Color(
                baseColor.getRed(),
                baseColor.getGreen(),
                baseColor.getBlue(),
                ((Float) fAlpha.valueAt(time)).intValue());
        }
    }

    // Helper class for interpolating colors.
    private static class ColorFunction extends AbstractAnimationFunction {

        /** 
         * Refers to an AnimationFunction of float values that maps
         * a time to the red component of an sRGB color value.
         */
        private final AnimationFunctions.FloatFunction redFunction;
        
        /** 
         * Refers to an AnimationFunction of float values that maps
         * a time to the green component of an sRGB color value.
         */
        private final AnimationFunctions.FloatFunction greenFunction;
        
        /** 
         * Refers to an AnimationFunction of float values that maps
         * a time to the blue component of an sRGB color value.
         */
        private final AnimationFunctions.FloatFunction blueFunction;
        
        /** 
         * Refers to an AnimationFunction of float values that maps
         * a time to the alpha value of an sRGB color value.
         */
        private final AnimationFunctions.FloatFunction alphaFunction;
        

        // Instance creation ******************************************************
        
        /**
         * Creates an <code>AnimationFunction</code> that maps times
         * to instances of <code>Color</code>. The mapping is interpolated
         * from an array of Colors using an array of key times.
         * 
         * @param duration   the duration of this animation function
         * @param colors     the colors to interpolate.
         * @param keyTimes   an array of key times used to distribute 
         *                    the result values over the time.
         */
        private ColorFunction(long duration, Color[] colors, float[] keyTimes) {
            super(duration);
            Float[] red   = new Float[colors.length];
            Float[] green = new Float[colors.length];
            Float[] blue  = new Float[colors.length];
            Float[] alpha = new Float[colors.length];

            for (int i = 0; i < colors.length; i++) {
                red[i]   = new Float(colors[i].getRed());
                green[i] = new Float(colors[i].getGreen());
                blue[i]  = new Float(colors[i].getBlue());
                alpha[i] = new Float(colors[i].getAlpha());
            }

            redFunction = AnimationFunctions.asFloat(AnimationFunctions.linear(
                            duration, red, keyTimes));
            greenFunction = AnimationFunctions.asFloat(AnimationFunctions.linear(
                            duration, green, keyTimes));
            blueFunction = AnimationFunctions.asFloat(AnimationFunctions.linear(
                            duration, blue, keyTimes));
            alphaFunction = AnimationFunctions.asFloat(AnimationFunctions.linear(
                            duration, alpha, keyTimes));
        }
        
        
        // AnimationFunction Implementation ***************************************

        /**
         * Returns the interpolated color for a given time in the valid 
         * time interval. This method is required to implement the
         * AnimationFunction interface and just forwards to the type-safe
         * counterpart <code>#colorValueAt</code> 
         * 
         * @param time   the time used to determine the interpolated color
         * @return the interpolated color for the given time
         * @see ColorFunction#colorValueAt(long)
         */
        public Object valueAt(long time) {
            return colorValueAt(time);
        }
        
        /**
         * Returns the interpolated color for a given time in the valid 
         * time interval.
         * 
         * @param time the time used to determine the interpolated color.
         * @return the interpolated color for the given time
         * @see ColorFunction#valueAt(long)
         */
        private Color colorValueAt(long time) {
            checkTimeRange(time);
            return new Color(
                    (int) redFunction.valueAt(time), 
                    (int) greenFunction.valueAt(time),
                    (int) blueFunction.valueAt(time), 
                    (int) alphaFunction.valueAt(time));
        }
        
    }
    
    /**
     * Helper class that wraps a Float-based animation function to answer floats.
     */
    public static class FloatFunction {
        private final AnimationFunction f;

        FloatFunction(AnimationFunction f) {
            this.f = f;
        }

        public long duration() {
            return f.duration();
        }
        public float valueAt(long time) {
            return ((Number) f.valueAt(time)).floatValue();
        }
    }

    // Helper class for interpolation based animation functions.
    private static class InterpolatedAnimationFunction
        extends AbstractAnimationFunction {
        static final int MODE_DISCRETE = 1;
        static final int MODE_LINEAR   = 2;
        private final float[] keyTimes;
        private final int mode;

        /* Left for long winter nights in northern Germany.
        static final int MODE_PACED      = 4; 
        static final int MODE_SPLINE     = 8;  
        */

        private final Object[] values;

        private InterpolatedAnimationFunction(
            long duration,
            Object[] values,
            float[] keyTimes,
            int mode) {
            super(duration);
            this.values = values;
            this.keyTimes = keyTimes;
            this.mode = mode;
            checkValidKeyTimes(values.length, keyTimes);
        }

        private void checkValidKeyTimes(int valuesLength, float[] theKeyTimes) {
            if (theKeyTimes == null)
                return;

            if (valuesLength < 2 || valuesLength != theKeyTimes.length)
                throw new IllegalArgumentException("The values and key times arrays must be non-empty and must have equal length.");

            for (int index = 0; index < theKeyTimes.length - 2; index++) {
                if (theKeyTimes[index] >= theKeyTimes[index + 1])
                    throw new IllegalArgumentException("The key times must be increasing.");
            }
        }

        private Object discreteValueAt(long time) {
            return values[indexAt(time, values.length)];
        }

        private int indexAt(long time, int intervalCount) {
            long duration = duration();
            // Gleichlange Zeitabschnitte
            if (keyTimes == null) {
                return (int) (time * intervalCount / duration);
            }
            for (int index = keyTimes.length - 1; index > 0; index--) {
                if (time > duration * keyTimes[index])
                    return index;
            }
            return 0;
        }

        /**
         * Currently we provide only linear interpolations that are based on floats.
         * 
         * @param value1   the first interpolation key point
         * @param value2   the second interpolation key point
         * @param time     the time to get an interpolated value for
         * @param duration the duration of the whole animation
         * @return the interpolated value at the given time
         */
        private Object interpolateLinear(
            Object value1,
            Object value2,
            long time,
            long duration) {
            float f1 = ((Number) value1).floatValue();
            float f2 = ((Number) value2).floatValue();
            float value = f1 + (f2 - f1) * time / duration;
            return new Float(value);
        }

        private Object linearValueAt(long time) {
            int segments = values.length - 1;
            int beginIndex = indexAt(time, segments);
            int endIndex = beginIndex + 1;
            long lastTime = duration() - 1;
            long beginTime =
                keyTimes == null
                    ? beginIndex * lastTime / segments
                    : (long) (keyTimes[beginIndex] * lastTime);
            long endTime =
                keyTimes == null
                    ? endIndex * lastTime / segments
                    : (long) (keyTimes[endIndex] * lastTime);

            return interpolateLinear(
                values[beginIndex],
                values[endIndex],
                time    - beginTime,
                endTime - beginTime);
        }

        public Object valueAt(long time) {
            checkTimeRange(time);
            switch (mode) {
                case MODE_DISCRETE :
                    return discreteValueAt(time);

                case MODE_LINEAR :
                    return linearValueAt(time);

                default :
                    throw new IllegalStateException("Unsupported interpolation mode.");
            }
        }
    }

    // Helper class for animation function that answer random values.
    private static class RandomAnimationFunction
        implements AnimationFunction {
        private final float changeProbability;
        private final int max;
        private final int min;
        private final Random random;

        private Object value;

        private RandomAnimationFunction(
            int min,
            int max,
            float changeProbability) {
            this.random = new Random();
            this.min = min;
            this.max = max;
            this.changeProbability = changeProbability;
        }

        public long duration() {
            return Integer.MAX_VALUE;
        }

        public Object valueAt(long time) {
            if ((value == null)
                || (random.nextFloat() < changeProbability)) {
                value = new Integer(min + random.nextInt(max - min));
            }
            return value;
        }
    }

    // Helper class used to repeat or sequence an animation function.
    private static class RepeatedAnimationFunction
        extends AbstractAnimationFunction {
        private final AnimationFunction f;
        private final long simpleDuration;

        private RepeatedAnimationFunction(
            AnimationFunction f,
            long repeatTime) {
            super(repeatTime);
            this.f = f;
            this.simpleDuration = f.duration();
        }

        public Object valueAt(long time) {
            return f.valueAt(time % simpleDuration);
        }
    }

    // Helper class for reversing an animation function.
    private static class ReversedAnimationFunction
        extends AbstractAnimationFunction {
        private final AnimationFunction f;

        private ReversedAnimationFunction(AnimationFunction f) {
            super(f.duration());
            this.f = f;
        }

        public Object valueAt(long time) {
            return f.valueAt(duration() - time);
        }
    }

    // Helper class to compose an animation functions from a sequences of such functions.
    private static class SequencedAnimationFunction
        implements AnimationFunction {
        private final List functions;

        private SequencedAnimationFunction(List functions) {
            this.functions = Collections.unmodifiableList(functions);
            if (this.functions.isEmpty())
                throw new IllegalArgumentException("The list of functions must not be empty.");
        }

        public long duration() {
            long cumulatedDuration = 0;
            for (Iterator i = functions.iterator(); i.hasNext();) {
                AnimationFunction f = (AnimationFunction) i.next();
                cumulatedDuration += f.duration();
            }
            return cumulatedDuration;
        }

        public Object valueAt(long time) {
            if (time < 0)
                throw new IllegalArgumentException("The time must be positive.");

            long begin = 0;
            long end;
            for (Iterator i = functions.iterator(); i.hasNext();) {
                AnimationFunction f = (AnimationFunction) i.next();
                end = begin + f.duration();
                if (time < end)
                    return f.valueAt(time - begin);
                begin = end;
            }
            throw new IllegalArgumentException("The time must be smaller than the total duration.");
        }
    }
    
}