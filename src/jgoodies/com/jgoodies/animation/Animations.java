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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class consists only of static methods that either 
 * operate on animations or create useful standard animations.
 *
 * @author  Karsten Lentzsch
 * @version $Revision: 1.4 $
 */
public final class Animations {

    private Animations() {
        // Overrides the default constructor; prevents instantiation.
    }
    

    /**
     * Creates and returns an animation that is defined by a given 
     * animation and offset; the resulting animation applies 
     * the original effect shifted in time.
     * 
     * @param beginTime   the time to begin the shifted animation
     * @param animation   the animation to shift
     * @return the shifted animation
     */
    public static Animation offset(long beginTime, Animation animation) {
        return new OffsetAnimation(beginTime, animation);
    }
    

    /**
     * Creates and returns a parallel time container, that is an animation 
     * that applies the effect of the given animations all at the same time.
     * 
     * @param animations   a <code>List</code> of animations
     * @return a parallel time container for the given animations
     */
    public static Animation parallel(List animations) {
        return new ParallelAnimation(animations);
    }
    

    /**
     * Creates and returns a parallel time container for the given animations, 
     * that is an animation that applies the effect of the given animations 
     * at the same time.
     * 
     * @param animation1   one of the animations to parallelize
     * @param animation2   the other animation to parallelize
     * @return the parallelized animation 
     */
    public static Animation parallel(Animation animation1, 
                                      Animation animation2) {
        List list = new LinkedList();
        list.add(animation1);
        list.add(animation2);
        return parallel(list);
    }
    

    /**
     * Creates and returns a pausing animation that has no effect 
     * but a duration. It is useful in combination with sequenced 
     * and parallel time containers.
     * 
     * @param duration    the pause duration
     * @return an animation that has no effect
     */
    public static Animation pause(long duration) {
        return new PauseAnimation(duration);
    }
    

    /**
     * Creates and answers an animation that is defined by repeating 
     * the given animation. The result's duration is the 
     * duration times repeatCount.
     * 
     * @param repeatCount   the number of repetitions
     * @param animation     the animation to repeat
     * @return the repeated animation
     */
    public static Animation repeat(float repeatCount, Animation animation) {
        long duration = (long) (animation.duration() * repeatCount);
        return new RepeatedAnimation(duration, animation);
    }
    

    /**
     * Creates and returns an animation that is defined by reverting 
     * the given animation over the time.
     * 
     * @param animation  the animation to reverse
     * @return the reversed animation
     */
    public static Animation reverse(Animation animation) {
        return new ReversedAnimation(animation);
    }
    

    /**
     * Creates and returns a sequenced time container that is an animation,
     * that concatenates the given list of animations over the time.
     * 
     * @param animations    a <code>List</code> of animations
     * @return the sequenced animation
     */
    public static Animation sequential(List animations) {
        return new SequencedAnimation(animations);
    }
    

    /**
     * Creates and returns a sequenced time container that is an animation,
     * that concatenates the given array of animations over the time.
     * 
     * @param animations    an array of animations
     * @return the sequenced animation
     */
    public static Animation sequential(Animation[] animations) {
        return sequential(Arrays.asList(animations));
    }

    
    /**
     * Creates and returns an animation that is defined by concatenating 
     * the two given animations.
     * 
     * @param first    the first animation in the sequence
     * @param second   the second animation in the sequence
     * @return a sequenced animation
     */
    public static Animation sequential(Animation first, Animation second) {
        List sequence = new LinkedList();
        sequence.add(first);
        sequence.add(second);
        return sequential(sequence);
    }
    

    // Helper Classes *********************************************************

    // Helper class that wraps an animation to give it a time offset.
    private static class OffsetAnimation extends AbstractAnimation {
        private final Animation animation;
        private final long beginTime;

        private OffsetAnimation(long beginTime, Animation animation) {
            super(beginTime + animation.duration(), true);
            this.animation = animation;
            this.beginTime = beginTime;
        }

        protected void applyEffect(long time) {
            long relativeTime = time - beginTime;
            if (relativeTime >= 0)
                animation.animate(relativeTime);
        }

    }

    /**
     * Used to apply an effect one-time only.
     */
    public static abstract class OneTimeAnimation extends AbstractAnimation {

        private boolean effectApplied;

        /**
         * Constructs a <code>OneTimeAnimation</code>.
         */
        public OneTimeAnimation() {
            super(0, true);
            effectApplied = false;
        }

        /**
         * Applies the effect to the animation target, 
         * only if is hasn't been applied before.
         * 
         * @param time   the time used to determine the animation effect
         */
        public void animate(long time) {
            if (effectApplied)
                return;

            fireAnimationStarted(time);
            applyEffect(time);
            fireAnimationStopped(time);
            effectApplied = true;
        }
    }

    // Helper class to parallelize animations
    private static class ParallelAnimation extends AbstractAnimation {
        private final List animations;

        private ParallelAnimation(List animations) {
            super(maxDuration(animations), true);
            this.animations = animations;
        }

        private static long maxDuration(List animations) {
            long maxDuration = 0;
            for (Iterator i = animations.iterator(); i.hasNext();) {
                Animation animation = (Animation) i.next();
                long duration = animation.duration();
                if (duration > maxDuration)
                    maxDuration = duration;
            }
            return maxDuration;
        }

        protected void applyEffect(long time) {
            for (Iterator i = animations.iterator(); i.hasNext();) {
                Animation animation = (Animation) i.next();
                animation.animate(time);
            }
        }

    }

    // Helper class for a pause, an animation, that has no effect.
    private static class PauseAnimation extends AbstractAnimation {
        PauseAnimation(long duration) {
            super(duration, true);
        }

        protected void applyEffect(long time) {
            // Just pause, do nothing.
        }
    }

    // Helper class to repeat an animation.
    private static class RepeatedAnimation extends AbstractAnimation {
        private final Animation animation;
        private final long simpleDuration;

        private RepeatedAnimation(long duration, Animation animation) {
            super(duration, true);
            this.animation = animation;
            this.simpleDuration = animation.duration();
        }

        protected void applyEffect(long time) {
            animation.animate(time % simpleDuration);
        }
    }

    // Helper class to reverse an animation over the time.
    private static class ReversedAnimation extends AbstractAnimation {
        private final Animation animation;

        private ReversedAnimation(Animation animation) {
            super(animation.duration(), true);
            this.animation = animation;
        }

        protected void applyEffect(long time) {
            long reversedTime = duration() - time;
            if (reversedTime < 0)
                throw new IllegalArgumentException("The time is outside the valid time interval.");

            animation.animate(reversedTime);
        }
    }

    // Helper class to create a sequence of animations.
    private static class SequencedAnimation extends AbstractAnimation {
        private final List animations;

        private SequencedAnimation(List animations) {
            super(cumulatedDuration(animations), true);
            this.animations = Collections.unmodifiableList(animations);
            if (this.animations.isEmpty())
                throw new IllegalArgumentException("The list of animations must not be empty.");
        }

        private static long cumulatedDuration(List animations) {
            long cumulatedDuration = 0;
            for (Iterator i = animations.iterator(); i.hasNext();) {
                Animation animation = (Animation) i.next();
                cumulatedDuration += animation.duration();
            }
            return cumulatedDuration;
        }

        protected void applyEffect(long time) {
            long startTime = 0;
            for (Iterator i = animations.iterator(); i.hasNext();) {
                Animation animation = (Animation) i.next();
                long relativeTime = time - startTime;
                if (relativeTime > 0)
                    animation.animate(relativeTime);
                startTime += animation.duration();
            }
        }

    }

}